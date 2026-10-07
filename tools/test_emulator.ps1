param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^emulator-[0-9]+$')]
    [string]$EmulatorSerial,
    [Parameter(Mandatory = $true)]
    [string]$SdkPath,
    [string]$TestClasses = 'studio.luma.FlashEffectsTest,studio.luma.FlashEffectsFlowTest,studio.luma.PhotoStudioFlowTest'
)

$ErrorActionPreference = 'Stop'
$lumaRoot = Split-Path -Parent $PSScriptRoot
$lumaAdb = Join-Path $SdkPath 'platform-tools/adb.exe'
$lumaApk = Join-Path $lumaRoot 'app/build/outputs/apk/debug/app-debug.apk'
$lumaTests = Join-Path $lumaRoot 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
$lumaResults = Join-Path $lumaRoot 'app/build/emulator-checks'
$lumaRemoteOutput = '/sdcard/Android/media/studio.luma.personal.qa/emulator-checks'

# Always target a named local emulator. Never enumerate or select physical devices.
foreach ($lumaFile in @($lumaAdb, $lumaApk, $lumaTests)) {
    if (!(Test-Path -LiteralPath $lumaFile -PathType Leaf)) { throw "Arquivo ausente: $lumaFile" }
}
$lumaState = & $lumaAdb -s $EmulatorSerial get-state
if ($LASTEXITCODE -ne 0 -or $lumaState -ne 'device') { throw 'O emulador indicado não está pronto.' }
New-Item -ItemType Directory -Path $lumaResults -Force | Out-Null
foreach ($lumaFile in @($lumaApk, $lumaTests)) {
    & $lumaAdb -s $EmulatorSerial install -r $lumaFile
    if ($LASTEXITCODE -ne 0) { throw "Não foi possível instalar no emulador: $lumaFile" }
}
$lumaRun = & $lumaAdb -s $EmulatorSerial shell am instrument -w -r -e class $TestClasses -e additionalTestOutputDir $lumaRemoteOutput 'studio.luma.personal.qa.test/androidx.test.runner.AndroidJUnitRunner' 2>&1
$lumaRun | Set-Content -LiteralPath (Join-Path $lumaResults 'instrumentation.log') -Encoding utf8
$lumaSummary = $lumaRun -join "`n"
if ($LASTEXITCODE -ne 0 -or $lumaSummary -notmatch 'OK \([0-9]+ tests?\)' -or $lumaSummary -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {
    $lumaRun | Select-Object -Last 45
    throw "Os testes não concluíram com sucesso. Consulte $lumaResults"
}
& $lumaAdb -s $EmulatorSerial pull $lumaRemoteOutput $lumaResults
if ($LASTEXITCODE -ne 0) { throw 'Os testes passaram, mas as capturas não foram copiadas.' }
$lumaRun | Select-Object -Last 12
Write-Output "Resultados locais: $lumaResults"
