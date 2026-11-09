<#
  update-redline.ps1 - one command to apply changed files, build and update Redline (keeps app data).

  1. Finds the code files (.kt / .xml / .kts) you downloaded recently (default: last 24 hours in Downloads)
  2. Copies each one to its correct place in the project (replacing the old one)
  3. Sets up Java from Android Studio
  4. Builds the fast fossRelease APK
  5. Installs it over the existing app on the running emulator/phone (install -r keeps your data)

  Run from anywhere:
    powershell -ExecutionPolicy Bypass -File C:\Users\Satyam\Downloads\Redline-App\Metrolist-main\update-redline.ps1
  Options:
    -Source <folder>   where the downloaded files are (default: your Downloads folder)
    -Hours <n>         only use files downloaded in the last n hours (default 24)
    -NoInstall         build only
#>
param(
    [string]$Source = "$env:USERPROFILE\Downloads",
    [int]$Hours = 24,
    [switch]$NoInstall
)

$ErrorActionPreference = "Stop"
$project = $PSScriptRoot
$main = Join-Path $project "app\src\main"
Set-Location $project

# New files that do not exist in the project yet: where they belong.
$newFiles = @{
    "SleepTimerSheet.kt" = "kotlin\com\metrolist\music\ui\player"
    "BrowseTabScreen.kt" = "kotlin\com\metrolist\music\ui\screens"
    "IosFling.kt"        = "kotlin\com\metrolist\music\ui\utils"
    "Typography.kt"      = "kotlin\com\metrolist\music\ui\theme"
    "AppleChrome.kt"     = "kotlin\com\metrolist\music\ui\component"
}

Write-Host "`n== 1. Copying changed files from $Source ==" -ForegroundColor Cyan
$cutoff = (Get-Date).AddHours(-$Hours)
$downloads = Get-ChildItem $Source -File | Where-Object {
    $_.LastWriteTime -gt $cutoff -and $_.Extension -in ".kt", ".xml", ".kts", ".toml"
}
# Browsers rename repeats to "Player (1).kt" or "Player_1.kt": use the real name and keep only the newest copy of each.
$latest = @{}
foreach ($f in $downloads) {
    $name = ($f.BaseName -replace '(\s*\(\d+\)|_\d+)$', '') + $f.Extension
    if (-not $latest.ContainsKey($name) -or $f.LastWriteTime -gt $latest[$name].LastWriteTime) { $latest[$name] = $f }
}

$copied = 0
foreach ($name in $latest.Keys) {
    $file = $latest[$name]
    $target = $null
    if ($name -eq "build.gradle.kts") {
        $target = Join-Path $project "app\build.gradle.kts"
    } elseif ($name -eq "libs.versions.toml") {
        $target = Join-Path $project "gradle\libs.versions.toml"
    } else {
        $found = @(Get-ChildItem $main -Recurse -File -Filter $name -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -notmatch '\\values-[^\\]+\\' })   # never overwrite translations
        if ($found.Count -eq 1) {
            $target = $found[0].FullName
        } elseif ($found.Count -gt 1) {
            Write-Host "  skipped $name (found in several places: $($found.FullName -join ', '))" -ForegroundColor Yellow
            continue
        } elseif ($newFiles.ContainsKey($name)) {
            $target = Join-Path (Join-Path $main $newFiles[$name]) $name
        }
    }
    if (-not $target) { Write-Host "  skipped $name (no matching file in the project)" -ForegroundColor Yellow; continue }
    Copy-Item $file.FullName $target -Force
    Write-Host "  updated $($target.Replace($project + '\', ''))" -ForegroundColor Green
    $copied++
}
if ($copied -eq 0) { Write-Host "  no new files found - building what is already in the project" -ForegroundColor Yellow }

Write-Host "`n== 2. Java ==" -ForegroundColor Cyan
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jbr = @("C:\Program Files\Android\Android Studio\jbr", "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
             "C:\Program Files\Android\Android Studio Preview\jbr") |
        Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
    if (-not $jbr) { Write-Host "Java not found. Open Android Studio once, or set JAVA_HOME." -ForegroundColor Red; exit 1 }
    $env:JAVA_HOME = $jbr
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "  $env:JAVA_HOME"

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
if (-not (Test-Path ".\local.properties")) { "sdk.dir=$($sdk -replace '\\','\\')" | Out-File -Encoding ascii ".\local.properties" }

Write-Host "`n== 3. Building fossRelease ==" -ForegroundColor Cyan
& .\gradlew.bat assembleFossRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nBUILD FAILED. Copy the red lines starting with 'e:' and send them back." -ForegroundColor Red; exit 1
}
$apk = Get-ChildItem ".\app\build\outputs\apk\foss\release\*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Copy-Item $apk.FullName (Join-Path $project "Redline.apk") -Force
Write-Host "  APK: $(Join-Path $project 'Redline.apk')" -ForegroundColor Green

if ($NoInstall) { exit 0 }

Write-Host "`n== 4. Updating the app (data kept) ==" -ForegroundColor Cyan
$adb = "$sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { Write-Host "adb not found at $adb" -ForegroundColor Red; exit 1 }
$devices = & $adb devices | Select-String "\tdevice$"
if (-not $devices) {
    Write-Host "No emulator/phone running. Start the virtual phone in Android Studio (Device Manager > Play), then run:" -ForegroundColor Yellow
    Write-Host "  & `"$adb`" install -r `"$(Join-Path $project 'Redline.apk')`""
    exit 0
}
& $adb install -r $apk.FullName
if ($LASTEXITCODE -eq 0) { Write-Host "`nDone. Open Redline - your changes are live." -ForegroundColor Green }
