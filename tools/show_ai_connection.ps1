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
