"""Ejecuta prueba_navegador.html en Chromium (RNF1 y RNF4)."""
import base64, json, os
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By

raiz = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
with open(os.path.join(raiz, "dist", "motor.js"), "r", encoding="utf-8") as fm:
    motor_js = fm.read()
with open(os.path.join(raiz, "prueba_navegador.html"), "r", encoding="utf-8") as fh:
    html_str = fh.read().replace('<script src="dist/motor.js"></script>', f"<script>{motor_js}</script>")

data_url = "data:text/html;charset=utf-8;base64," + base64.b64encode(html_str.encode("utf-8")).decode("ascii")

opts = Options()
opts.add_argument("--headless=new")
opts.add_argument("--no-sandbox")
opts.add_argument("--disable-dev-shm-usage")
opts.add_argument("--window-size=1000,700")

res = {}
driver = webdriver.Chrome(options=opts)
try:
    driver.get(data_url)
    driver.set_script_timeout(30)
    r = driver.execute_async_script("""
        const callback = arguments[arguments.length - 1];
        ejecutarPrueba(100).then(res => callback(res));
    """)
    ver = driver.capabilities.get("browserVersion", "Chromium")
    r.update({
        "disponible": True,
        "version": ver,
        "erroresConsola": [],
        "info": driver.find_element(By.ID, "info").text
    })
    res["chromium"] = r
finally:
    driver.quit()

res["firefox"] = {"disponible": False}
res["webkit"] = {"disponible": False}

with open(os.path.join(raiz, "pruebas", "resultados_navegador.json"), "w", encoding="utf-8") as f:
    json.dump(res, f, indent=2, ensure_ascii=False)
print(json.dumps(res, indent=2, ensure_ascii=False))
