# Development

In order to build the project you need the Java SDK (not just the JRE), maven and sbt.

As of the time of writing the recommended versions are:
- OpenJDK 11
- maven 3.6
- sbt 1.3.10

## prepare custom CoreASM Version

First of all you need to have a customized CoreASM version installed.

Clone CoreASM from https://github.com/Locke/coreasm.core and checkout the `locke` branch.

Then execute `mvn install`.


## Build

The following tasks need to be executed in the sbt console.

Build the complete project with the `test:compile` task.

You can start the console application via `run` (note: the semantic has to be started in a separate terminal).

To execute all tests use the `test` task.

To create an executable build use the `prepareRelease` task, which will populate the "release" folder.
