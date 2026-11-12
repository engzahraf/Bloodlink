@echo off
echo ===================================================
echo   Compiling BloodLink Application Sources
echo ===================================================
if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -Xlint:none -cp "lib\sqlite-jdbc-3.51.3.0.jar" -d bin @sources.txt
del sources.txt
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation completed successfully.
) else (
    echo [ERROR] Compilation failed with error code %ERRORLEVEL%.
)

