#!/bin/bash

java -Dconfig.file=application.conf -Dlogback.configurationFile="logback-parse.xml" -cp pass-processmodel-cli.jar de.athalis.pass.processmodel.interface.cli.ConsoleParser "$@"
