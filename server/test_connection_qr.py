import base64
import re
from urllib.parse import parse_qs, urlsplit

import pytest

from server.connection_qr import pairing_uri, render_page


def test_page_contains_decodable_temporary_qr_and_no_external_assets():
    cv2 = pytest.importorskip("cv2")
    np = pytest.importorskip("numpy")
    page = render_page("http://192.168.100.190:8765", "001973", 1234567890)
    png = base64.b64decode(re.search(r'data:image/png;base64,([A-Za-z0-9+/=]+)', page).group(1))
    image = cv2.imdecode(np.frombuffer(png, dtype=np.uint8), cv2.IMREAD_COLOR)
    value, points, _ = cv2.QRCodeDetector().detectAndDecode(image)
    assert points is not None
    uri = urlsplit(value)
    assert uri.scheme == "luma" and uri.netloc == "connect"
    assert parse_qs(uri.query) == {"v": ["1"], "server": ["http://192.168.100.190:8765"], "code": ["001973"], "expires": ["1234567890"]}
    assert "token" not in page.lower()
    assert "<script src" not in page and "<link " not in page
    assert "Código expirado" in page


def test_invalid_code_or_embedded_credentials_do_not_generate_a_page():
    for endpoint, code in [("https://user:pass@example.com", "123456"), ("http://192.168.1.2/path", "123456"), ("http://8.8.8.8", "123456"), ("http://192.168.1.2:999999", "123456"), ("http://192.168.1.2", "123"), ("http://192.168.1.2", "１２３４５６")]:
        with pytest.raises(ValueError):
            pairing_uri(endpoint, code, 1234567890)
