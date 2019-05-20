
@echo off
cd "C:/projects/Ohio3_Tier1/Lima_c10/CODE/CT-RAMP/runScripts"

:: environment variables used in command line commands
SET WORKSPACE_PATH=C:/projects/Ohio3_Tier1/Lima_c10/CODE/CT-RAMP
set JAVA_64_PATH=%WORKSPACE_PATH%/lib/Java/jre1.8.0_31
set CUBE_PATH1=%WORKSPACE_PATH%/lib/VoyagerFileAPI
set GNUWIN_PATH=%WORKSPACE_PATH%/lib/Auxiliary/gnuwin32/bin
REM set path=C:\Python27\ArcGIS10.3\;C:\PROGRAM FILES (X86)\ARCGIS\ENGINE10.3\BIN;C:\Program Files (x86)\Citilabs\Cube;C:\Program Files (x86)\Intel\MPI-RT\4.1.1.036\em64t\bin;C:\Program Files (x86)\Intel\MPI-RT\4.1.1.036\ia32\bin;C:\Program Files\HP\NCU;C:\Windows\system32;C:\Windows;C:\Windows\System32\Wbem;C:\Windows\System32\WindowsPowerShell\v1.0\;C:\Program Files (x86)\Citilabs\CubeVoyager;c:\models\ctramp\runtime\jarfiles\;C:\Program Files\MySQL\MySQL Utilities 1.6\;C:\IBM\ITM\bin;C:\IBM\ITM\InstallITM;C:\IBM\ITM\TMAITM6;%GNUWIN_PATH%

:: environment variables used in CLASSPATH definition
:: set JPPF_RELEASE=4.0
set JPPF_RELEASE=5.0.4
set JPPF_PATH=%WORKSPACE_PATH%/lib/JPPF-%JPPF_RELEASE%
set JPPF_CONFIG=%WORKSPACE_PATH%/config
set JPPF_CLIENT_PATH=%JPPF_PATH%/JPPF-%JPPF_RELEASE%-admin-ui
set JPPF_DRIVER_PATH=%JPPF_PATH%/JPPF-%JPPF_RELEASE%-driver
set JPPF_NODE_PATH=%JPPF_PATH%/JPPF-%JPPF_RELEASE%-node

set  LIB_PATH=%WORKSPACE_PATH%/lib/logging-log4j-1.2.9/*;%WORKSPACE_PATH%/lib/jexcelapi/*;%WORKSPACE_PATH%/lib/commons-lang3-3.4/*;%WORKSPACE_PATH%/lib/commonbaselib/*;%WORKSPACE_PATH%/lib/solver/;;%WORKSPACE_PATH%/lib/solver/*
set JAR_PATH=%WORKSPACE_PATH%/jarFiles/*;%WORKSPACE_PATH%/jarFiles/carTrack/*

set JPPF_USER_EXTENSION_PATH=%WORKSPACE_PATH%/config;%WORKSPACE_PATH%/nodeListenerClasses;%LIB_PATH%;%CUBE_PATH1%;%WORKSPACE_PATH%UECs;%WORKSPACE_PATH%/lib;%JAR_PATH%     
