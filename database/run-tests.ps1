# Run BloodLink Database Test Suite
Write-Host "Compiling BloodLink Database Module..." -ForegroundColor Cyan
$sources = (Get-ChildItem -Path "$PSScriptRoot\src" -Recurse -Filter "*.java").FullName
New-Item -ItemType Directory -Force -Path "$PSScriptRoot\bin" | Out-Null
javac -cp "$PSScriptRoot\lib\*" -d "$PSScriptRoot\bin" $sources

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful. Executing Automated Test Suite..." -ForegroundColor Green
    java -cp "$PSScriptRoot\bin;$PSScriptRoot\lib\*" com.bloodlink.database.DatabaseTestSuite
} else {
    Write-Host "Compilation failed with error code $LASTEXITCODE" -ForegroundColor Red
}

