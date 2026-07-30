@echo off
setlocal

rem The JAR is expected beside this script; SOLON_ENV may override prod.
set "AppName=jimuqu-admin.jar"
set "JVM_OPTS=-Dname=%AppName% -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC -XX:+ZGenerational"
if not defined SOLON_ENV set "SOLON_ENV=prod"

echo.
echo [1] Start %AppName%
echo [2] Stop %AppName%
echo [3] Restart %AppName%
echo [4] Status %AppName%
echo [5] Exit
echo.
set /p "ID=Select an option: "

if "%ID%"=="1" goto start
if "%ID%"=="2" goto stop
if "%ID%"=="3" goto restart
if "%ID%"=="4" goto status
if "%ID%"=="5" exit /b 0
echo Invalid option: %ID%
exit /b 1

:findPid
set "pid="
for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr /i /c:"%AppName%"`) do set "pid=%%a"
exit /b 0

:start
call :findPid
if defined pid (
    echo %AppName% is already running ^(pid: %pid%^).
    exit /b 0
)
if not exist "%AppName%" (
    echo Application JAR does not exist: %CD%\%AppName%
    exit /b 1
)
start "" java %JVM_OPTS% -jar "%AppName%" --solon.env=%SOLON_ENV%
echo Started %AppName% in %SOLON_ENV% mode.
exit /b 0

:stop
call :findPid
if not defined pid (
    echo %AppName% is already stopped.
    exit /b 0
)
echo Stopping %AppName% ^(pid: %pid%^)...
taskkill /f /pid %pid% >nul
if errorlevel 1 exit /b 1
echo Stopped %AppName%.
exit /b 0

:restart
call :stop
if errorlevel 1 exit /b 1
goto start

:status
call :findPid
if defined pid (
    echo %AppName% is running ^(pid: %pid%^).
    exit /b 0
)
echo %AppName% is not running.
exit /b 1
