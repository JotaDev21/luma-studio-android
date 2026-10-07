param(
    [string]$ListenAddress='127.0.0.1',
    [ValidateRange(1024,65535)][int]$Port=8765
)
$ErrorActionPreference='Stop'
$lumaRoot=Split-Path -Parent $PSScriptRoot
$lumaPython=Join-Path $lumaRoot '.tools/luma-ai-venv/Scripts/python.exe'
$lumaData=Join-Path $lumaRoot '.tools/luma-ai'
$lumaPidFile=Join-Path $lumaData 'server.pid'
$lumaSettingsFile=Join-Path $lumaData 'server-settings.json'
if(!$PSBoundParameters.ContainsKey('ListenAddress') -and (Test-Path -LiteralPath $lumaSettingsFile)) {
    $lumaSettings=Get-Content -LiteralPath $lumaSettingsFile -Raw | ConvertFrom-Json
    $ListenAddress=$lumaSettings.listen_address
    if(!$PSBoundParameters.ContainsKey('Port')) {$Port=[int]$lumaSettings.port}
}
if(!(Test-Path -LiteralPath $lumaPython)) {throw 'Instale o ambiente Python conforme server/README.md.'}
if($ListenAddress -ne '127.0.0.1') {
    $lumaIp=$null
    if(![System.Net.IPAddress]::TryParse($ListenAddress,[ref]$lumaIp) -or $lumaIp.AddressFamily -ne [System.Net.Sockets.AddressFamily]::InterNetwork) {throw 'Informe um IPv4 local explícito.'}
    $lumaBytes=$lumaIp.GetAddressBytes()
    if(!($lumaBytes[0] -eq 10 -or ($lumaBytes[0] -eq 192 -and $lumaBytes[1] -eq 168) -or ($lumaBytes[0] -eq 172 -and $lumaBytes[1] -ge 16 -and $lumaBytes[1] -le 31))) {throw 'O servidor pessoal aceita apenas endereços da rede privada.'}
    if(!(Get-NetIPAddress -AddressFamily IPv4 | Where-Object IPAddress -eq $ListenAddress)) {throw 'Este endereço não pertence ao PC.'}
}
if(Test-Path -LiteralPath $lumaPidFile) {
    $lumaExistingId=[int](Get-Content -LiteralPath $lumaPidFile)
    $lumaExisting=Get-CimInstance Win32_Process -Filter "ProcessId=$lumaExistingId" -ErrorAction SilentlyContinue
    if($lumaExisting -and $lumaExisting.CommandLine -like '*server.app:create_app*') {throw 'O servidor Luma AI já está aberto. Use stop_ai_server.ps1 antes de reiniciar.'}
}
New-Item -ItemType Directory -Path $lumaData -Force | Out-Null
@{listen_address=$ListenAddress;port=$Port} | ConvertTo-Json | Set-Content -LiteralPath $lumaSettingsFile -Encoding utf8
$env:LUMA_AI_CONFIG=Join-Path $lumaData 'connection.json'
$env:LUMA_AI_WEIGHTS=Join-Path $lumaData 'weights'
$env:PYTHONUNBUFFERED='1'
$lumaArguments=@('-m','uvicorn','server.app:create_app','--factory','--host',$ListenAddress,'--port',"$Port",'--no-access-log')
$lumaProcess=Start-Process -FilePath $lumaPython -ArgumentList $lumaArguments -WorkingDirectory $lumaRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $lumaData 'server-output.log') -RedirectStandardError (Join-Path $lumaData 'server-errors.log') -PassThru
$lumaProcess.Id | Set-Content -LiteralPath $lumaPidFile
Write-Output "Servidor iniciado em segundo plano: http://${ListenAddress}:$Port"
Write-Output 'Aguarde o carregamento da IA. Para obter o endereço e o código temporário, execute tools/show_ai_connection.ps1.'
