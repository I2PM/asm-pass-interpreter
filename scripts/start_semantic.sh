#!/bin/bash

echo "JAVA_HOME: $JAVA_HOME"
java -version
java -Dconfig.file=application.conf -jar carma.jar asm/start.casm -D engine.pluginFolders="plugins" -v info
