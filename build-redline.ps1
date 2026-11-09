<#
  Builds the Redline app (fossRelease) and optionally installs it on a connected phone/emulator.

  Run from the project folder (the one containing gradlew.bat):
    powershell -ExecutionPolicy Bypass -File .\build-redline.ps1
  Add -Install to install on a phone plugged in with USB debugging on.
#>
param([switch]$Install)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path ".\gradlew.bat")) {
    Write-Host "gradlew.bat not found. Run this script from the extracted Metrolist-main folder." -ForegroundColor Red; exit 1
}

# 1. Java 21: use Android Studio's built-in JDK if JAVA_HOME isn't set.
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jbr = @(
        "C:\Program Files\Android\Android Studio\jbr",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
        "$env:ProgramFiles\Android\Android Studio Preview\jbr"
    ) | Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
    if (-not $jbr) { Write-Host "Java not found. Install Android Studio (it includes Java 21)." -ForegroundColor Red; exit 1 }
    $env:JAVA_HOME = $jbr
}
Write-Host "Java: $env:JAVA_HOME" -ForegroundColor Cyan

# 2. Android SDK location (local.properties).
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
if (-not (Test-Path $sdk)) { Write-Host "Android SDK not found at $sdk. Open Android Studio once to install it." -ForegroundColor Red; exit 1 }
if (-not (Test-Path ".\local.properties")) {
    "sdk.dir=$($sdk -replace '\\','\\')" | Out-File -Encoding ascii ".\local.properties"
}
Write-Host "SDK:  $sdk" -ForegroundColor Cyan

# 3. Build the optimized (smooth) version.
Write-Host "`nBuilding Redline (fossRelease)... first build downloads dependencies and can take 10-20 minutes.`n" -ForegroundColor Cyan
& .\gradlew.bat clean assembleFossRelease --stacktrace
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nBuild failed. Copy the red error lines above (search for 'e: ' or 'error:') and send them back." -ForegroundColor Red; exit 1
}

$apk = Get-ChildItem ".\app\build\outputs\apk\foss\release\*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) { Write-Host "Build finished but no APK found." -ForegroundColor Red; exit 1 }
$dest = Join-Path $PSScriptRoot "Redline.apk"
Copy-Item $apk.FullName $dest -Force
Write-Host "`nAPK ready: $dest  ($([math]::Round($apk.Length/1MB,1)) MB)" -ForegroundColor Green

# 4. Optional install.
if ($Install) {
    $adb = "$sdk\platform-tools\adb.exe"
    if (-not (Test-Path $adb)) { Write-Host "adb not found in $sdk\platform-tools" -ForegroundColor Red; exit 1 }
    & $adb install -r $dest
    if ($LASTEXITCODE -eq 0) { Write-Host "Installed. Open 'Redline' on the phone." -ForegroundColor Green }
}
