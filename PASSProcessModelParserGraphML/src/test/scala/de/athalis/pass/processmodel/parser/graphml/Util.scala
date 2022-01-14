package de.athalis.pass.processmodel.parser.graphml

import de.athalis.pass.processmodel.parser.graphml.Helper.ParserLocation
import de.athalis.pass.processmodel.parser.graphml.structure.GraphML
import de.athalis.pass.processmodel.parser.graphml.structure.Key

import scala.collection.immutable
import scala.xml.Elem

object Util {
  private implicit val loc: ParserLocation = ParserLocation("Util", None)

  private val graphMLXML: Elem =
    <graphml>
        <key attr.name="Description" attr.type="string" for="graph" id="d0"/>
        <key for="port" id="d1" yfiles.type="portgraphics"/>
        <key for="port" id="d2" yfiles.type="portgeometry"/>
        <key for="port" id="d3" yfiles.type="portuserdata"/>
        <key attr.name="IP Size" attr.type="int" for="node" id="d4">
          <default>20</default>
        </key>
        <key attr.name="url" attr.type="string" for="node" id="d5"/>
        <key attr.name="description" attr.type="string" for="node" id="d6"/>
        <key for="node" id="d7" yfiles.type="nodegraphics"/>
        <key for="graphml" id="d8" yfiles.type="resources"/>
        <key attr.name="DataMaps" attr.type="string" for="edge" id="d9">
          <default/>
        </key>
        <key attr.name="url" attr.type="string" for="edge" id="d10"/>
        <key attr.name="description" attr.type="string" for="edge" id="d11"/>
        <key for="edge" id="d12" yfiles.type="edgegraphics"/>
      <graph id="G" />
    </graphml>

  private lazy val graphML: GraphML = Helper.parseGraphML(graphMLXML)

  lazy val keys: immutable.Seq[Key[_]] = graphML.keys

}
