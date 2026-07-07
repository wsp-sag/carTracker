@echo off
REM Build carTracker JAR using Java 21 and Gradle 9.3.1
REM This script automates the build process for the carTracker project

setlocal enabledelayedexpansion

REM Set Java 21 JDK path
set JAVA_HOME=C:\Projects\scag_51152\development\jdk-21.0.10

REM Verify Java is available
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: Java 21 JDK not found at %JAVA_HOME%
    echo Please verify the JDK installation path.
    pause
    exit /b 1
)

REM Change to project directory
cd /d C:\Projects\scag_51152\development\carTracker
if errorlevel 1 (
    echo Error: Failed to change to carTracker directory
    pause
    exit /b 1
)

echo ============================================
echo Building carTracker JAR
echo ============================================
echo Java Home: %JAVA_HOME%
echo Project Directory: %CD%
echo.

REM Display Java version
echo Java Version:
"%JAVA_HOME%\bin\java.exe" -version
echo.

REM Run Gradle build
echo Starting Gradle build...
echo.
call gradlew.bat clean build

if errorlevel 1 (
    echo.
    echo Build FAILED!
    echo Please review the error messages above.
    pause
    exit /b 1
)

echo.
echo ============================================
echo Build SUCCESSFUL!
echo ============================================
echo JAR Location: %CD%\build\libs\carTracker-12Oct2022_v4.3.jar
echo.

REM List generated artifacts
echo Generated Artifacts:
dir /b "%CD%\build\libs\*.jar"
echo.

pause
endlocal
