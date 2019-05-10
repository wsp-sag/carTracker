
@echo off
cd "C:/projects/Ohio3_Tier1/Lima_c10/CODE/CT-RAMP/runScripts"

:: environment variables used in command line commands
SET WORKSPACE_PATH=C:/projects/Ohio3_Tier1/Lima_c10/CODE/CT-RAMP
set JAVA_64_PATH=%WORKSPACE_PATH%/lib/Java/jre1.8.0_31
set CUBE_PATH1=%WORKSPACE_PATH%/lib/VoyagerFileAPI
set PROPERTY_FILE_BASE_NAME=carTracker
call setupJppfEnvironmentCarTrack.cmd
set  LIB_PATH=%WORKSPACE_PATH%/lib/logging-log4j-1.2.9/*;%WORKSPACE_PATH%/lib/jexcelapi/*;%WORKSPACE_PATH%/lib/commons-lang3-3.4/*;%WORKSPACE_PATH%/lib/commonbaselib/*;%WORKSPACE_PATH%/lib/ortools;%WORKSPACE_PATH%/lib/ortools/*
set JAR_PATH=%WORKSPACE_PATH%/jarFiles/*;%WORKSPACE_PATH%/jarFiles/carTrack/*
set CLASSPATH=%CUBE_PATH1%;%WORKSPACE_PATH%/config;%JPPF_CLIENT_PATH%/lib/*;%JAR_PATH%;%LIB_PATH%;%WORKSPACE_PATH%/jarfiles/common-base.jar;%WORKSPACE_PATH%/lib/jobListenerClass
rem set NUM_NODE=36
set NUM_NODE=12
set /a NUM_NODE=NUM_NODE-10
start ..\lib\startDriver.cmd
timeout /t 10
:loop
start ..\lib\startNode.cmd
set /a NUM_NODE=NUM_NODE-1
if %NUM_NODE%==0 goto exitloop
goto loop
:exitloop
timeout /t 20
rem -Xdebug -Xrunjdwp:transport=dt_socket,address=1040,server=y,suspend=y ^
@echo on
%JAVA_64_PATH%\bin\java.exe -Dname=carAlloc ^
 -Djava.library.path=%LIB_PATH% ^
 -showversion -server -Xmx80g ^
 -Djppf.config=jppfCarTrack.properties ^
 -Dlog4j.configuration=log4j_ct.xml ^
 appLayer.CarAllocatorMain %PROPERTY_FILE_BASE_NAME% true

taskkill /im "java.exe" /f
