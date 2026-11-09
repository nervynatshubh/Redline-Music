<#
  Redline performance check (Windows PowerShell).

  Measures on a connected phone or emulator:
    1. Cold start time (3 launches, average)
    2. Scrolling smoothness: % of janky (dropped) frames while auto-scrolling Home
    3. Memory use (total PSS)
    4. Current screen refresh rate

  Usage (from the project folder, phone plugged in with USB debugging on):
    powershell -ExecutionPolicy Bypass -File tools\redline-perf.ps1
  Optional: -Package com.metrolist.music.apple   (default: the fossRelease build)
#>
param(
    [string]$Package = "com.metrolist.music.apple",
    [int]$Swipes = 20
)

$ErrorActionPreference = "Stop"

# Find adb: PATH first, then the default Android Studio SDK location.
$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) {
    $candidates = @("$env:ANDROID_HOME\platform-tools\adb.exe", "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe")
    $adb = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $adb) { Write-Host "adb not found. Install Android SDK Platform-Tools from Android Studio." -ForegroundColor Red; exit 1 }

$devices = & $adb devices | Select-String "\tdevice$"
if (-not $devices) { Write-Host "No phone/emulator connected (check USB debugging)." -ForegroundColor Red; exit 1 }

$installed = & $adb shell pm list packages $Package
if (-not ($installed -match "package:$Package$")) {
    Write-Host "$Package is not installed. Build and run fossRelease first, or pass -Package <id>." -ForegroundColor Red; exit 1
}

function Show-Result($name, $value, $target, [bool]$ok) {
    $color = if ($ok) { "Green" } else { "Yellow" }
    $mark = if ($ok) { "PASS" } else { "CHECK" }
    Write-Host ("{0,-28} {1,-14} target {2,-12} {3}" -f $name, $value, $target, $mark) -ForegroundColor $color
}

Write-Host "`nRedline performance check on $Package`n" -ForegroundColor Cyan

# Screen size, used to aim the swipes at the middle of the screen.
$size = (& $adb shell wm size) -replace ".*: ", ""
$w, $h = $size.Trim().Split("x") | ForEach-Object { [int]$_ }
$x = [int]($w / 2); $yLow = [int]($h * 0.75); $yHigh = [int]($h * 0.25)

# 1. Cold start
$times = @()
for ($i = 1; $i -le 3; $i++) {
    & $adb shell am force-stop $Package | Out-Null
    Start-Sleep -Milliseconds 800
    $out = & $adb shell am start -W -n "$Package/com.metrolist.music.MainActivity"
    $t = ($out | Select-String "TotalTime: (\d+)").Matches.Groups[1].Value
    if ($t) { $times += [int]$t }
    Start-Sleep -Seconds 2
}
$avgStart = [int](($times | Measure-Object -Average).Average)
Show-Result "Cold start" "$avgStart ms" "< 1000 ms" ($avgStart -lt 1000)

# 2. Scrolling: reset frame stats, auto-scroll Home down and up, read jank.
Start-Sleep -Seconds 3
& $adb shell dumpsys gfxinfo $Package reset | Out-Null
for ($i = 0; $i -lt $Swipes; $i++) {
    if ($i % 2 -eq 0) { & $adb shell input swipe $x $yLow $x $yHigh 140 } else { & $adb shell input swipe $x $yHigh $x $yLow 140 }
    Start-Sleep -Milliseconds 450
}
$gfx = & $adb shell dumpsys gfxinfo $Package
$total = [int](($gfx | Select-String "Total frames rendered: (\d+)").Matches.Groups[1].Value)
$jankLine = ($gfx | Select-String "Janky frames: (\d+) \(([\d.]+)%\)").Matches
$jankPct = if ($jankLine) { [double]$jankLine.Groups[2].Value } else { -1 }
$p90 = ($gfx | Select-String "90th percentile: (\d+)ms").Matches.Groups[1].Value
$p99 = ($gfx | Select-String "99th percentile: (\d+)ms").Matches.Groups[1].Value
Show-Result "Frames rendered" "$total" "-" $true
Show-Result "Janky frames" "$jankPct %" "< 5 %" ($jankPct -ge 0 -and $jankPct -lt 5)
Show-Result "90th pct frame time" "$p90 ms" "<= 16 ms" ([int]$p90 -le 16)
Show-Result "99th pct frame time" "$p99 ms" "<= 25 ms" ([int]$p99 -le 25)

# 3. Memory
$mem = & $adb shell dumpsys meminfo $Package
$pss = ($mem | Select-String "TOTAL PSS:\s+(\d+)").Matches.Groups[1].Value
if (-not $pss) { $pss = ($mem | Select-String "^\s+TOTAL\s+(\d+)").Matches.Groups[1].Value }
$pssMb = [int]([int]$pss / 1024)
Show-Result "Memory (PSS)" "$pssMb MB" "< 300 MB" ($pssMb -lt 300)

# 4. Refresh rate the screen is running at
$disp = & $adb shell dumpsys display
$rate = ($disp | Select-String "mRefreshRate=([\d.]+)" | Select-Object -First 1).Matches.Groups[1].Value
if (-not $rate) { $rate = ($disp | Select-String "refreshRate=([\d.]+)" | Select-Object -First 1).Matches.Groups[1].Value }
Write-Host ("{0,-28} {1} Hz" -f "Display refresh rate", $rate) -ForegroundColor Cyan

Write-Host "`nDone. Copy everything above and send it back for tuning.`n"
