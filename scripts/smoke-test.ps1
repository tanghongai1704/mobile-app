param(
    [string]$Serial = 'emulator-5554'
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
Push-Location $projectDirectory
try {
    $sdkDirectory = $env:ANDROID_HOME
    if (-not $sdkDirectory) { $sdkDirectory = $env:ANDROID_SDK_ROOT }
    $sdkSetting = Get-Content -LiteralPath 'local.properties' -ErrorAction SilentlyContinue |
        Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
    if ($sdkSetting) {
        $sdkDirectory = $sdkSetting.Substring(8).Replace('\:', ':').Replace('\\', '\')
    }
    if (-not $sdkDirectory) { $sdkDirectory = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
    $adbPath = Join-Path $sdkDirectory 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adbPath)) { throw "ADB not found: $adbPath" }

    $deviceState = & $adbPath -s $Serial get-state 2>&1
    if ($LASTEXITCODE -ne 0 -or "$deviceState".Trim() -ne 'device') {
        throw "Start an emulator in Device Manager first. Device $Serial is not online."
    }
    $bootCompleted = & $adbPath -s $Serial shell getprop sys.boot_completed
    if ($LASTEXITCODE -ne 0 -or "$bootCompleted".Trim() -ne '1') {
        throw 'Wait until the emulator finishes booting, then run this script again.'
    }

    Write-Output 'Building debug app and UI tests...'
    & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain --quiet --max-workers=2
    if ($LASTEXITCODE -ne 0) { throw 'Debug build failed.' }

    foreach ($apk in @(
        'app/build/outputs/apk/debug/app-debug.apk',
        'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    )) {
        if (-not (Test-Path -LiteralPath $apk)) { throw "APK missing: $apk" }
        & $adbPath -s $Serial install -r $apk
        if ($LASTEXITCODE -ne 0) { throw "Failed to install $apk on $Serial" }
    }

    foreach ($setting in @('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale')) {
        & $adbPath -s $Serial shell settings put global $setting 0
        if ($LASTEXITCODE -ne 0) { throw 'Emulator disconnected while preparing tests.' }
    }

    # Verify the instrumentation summary itself: an offline device can otherwise
    # finish a Gradle task without having executed any UI tests.
    $testOutput = & $adbPath -s $Serial shell am instrument -w -r `
        -e class com.example.onespace.NavigationSmokeTest `
        com.example.onespace.test/androidx.test.runner.AndroidJUnitRunner 2>&1
    $testExitCode = $LASTEXITCODE
    $testText = $testOutput -join "`n"
    $testText | Tee-Object -FilePath 'build/emulator-smoke-test.txt' | Write-Output
    if ($testExitCode -ne 0 -or $testText -notmatch 'OK \([1-9][0-9]* tests?\)' -or
        $testText -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {
        throw 'Emulator smoke tests did not pass. See build/emulator-smoke-test.txt.'
    }

    & $adbPath -s $Serial pull /sdcard/Android/data/com.example.onespace/files/screenshots build/
    if ($LASTEXITCODE -ne 0) { throw 'Could not collect emulator screenshots.' }

    $launchOutput = & $adbPath -s $Serial shell am start -W -n com.example.onespace/.MainActivity 2>&1
    $launchText = $launchOutput -join "`n"
    Write-Output $launchText
    if ($LASTEXITCODE -ne 0 -or $launchText -notmatch 'Status: ok') {
        throw 'App failed to launch after testing.'
    }
    Write-Output "OneSpace is running on $Serial."
}
finally {
    Pop-Location
}
