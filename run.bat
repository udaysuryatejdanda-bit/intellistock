@echo off
setlocal
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto use_java_home
echo Launching IntelliStock AI using Java from PATH...
java -jar "%~dp0target\intellistock-ai.jar"
goto done

:use_java_home
echo Launching IntelliStock AI using JAVA_HOME...
"%JAVA_HOME%\bin\java.exe" -jar "%~dp0target\intellistock-ai.jar"

:done
pause
