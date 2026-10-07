$ErrorActionPreference='Stop'
$lumaRoot=Split-Path -Parent $PSScriptRoot
$lumaPidFile=Join-Path $lumaRoot '.tools/luma-ai/server.pid'
if(!(Test-Path -LiteralPath $lumaPidFile)) {Write-Output 'Servidor já está parado.';return}
$lumaServerId=[int](Get-Content -LiteralPath $lumaPidFile)
$lumaProcess=Get-CimInstance Win32_Process -Filter "ProcessId=$lumaServerId" -ErrorAction SilentlyContinue
if($lumaProcess) {
    if($lumaProcess.CommandLine -notlike '*server.app:create_app*') {throw 'O identificador não pertence ao servidor Luma AI; nenhum processo foi encerrado.'}
    Get-CimInstance Win32_Process -Filter "ParentProcessId=$lumaServerId" | Where-Object {$_.CommandLine -like '*server.app:create_app*'} | ForEach-Object {Stop-Process -Id $_.ProcessId -ErrorAction SilentlyContinue}
    Stop-Process -Id $lumaServerId -ErrorAction SilentlyContinue
}
Remove-Item -LiteralPath $lumaPidFile
Write-Output 'Servidor Luma AI parado.'
