# Run BloodLink Frontend Unit & Regression Test Suite
$ErrorActionPreference = "Stop"

$jdkPath = "$PSScriptRoot\tools\zulu17.38.21-ca-jdk17.0.5-win_x64\bin"
if (Test-Path "$jdkPath\java.exe") {
    $java = "$jdkPath\java.exe"
    $javac = "$jdkPath\javac.exe"
} else {
    $java = "java"
    $javac = "javac"
}

Write-Host "Compiling BloodLink Frontend and Test Suite..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "$PSScriptRoot\frontend\bin" | Out-Null

$srcFiles = (Get-ChildItem -Path "$PSScriptRoot\frontend\src", "$PSScriptRoot\frontend\test" -Recurse -Filter "*.java" | Where-Object { $_.Name -notmatch "ClientServerIntegrationTest" }).FullName

& $javac -encoding UTF-8 -d "$PSScriptRoot\frontend\bin" $srcFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful. Running FrontendTestSuite..." -ForegroundColor Green
    & $java -cp "$PSScriptRoot\frontend\bin" com.bloodlink.client.test.FrontendTestSuite
} else {
    Write-Host "Compilation failed with error code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
