# Luma AI no PC

Serviço privado para restaurar fotos com Real-ESRGAN x4plus e rostos com GFPGAN v1.4. Requer GPU NVIDIA/CUDA, Python 3.12, PyTorch 2.14.1/CUDA 13.0 e torchvision 0.29.1. Os modelos e as credenciais ficam em `.tools/`, fora do Git. Não é a tecnologia proprietária do Remini e não promete os mesmos resultados.

## Usar

Depois da instalação, abrir `Iniciar Luma IA.cmd` inicia o serviço em segundo plano. `Parar Luma IA.cmd` encerra somente os processos identificados do serviço. Os scripts guardam o endereço/porta escolhidos no primeiro início. Não há tarefa automática de inicialização do Windows.

No app: **Melhorar com IA → Ler QR do PC**. No computador, abrir **Ver conexão Luma IA.cmd** mostra uma página local com QR e código temporário. Aponte o leitor do Luma para esse QR e confirme o endereço apresentado. O QR contém somente endereço, código temporário e vencimento; o token permanente não entra no QR. Não há envio de foto ao ler ou revisar o QR.

A entrada manual de endereço/código continua disponível. O código vence em dez minutos, aceita uma conexão e no máximo dez tentativas. A página esconde o QR e o código quando vencem; abra o comando novamente para gerar outro. Gerar outro código ou reiniciar o servidor não invalida a conexão já salva no app. O token permanente é cifrado com uma chave do Android Keystore; não consta do APK.

O leitor usa o Google Play Services e não exige permissão de câmera do Luma. Na primeira utilização, seu módulo pode precisar de download. Em aparelho sem o leitor disponível, use a entrada manual. Depois de conectar, **Verificar PC novamente** confere se a IA está pronta, carregando, ocupada ou indisponível antes de liberar a melhoria. Uma resposta pronta não garante que o PC continuará ligado durante a operação.

O QR requer `qrcode==8.2`, incluído nos requisitos atualizados. Para atualizar apenas essa dependência: `.tools/luma-ai-venv/Scripts/python.exe -m pip install qrcode==8.2`. A página privada fica em `.tools/luma-ai/connection-qr.html`; não a publique. Para gerar sem abrir navegador: `tools/show_ai_connection.ps1 -NoBrowser`.

O celular e o PC devem estar na mesma rede e o servidor precisa estar aberto. Uma mudança de IP exige reconectar pelo novo endereço. Fora da rede privada o cliente exige HTTPS; não segue redirecionamentos. Na rede local, HTTP precisa ser usado apenas em rede confiável. Não exponha a porta diretamente na internet; uma implantação remota exige HTTPS e revisão da autenticação.

## Instalação reproduzível

Criar um ambiente virtual Python 3.12 separado em `.tools/luma-ai-venv` (os launchers usam esse caminho). No ambiente:

```powershell
python -m pip install torch==2.14.1 torchvision==0.29.1 --index-url https://download.pytorch.org/whl/cu130
python -m pip install -r server/requirements.txt
python server/download_models.py
```

O script de download usa somente releases oficiais e verifica os hashes dos quatro modelos. A instalação feita nesta máquina usa `.tools/luma-ai-venv`. Para iniciar manualmente:

```powershell
./tools/start_ai_server.ps1 -ListenAddress SEU_IPV4_PRIVADO -Port 8765
```

A regra de firewall deve permitir somente o programa Python, porta TCP escolhida, endereço local do PC e IP do celular. O servidor não se associa a `0.0.0.0` por padrão. Os testes do protocolo não precisam da GPU:

```powershell
python -m pytest server/test_service.py -q
```

No ambiente com PyTorch instalado, `python -m pytest server/test_runtime.py -q` verifica a composição dos tiles e a propagação de falhas. `python server/verify_gpu.py` executa os modelos reais pelo servidor usando apenas a fixture pública de retrato NASA; requer o serviço pronto e salva as comparações locais em `.tools/luma-ai/checks`.

`python -m pytest server/test_connection_qr.py -q` verifica a geração e decodificação real do QR, vencimento da página e rejeição de entradas inválidas. Esse teste usa OpenCV já presente nos requisitos do servidor.

## Integridade dos modelos

O servidor confere os quatro hashes SHA-256 antes de carregar os modelos. Se um arquivo estiver ausente ou modificado, rode novamente o passo de download após retirar o arquivo inválido da pasta de modelos.

A rotina de treinamento distribuído via SLURM do BasicSR fica desativada no servidor de inferência. A dependência ainda é identificada pelo aviso [CVE-2024-27763](https://github.com/advisories/GHSA-86w8-vhw6-q9qq), relativo a essa rotina.

## Comportamento e limites

- Somente a imagem editada é enviada, codificada em PNG sem EXIF/GPS. O app prepara a entrada em até 2048 px no maior lado. A melhoria gera até 4096 px, com fator pedido de até 2×/4×. Resultados mantêm a proporção e transparência.
- Comparação e força ajustáveis. Restaurar rostos pode alterar detalhes ou identidade; conferir o resultado antes de aceitar. O modelo não recupera informação verdadeira garantida de uma foto desfocada.
- Confirmar salva uma cópia com a melhoria incorporada aos pixels, conservando o projeto anterior e suas edições. Descartar remove o resultado temporário. Não há mudança no arquivo original da galeria.
- Uma inferência por vez, sem fila de fotos. Uploads até 64 MB, entrada até 32 MP/8192 px por lado, saída até 32 MP. Erros de inferência são erros; não são substituídos por simples nitidez.
- Cancelar no app interrompe a espera/conexão. Uma inferência já iniciada no PC pode terminar antes de liberar a GPU. Os buffers de imagem são liberados após a operação; os modelos continuam carregados.
- Não há persistência de fotos no servidor, registros de acesso ou corpos de pedidos. Imagens e resultados passam pela memória; apenas modelos, configurações e logs técnicos permanecem no PC.

Fontes: [Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN) (BSD-3-Clause), [GFPGAN](https://github.com/TencentARC/GFPGAN) (Apache-2.0), [facexlib](https://github.com/xinntao/facexlib), [PyTorch/CUDA](https://pytorch.org/get-started/previous-versions/). Bibliotecas e pesos permanecem sujeitos às condições dos respectivos projetos.
