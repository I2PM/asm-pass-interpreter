package de.athalis.coreasm.plugins.storage

import java.util.{Set => JSet}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import de.athalis.coreasm.helper.Implicits._
import de.athalis.coreasm.base.Typedefs._

import de.athalis.coreasm.plugins.storage.lib._

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}
import com.typesafe.config.{Config, ConfigFactory}
import akka.actor._
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.duration._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.coreasm.engine.VersionInfo
import org.coreasm.engine.plugin._
import org.coreasm.engine.interpreter._
import org.coreasm.engine.absstorage._
import org.coreasm.engine.plugins.io.IOPlugin
import org.coreasm.engine.plugins.string.StringElement


object AkkaStoragePlugin {
  import info.BuildInfo

  val VERSION_INFO: VersionInfo = VersionInfo.valueOf(BuildInfo.version)
  val PLUGIN_NAME: String       = "AkkaStoragePlugin"

  private val logger: Logger = LoggerFactory.getLogger(AkkaStoragePlugin.getClass)
}

class AkkaStoragePlugin() extends Plugin with InterpreterPlugin with VocabularyExtender {
  import AkkaStoragePlugin._

  // TODO: possibly more.. everything we can map to?
  override def getDependencyNames: JSet[String] = Set(IOPlugin.PLUGIN_NAME).asJava

  logger.info("starting " + PLUGIN_NAME + " (" + VERSION_INFO + ") [" + info.BuildInfo + "]")

  val queue = new LinkedBlockingQueue[(ActorRef, AkkaStorageJob)]()

  logger.info("initializing Akka ActorSystem")

  val config: Config = ConfigFactory.load(getClass.getClassLoader)

  implicit val timeout = Timeout(5.seconds)
  // TODO: move to initialize
  private val system: ActorSystem = ActorSystem("coreasm-storage", config.getConfig("coreasm-storage").withFallback(config), getClass.getClassLoader)
  private val actor: ActorRef = system.actorOf(Props(new AkkaStorageActor(queue)), "AkkaStorageActor")

  logger.info("initialized Akka ActorSystem")

  val backgrounds: MutableMap[String, BackgroundElement] = MutableMap[String, BackgroundElement]()
  val functions: MutableMap[String, FunctionElement] = MutableMap[String, FunctionElement]()
  val rules: MutableMap[String, RuleElement] = MutableMap[String, RuleElement]()
  val universes: MutableMap[String, UniverseElement] = MutableMap[String, UniverseElement]()

  var lastInterpretEnd: Long = System.nanoTime

  // Members declared in org.coreasm.engine.plugin.Plugin
  override def initialize(): Unit = {
    logger.info("initializing plugin")

    addAkkaStorageRuleElement()
  }

  override def terminate(): Unit = {
    logger.info("terminate plugin")
    super.terminate()
    Await.result(system.terminate, 10.seconds)
    queue.clear()
  }

  private def getValue(l: ASMLocation): Option[Any] = {
    // storage.getValue is not safe to be called from other threads. It must be called from same thread as the one that called our `interpret` as it may use the interpreter itself
    val el: Element = capi.getStorage.getValue(L(l))
    V(el) match {
        case None => None
        case x => Some(x)
      }
  }

  def addAkkaStorageRuleElement(): Unit = {
    val ruleName: String = AkkaStorageRuleNode.RULE_NAME
    val bodyNode: ASTNode = new AkkaStorageRuleNode()
    val params: Seq[String] = Seq[String]()

    // TODO: declarationNode??
    val ruleElement = new RuleElement(bodyNode, ruleName, params.asJava, bodyNode)
    rules(ruleName) = ruleElement
  }

  // Members declared in org.coreasm.engine.VersionInfoProvider
  override def getVersionInfo: org.coreasm.engine.VersionInfo = VERSION_INFO

  // Members declared in org.coreasm.engine.plugin.InterpreterPlugin
  override def interpret(interpreter: Interpreter, node: ASTNode): ASTNode = node match {
      case storageNode: AkkaStorageRuleNode => interpret(interpreter, storageNode)
      case x => { logger.warn("can not handle node: " + x) ; x }
    }

  // IMPORTANT: nothing should be made in a separate thread, as CoreASM uses `ThreadLocal`s
  def interpret(interpreter: Interpreter, node: AkkaStorageRuleNode): ASTNode = {
    implicit val i: Interpreter = interpreter
    implicit val n: AkkaStorageRuleNode = node

    val startTime: Long = System.nanoTime
    val timeSinceLastInterpret: Long = startTime - lastInterpretEnd
    val queueTimeout: Long = Math.min(timeSinceLastInterpret >> 2, (100 * 1e6).toLong) // max 100ms

    logger.info("{}: time since last interpret: {}ms", i.getSelf, timeSinceLastInterpret/1e6)
    logger.info("{}: queueTimeout: {}ms", i.getSelf, queueTimeout/1e6)


    var updateList: Seq[Update] = Nil
    var notificateStored: Seq[ActorRef] = Nil
    var notificateASMStep: Seq[ActorRef] = Nil

    // only for logging
    var readJobCount: Int = 0
    var writeJobCount: Int = 0

    var durationIdle: Long = 0
    var durationReading: Long = 0
    var durationWriteMapping: Long = 0

    var startPoll = System.nanoTime
    var richJob: (ActorRef, AkkaStorageJob) = queue.poll(queueTimeout, TimeUnit.NANOSECONDS)
    durationIdle += System.nanoTime - startPoll
    while (richJob != null) {
      val (sender, job) = richJob

      job match {
        case ValueRequest(location) => {
          logger.info("{}: answering read job #" + readJobCount + ": {}", i.getSelf, location.asInstanceOf[Any])
          logger.info("{}: current idle duration: {}ms", i.getSelf, durationIdle / 1e6)
          val startRead = System.nanoTime
          val value = getValue(location)
          val durationRead = System.nanoTime - startRead
          sender ! ValueReply(value, startTime, durationRead / 1e6)
          readJobCount += 1
          durationReading += durationRead
        }
        case ApplyUpdates(updates) => {
          logger.info("{}: converting write update job #" + writeJobCount + " {}", i.getSelf, updates.asInstanceOf[Any])
          val startMap = System.nanoTime
          updateList ++= updates.map(U)
          notificateStored +:= sender
          writeJobCount += 1
          durationWriteMapping += System.nanoTime - startMap
        }
        case AwaitASMStep => {
          notificateASMStep +:= sender
        }
      }

      startPoll = System.nanoTime
      richJob = queue.poll(queueTimeout, TimeUnit.NANOSECONDS)
      durationIdle += System.nanoTime - startPoll
    }


    notificateStored.foreach(_ ! UpdateStored)
    notificateASMStep.foreach(_ ! ASMStep)

    updateList +:= new Update(
      IOPlugin.PRINT_OUTPUT_FUNC_LOC,
      new StringElement(i.getSelf + ": AkkaStorageRuleNode collected " + writeJobCount + " writes"),
      IOPlugin.PRINT_ACTION,
      interpreter.getSelf,
      node.getScannerInfo)

    updateList +:= new Update(
      IOPlugin.PRINT_OUTPUT_FUNC_LOC,
      new StringElement(i.getSelf + ": AkkaStorageRuleNode answered " + readJobCount + " reads"),
      IOPlugin.PRINT_ACTION,
      interpreter.getSelf,
      node.getScannerInfo)

    val updateMultiset: UpdateMultiset = new UpdateMultiset(updateList.asJava)

    node.setNode(null, updateMultiset, Element.UNDEF) // undef instead of null so that isEvaluated can be true even when the updateMultiset is null

    lastInterpretEnd = System.nanoTime

    logger.info("{}: interpret took: {}ms", i.getSelf, (lastInterpretEnd-startTime)/1e6)
    logger.info("{}: idle duration: {}ms", i.getSelf, durationIdle / 1e6)
    logger.info("{}: readJobs: {}", i.getSelf, readJobCount)
    logger.info("{}: read duration: {}ms", i.getSelf, durationReading / 1e6)
    logger.info("{}: writeJobs: {}", i.getSelf, writeJobCount)
    logger.info("{}: write mapping duration: {}ms", i.getSelf, durationWriteMapping / 1e6)
    logger.info("{}: updates: {}", i.getSelf, updateList.length)

    node
  }

  // Members declared in org.coreasm.engine.plugin.VocabularyExtender
  override def getBackgroundNames: java.util.Set[String] = backgrounds.keySet.asJava
  override def getBackgrounds: java.util.Map[String, BackgroundElement] = backgrounds.asJava
  override def getFunctionNames: java.util.Set[String] = functions.keySet.asJava
  override def getFunctions: java.util.Map[String, FunctionElement] = functions.asJava
  override def getRuleNames: java.util.Set[String] = rules.keySet.asJava
  override def getRules: java.util.Map[String, RuleElement] = rules.asJava
  override def getUniverseNames: java.util.Set[String] = universes.keySet.asJava
  override def getUniverses: java.util.Map[String, UniverseElement] = universes.asJava
}
