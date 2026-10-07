"""Build a local pairing page from a temporary code, never from a bearer token."""
import argparse
import base64
import html
import io
import ipaddress
import json
import time
from pathlib import Path
from urllib.parse import urlencode, urlsplit

import qrcode


def pairing_uri(endpoint: str, code: str, expires_at: int) -> str:
    server = urlsplit(endpoint)
    if server.scheme not in ("http", "https") or not server.hostname or server.username is not None or server.password is not None or server.path or server.query or server.fragment:
        raise ValueError("Use apenas o endereço do servidor Luma.")
    if server.port is not None and not 1 <= server.port <= 65535:
        raise ValueError("Porta inválida.")
    if server.scheme == "http" and server.hostname != "localhost":
        address = ipaddress.ip_address(server.hostname)
        private_lan = any(address in ipaddress.ip_network(network) for network in ("127.0.0.0/8", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"))
        if not private_lan:
            raise ValueError("Fora da rede local, use HTTPS.")
    if len(code) != 6 or not code.isascii() or not code.isdigit():
        raise ValueError("Código de conexão inválido.")
    if not isinstance(expires_at, int) or expires_at <= 0:
        raise ValueError("Validade do código inválida.")
    return "luma://connect?" + urlencode({"v": "1", "server": endpoint, "code": code, "expires": expires_at})


def render_page(endpoint: str, code: str, expires_at: int) -> str:
    payload = pairing_uri(endpoint, code, expires_at)
    output = io.BytesIO()
    qrcode.make(payload, error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=8, border=4).save(output, format="PNG")
    image = base64.b64encode(output.getvalue()).decode("ascii")
    return f'''<!doctype html><html lang="pt-BR"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; script-src 'unsafe-inline'">
<title>Conectar o Luma ao PC</title><style>
body{{margin:0;background:#111418;color:#f5f4f0;font:20px/1.5 system-ui}}
main{{max-width:620px;margin:40px auto;padding:24px;background:#1c2026;border-radius:24px}}
h1{{font-size:32px}}img{{display:block;width:min(100%,360px);height:auto;margin:24px auto;border-radius:12px}}
strong{{color:#f0ab83}}code{{overflow-wrap:anywhere}}.expired img{{display:none}}.expired #code{{display:none}}
</style><main id="connection"><h1>Conecte seu celular</h1>
<p>1. No Luma, toque em <strong>Melhorar com IA</strong>.</p>
<p>2. Escolha <strong>Ler QR do PC</strong> e aponte a câmera.</p>
<p>3. Confira o endereço e toque em <strong>Conectar PC</strong>.</p>
<img src="data:image/png;base64,{image}" alt="QR temporário para conectar ao servidor Luma deste PC">
<p>PC: <code>{html.escape(endpoint)}</code></p>
<p id="code">Código manual: <strong>{html.escape(code)}</strong></p>
<p id="expiry" role="status">Código temporário, válido por uma conexão.</p>
<p>Use a mesma rede Wi-Fi. Suas fotos só são enviadas quando você pedir uma melhoria.</p>
<p>Para gerar outro código, abra novamente <strong>Ver conexão Luma IA.cmd</strong>.</p></main>
<script>const expires={expires_at};function tick(){{const left=Math.max(0,expires-Math.floor(Date.now()/1000));
document.getElementById('expiry').textContent=left?'Expira em '+Math.floor(left/60)+' min '+(left%60)+' s.':'Código expirado. Abra Ver conexão Luma IA.cmd para gerar outro.';
if(!left){{document.getElementById('connection').classList.add('expired');clearInterval(timer)}}}}
const timer=setInterval(tick,1000);tick();</script></html>'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--request", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    data = json.loads(args.request.read_text(encoding="utf-8-sig"))
    expires = int(data.get("expires_at", int(time.time()) + int(data["expires_in"]) - 5))
    if not 0 < int(data["expires_in"]) <= 600:
        raise ValueError("Validade do código fora do limite.")
    args.output.write_text(render_page(data["endpoint"], data["code"], expires), encoding="utf-8")


if __name__ == "__main__":
    main()
