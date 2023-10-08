# Development

In order to build the project you need the Java SDK (not just the JRE) and sbt.

As of the time of writing the recommended versions are:
- OpenJDK 17
- maven 3.6
- sbt 1.7.2
- scala 2.13

## Custom CoreASM fork

For stability, performance improvements and new features [a fork of the CoreASM framework](https://github.com/Locke/coreasm.core) is used.

Custom builds of the fork are published to [Maven Central](https://search.maven.org/search?q=g:de.athalis.coreasm%20a:coreasm-engine) and should be used in general.

During development, sometimes a development version has to be used. In that case, clone the CoreASM fork
from https://github.com/Locke/coreasm.core,  checkout the `locke` branch and execute `mvn install`.

## Build and Test

Build the complete project with the `Test/compile` task in the sbt console. To execute all tests, use the `test` task.

To create an executable build, use the `prepareRelease` task, which will populate the "release" folder. The `zipRelease` task will create a ZIP file for publication.

You can start the interpreter (server) with `start_semantic.sh` in the "release" folder.
You can start the console application (client) either via the `run` task in sbt or in the "release" folder with `start_console.sh`.
