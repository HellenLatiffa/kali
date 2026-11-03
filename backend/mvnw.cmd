@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
if not "%KALI_JDK21_HOME%"=="" (
    set "PROJECT_JDK_HOME=%KALI_JDK21_HOME%"
) else (
    set "PROJECT_JDK_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
)

if not exist "%WRAPPER_JAR%" (
    echo Maven Wrapper JAR not found: %WRAPPER_JAR%
    exit /b 1
)

if not exist "%PROJECT_JDK_HOME%\bin\java.exe" (
    echo Java 21 not found: %PROJECT_JDK_HOME%\bin\java.exe
    exit /b 1
)

set "JAVA_EXE=%PROJECT_JDK_HOME%\bin\java.exe"
"%JAVA_EXE%" %MAVEN_OPTS% %MAVEN_WRAPPER_OPTS% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
