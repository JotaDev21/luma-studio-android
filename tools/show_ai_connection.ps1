param([switch]$NoBrowser)
$ErrorActionPreference='Stop'
$lumaRoot=Split-Path -Parent $PSScriptRoot
$lumaData=Join-Path $lumaRoot '.tools/luma-ai'
$lumaServer=Get-Content -LiteralPath (Join-Path $lumaData 'server-settings.json') -Raw | ConvertFrom-Json
$lumaConnection=Get-Content -LiteralPath (Join-Path $lumaData 'connection.json') -Raw | ConvertFrom-Json
$lumaEndpoint="http://$($lumaServer.listen_address):$($lumaServer.port)"
$lumaReply=Invoke-RestMethod -Uri "$lumaEndpoint/pair/renew" -Method Post -Headers @{Authorization="Bearer $($lumaConnection.token)"} -TimeoutSec 10
Write-Output "Endereço do PC: $lumaEndpoint"
Write-Output "Código para conectar: $($lumaReply.code)"
Write-Output 'Válido por 10 minutos e uma conexão. A conexão já salva no celular continua funcionando.'
$lumaPython=Join-Path $lumaRoot '.tools/luma-ai-venv/Scripts/python.exe'
$lumaQrRequest=Join-Path $lumaData 'qr-request.json'
$lumaQrPage=Join-Path $lumaData 'connection-qr.html'
try {
    @{endpoint=$lumaEndpoint;code=$lumaReply.code;expires_in=$lumaReply.expires_in;expires_at=([DateTimeOffset]::UtcNow.ToUnixTimeSeconds()+[int]$lumaReply.expires_in-5)} | ConvertTo-Json | Set-Content -LiteralPath $lumaQrRequest -Encoding utf8
    & $lumaPython (Join-Path $lumaRoot 'server/connection_qr.py') --request $lumaQrRequest --output $lumaQrPage
    if($LASTEXITCODE -ne 0) {throw 'Instale os requisitos atualizados do servidor para mostrar o QR.'}
    Write-Output "QR de conexão: $lumaQrPage"
    if(!$NoBrowser) {Invoke-Item -LiteralPath $lumaQrPage}
} catch {Write-Warning 'Não foi possível mostrar o QR. Use o endereço/código acima; confira os requisitos em server/README.md.'}
finally {if(Test-Path -LiteralPath $lumaQrRequest) {Remove-Item -LiteralPath $lumaQrRequest -Force}}
