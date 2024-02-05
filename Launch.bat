@echo off
setlocal enabledelayedexpansion

REM Set the path to your JAR file
set JAR_PATH=.\out\artifacts\Proxy_jar\Proxy.jar

REM Set any arguments you want to pass to your JAR
set APP_ARGS=8080

REM Enable Ctrl+C trap
set CTRL_C_PRESSED=0

REM Define what to do when Ctrl+C is pressed
:CtrlC
set CTRL_C_PRESSED=1
echo Ctrl+C pressed! Attempting graceful shutdown...
goto :run_app

REM Define the main application run routine
:run_app
REM Check if Ctrl+C was pressed
if !CTRL_C_PRESSED! equ 1 (
    REM Perform any cleanup tasks here
    REM ...
    echo Cleanup complete.
    REM Reset CTRL_C_PRESSED
    set CTRL_C_PRESSED=0
)

REM Run your Java application
java -jar %JAR_PATH% %APP_ARGS%

REM Check for app completion or Ctrl+C press
if !CTRL_C_PRESSED! equ 0 goto :eof

REM Handle post Ctrl+C actions (if any)
REM ...

REM End of the script
endlocal

pause