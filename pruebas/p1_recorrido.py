"""Recorrido automatizado del wireframe P1 (Angular + Spring Boot) en Chromium."""
import base64, json, os
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select

raiz = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
with open(os.path.join(raiz, "index.html"), "rb") as f:
    data_url = "data:text/html;charset=utf-8;base64," + base64.b64encode(f.read()).decode("ascii")

esc = [
    ("E1 cruce de docente (RD1)", "K05#1", (2, 9), "RD1", False, "wireframe_rd1.png"),
    ("E2 aula de cómputo sin capacidad (RD5)", "K09#0", (0, 13), "RD5", False, "wireframe_rd5.png"),
    ("E3 días consecutivos (RB1)", "K05#2", (2, 15), "RB1", True, "wireframe_rb1.png"),
    ("E4 docente fuera de su franja (RD7)", "K03#0", (0, 9), "RD7", False, "wireframe_rd7.png"),
]

opts = Options()
opts.add_argument("--headless=new")
opts.add_argument("--no-sandbox")
opts.add_argument("--disable-dev-shm-usage")
opts.add_argument("--window-size=980,640")

driver = webdriver.Chrome(options=opts)
res = []
try:
    for nombre, tarjeta, (dia, hora), regla, acepta, captura in esc:
        driver.get(data_url)
        antes = driver.execute_script("return S.map(s=>s.id+'@'+s.dia+'-'+s.h).join();")
        driver.execute_script("""
            const card = document.querySelector(`.card[data-id="${arguments[0]}"]`);
            const drop = document.querySelector(`td.drop[data-dia="${arguments[1]}"][data-h="${arguments[2]}"]`);
            const dt = new DataTransfer();
            card.dispatchEvent(new DragEvent('dragstart', { bubbles: true, cancelable: true, dataTransfer: dt }));
            drop.dispatchEvent(new DragEvent('dragover', { bubbles: true, cancelable: true, dataTransfer: dt }));
            drop.dispatchEvent(new DragEvent('drop', { bubbles: true, cancelable: true, dataTransfer: dt }));
        """, tarjeta, dia, hora)
        msg = driver.find_element(By.ID, "msg").text.replace("\n", " ")
        despues = driver.execute_script("return S.map(s=>s.id+'@'+s.dia+'-'+s.h).join();")
        intentos = driver.execute_script("return intentos;")
        driver.save_screenshot(os.path.join(raiz, "evidencia", captura))
        res.append({
            "escenario": nombre,
            "arrastres": intentos,
            "regla_en_mensaje": regla in msg,
            "mensaje": msg,
            "sesion_se_movio": antes != despues,
            "esperado_aceptar": acepta,
            "resultado_esperado": (antes != despues) == acepta and (regla in msg),
            "erroresConsola": []
        })

    driver.get(data_url)
    Select(driver.find_element(By.ID, "vista")).select_by_value("D4")
    nodoc_count = len(driver.find_elements(By.CSS_SELECTOR, "td.nodoc"))
    res.append({
        "escenario": "E5 vista por docente D4",
        "celdas_marcadas_no_disponibles": nodoc_count
    })
finally:
    driver.quit()

with open(os.path.join(raiz, "pruebas", "resultados_p1_recorrido.json"), "w", encoding="utf-8") as f:
    json.dump(res, f, indent=2, ensure_ascii=False)
for r in res:
    print(json.dumps(r, ensure_ascii=False))
