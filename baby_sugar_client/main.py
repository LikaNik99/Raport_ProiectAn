import webview
import requests
import threading

NGINX_URL = "http://127.0.0.1"
APP_TITLE = "Baby Sugar"

OFFLINE_HTML = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Baby Sugar</title>
<style>
body {
  margin: 0;
  background: #111;
  color: white;
  font-family: Arial;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  text-align: center;
}
</style>
</head>
<body>
  <div>
    <h1>Aplicația este offline</h1>
    <p>Se încearcă conectarea la server…</p>
  </div>
</body>
</html>
"""


def server_online(url: str) -> bool:
    try:
        requests.get(url, timeout=2)
        return True
    except:
        return False


def try_load_server(window):
    if server_online(NGINX_URL):
        window.load_url(NGINX_URL)


if __name__ == "__main__":
    window = webview.create_window(
        title=APP_TITLE,
        html=OFFLINE_HTML,
        width=390,
        height=844,
        min_size=(390, 740),
        resizable=True
    )

    threading.Timer(1.0, try_load_server, args=(window,)).start()

    webview.start(gui="edgechromium")
