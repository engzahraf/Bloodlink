@echo off
setlocal
echo ========================================================
echo   BloodLink Desktop Client Launcher (Windows)
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
if not exist "%~dp0frontend\bin" mkdir "%~dp0frontend\bin"

echo Compiling BloodLink Frontend...
dir /s /B "%~dp0frontend\src\*.java" > "%TEMP%\bloodlink_sources.txt"
"%JAVAC_CMD%" -encoding UTF-8 -d "%~dp0frontend\bin" @"%TEMP%\bloodlink_sources.txt"
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b %ERRORLEVEL%
)

echo Starting BloodLink Desktop Client...
"%JAVA_CMD%" -cp "%~dp0frontend\bin" com.bloodlink.client.Main %*
endlocal
