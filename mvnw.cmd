@ECHO OFF
SETLOCAL
SET "MAVEN_PROJECTBASEDIR=%~dp0"
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
IF "%JAVA_HOME%"=="" (
  IF EXIST "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot" (
    SET "JAVA_HOME=%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
  )
)
IF "%JAVA_HOME%"=="" (
  ECHO JAVA_HOME is not set and JDK 21 was not found in the default Adoptium location.
  ECHO Install JDK 21 and set JAVA_HOME, then retry.
  EXIT /B 1
)
SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
"%JAVA_HOME%\bin\java.exe" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
EXIT /B %ERRORLEVEL%
