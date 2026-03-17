@REM Maven Wrapper script for Windows
@REM Downloads Maven if not present, then runs it

@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6"

if not exist "%MAVEN_HOME%" (
    echo Downloading Maven...
    mkdir "%MAVEN_HOME%"
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_HOME%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%MAVEN_HOME%\maven.zip' -DestinationPath '%MAVEN_HOME%' -Force"
    del "%MAVEN_HOME%\maven.zip"
)

for /f "delims=" %%i in ('dir /s /b "%MAVEN_HOME%\mvn.cmd"') do set "MAVEN_BIN=%%i"

"%MAVEN_BIN%" %*
