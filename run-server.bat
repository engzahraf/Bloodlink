@echo off
setlocal
echo ========================================================
echo   BloodLink Backend TCP Server Launcher (Windows)
echo ========================================================

REM Look for local JDK or system java
set "JAVA_CMD=java"
if exist "%~dp0tools\zulu17.38.21-ca-jdk17.0.5-win_x64\bin\java.exe" (
    set "JAVA_CMD=%~dp0tools\zulu17.38.21-ca-jdk17.0.5-win_x64\bin\java.exe"
    set "JAVAC_CMD=%~dp0tools\zulu17.38.21-ca-jdk17.0.5-win_x64\bin\javac.exe"
) else (
    set "JAVAC_CMD=javac"
)

REM Build directory
if not exist "%~dp0bin" mkdir "%~dp0bin"

echo Compiling BloodLink Server and Database...
dir /s /B "%~dp0src\*.java" > "%TEMP%\bloodlink_server_sources.txt"
"%JAVAC_CMD%" -encoding UTF-8 -cp "%~dp0lib\sqlite-jdbc-3.51.3.0.jar" -d "%~dp0bin" @"%TEMP%\bloodlink_server_sources.txt"
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b %ERRORLEVEL%
)

REM Default admin seeding (enabled by default for convenience)
if "%BLOODLINK_SEED_ADMIN%"=="" set "BLOODLINK_SEED_ADMIN=true"
if "%BLOODLINK_ADMIN_USER%"=="" set "BLOODLINK_ADMIN_USER=admin@bloodlink.com"
if "%BLOODLINK_ADMIN_PASS%"=="" set "BLOODLINK_ADMIN_PASS=Admin@BloodLink2026"

echo Starting BloodLink TCP Server on port 9090...
"%JAVA_CMD%" -cp "%~dp0bin;%~dp0lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.server.ServerMain %*
endlocal

