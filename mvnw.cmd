@REM AuthEase Maven Wrapper Windows Script
@echo off
setlocal

if not defined JAVA_HOME (
    if exist "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot" (
        set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
    ) else if exist "C:\Users\THARUN SM\tools\jdk17" (
        set "JAVA_HOME=C:\Users\THARUN SM\tools\jdk17"
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

if exist "C:\Users\THARUN SM\tools\maven\bin\mvn.cmd" (
    set "MAVEN_EXE=C:\Users\THARUN SM\tools\maven\bin\mvn.cmd"
) else (
    set "MAVEN_EXE=mvn"
)

call "%MAVEN_EXE%" %*
exit /b %ERRORLEVEL%
