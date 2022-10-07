echo JAVA_HOME: %JAVA_HOME%
java -version
java -Dconfig.file=application.conf -Dlogback.configurationFile="logback.xml" -jar pass-interpreter-console.jar
