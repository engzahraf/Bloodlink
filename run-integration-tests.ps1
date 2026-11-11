# Run BloodLink Live Client-Server TCP Integration Test
$ErrorActionPreference = "Stop"

$jdkPath = "$PSScriptRoot\tools\zulu17.38.21-ca-jdk17.0.5-win_x64\bin"
if (Test-Path "$jdkPath\java.exe") {
    $java = "$jdkPath\java.exe"
    $javac = "$jdkPath\javac.exe"
} else {
    $java = "java"
    $javac = "javac"
}

Write-Host "Compiling BloodLink Full Stack (Database + Backend + Frontend)..." -ForegroundColor Cyan

New-Item -ItemType Directory -Force -Path "$PSScriptRoot\bin\all" | Out-Null

$allSources = (Get-ChildItem -Path "$PSScriptRoot\database\src", "$PSScriptRoot\backend\src", "$PSScriptRoot\frontend\src", "$PSScriptRoot\frontend\test" -Recurse -Filter "*.java").FullName
$cp = "$PSScriptRoot\database\lib\*"

& $javac -encoding UTF-8 -cp $cp -d "$PSScriptRoot\bin\all" $allSources

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful. Executing ClientServerIntegrationTest..." -ForegroundColor Green
    $runCp = "$PSScriptRoot\bin\all;$PSScriptRoot\database\lib\*"
    & $java -cp $runCp com.bloodlink.client.test.ClientServerIntegrationTest
} else {
    Write-Host "Compilation failed with error code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
