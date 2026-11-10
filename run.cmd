@ECHO OFF
SETLOCAL
SET "PROJECT_DIR=%~dp0"
IF "%JAVA_HOME%"=="" (
  IF EXIST "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot" (
    SET "JAVA_HOME=%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
  )
)
IF "%JAVA_HOME%"=="" (
  ECHO JAVA_HOME is not set. Install JDK 21 and set JAVA_HOME.
  EXIT /B 1
)
SET "PATH=%JAVA_HOME%\bin;%PATH%"
PUSHD "%PROJECT_DIR%"
IF NOT EXIST "target\intellistock-ai.jar" (
  CALL mvnw.cmd -q package -DskipTests
  IF ERRORLEVEL 1 EXIT /B 1
)
"%JAVA_HOME%\bin\java.exe" -jar "target\intellistock-ai.jar"
POPD
