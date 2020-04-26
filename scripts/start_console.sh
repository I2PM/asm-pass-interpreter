#!/bin/bash

echo "JAVA_HOME: $JAVA_HOME"
java -version
java -Dconfig.file=application.conf -jar pass-interpreter-console.jar
