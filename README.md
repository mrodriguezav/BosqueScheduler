# BosqueScheduler

> **Sistema de Programación y Validación de Horarios Académicos Universitarios**  
> **Proyecto Núcleo 2** — 6.º Semestre, Facultad de Ingeniería de Sistemas, Universidad El Bosque (UEB).  
> **Entregable:** Artefacto de Software — Entrega Final Módulo 3 (M1, M2 y M3).

---

## Tabla de Contenidos

1. [Descripción General](#descripción-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Catálogo de Reglas y Restricciones](#catálogo-de-reglas-y-restricciones)
4. [Requisitos del Sistema](#requisitos-del-sistema)
5. [Instalación y Configuración](#instalación-y-configuración)
6. [Guía de Ejecución y Despliegue Local](#guía-de-ejecución-y-despliegue-local)
   - [Opción A: Consola Web Integrada en Backend (Puerto 8080)](#opción-a-consola-web-integrada-en-backend-puerto-8080)
   - [Opción B: Servidor de Desarrollo Angular 18 + Backend REST (Puertos 4200 y 8080)](#opción-b-servidor-de-desarrollo-angular-18--backend-rest-puertos-4200-y-8080)
   - [Operación de la Consola de Administración](#operación-de-la-consola-de-administración)
7. [Suite de Pruebas y Benchmarks (7 Baterías)](#suite-de-pruebas-y-benchmarks-7-baterías)
8. [Referencia y Verificación de la API REST](#referencia-y-verificación-de-la-api-rest)
9. [Estructura del Proyecto](#estructura-del-proyecto)
10. [Cumplimiento de Requerimientos (RF y RNF)](#cumplimiento-de-requerimientos-rf-y-rnf)

---

## Descripción General

**BosqueScheduler** automatiza la programación, validación y ajuste interactivo de horarios de clases del Programa de Ingeniería de Sistemas (caso base de 20 asignaturas con 41 sesiones semanales de 2 horas, 12 docentes en dedicaciones TC, TQ, MT y UQ, 12 aulas en tipologías estándar, cómputo y móvil, y 32 bloques semanales de lunes a sábado respetando la hora de almuerzo de 12:00 a 13:00).

El sistema garantiza **0 violaciones duras** mediante su motor combinatorio en Java 21 (`GeneradorVoraz` con prefiltro en $O(1)$ y retroceso de un nivel) y su verificador independiente (`Auditor`), ofreciendo validación en tiempo real (< 200 ms) durante ajustes manuales por arrastrar y soltar (RF2), reasignación de aula (RF3), modificación de parámetros normativos (RF6), carga de catálogos JSON y exportación a CSV/JSON.

---

## Arquitectura del Sistema

```text
┌────────────────────────────────────────────────────────────┐
│           Cliente Web (Consola HTML5 + Angular 18)         │
│   • Encabezado de estado, Auditor y filtro "Ver por"       │
│   • Barra de administración (1. Carga, 2. RF6, 3. RF3, 4.) │
│   • Grilla interactiva Drag & Drop + Panel Sin ubicar RF5  │
└─────────────────────────────┬──────────────────────────────┘
                              │ HTTP REST JSON / CSV (8080)
┌─────────────────────────────▼──────────────────────────────┐
│            Backend (Java 21 / Spring Boot REST)            │
│   • ServidorHttpSpringBoot / HorarioController             │
│   • HorarioService / SistemaHorarios                       │
│   • GeneradorVoraz (Voraz + Prefiltro + Retroceso 1 nivel) │
│   • ValidadorRestricciones (Patrón GoF Strategy - 9 reglas)│
│   • Horario (Mapas hash privados en O(1)) y Auditor        │
└────────────────────────────────────────────────────────────┘
```

### Decisiones Arquitectónicas y Patrones Clave:
* **Patrón GoF Strategy (RNF3 / OCP):** La clase abstracta `Regla` define el contrato `evaluar(Sesion, Horario)`. `CatalogoReglas` registra las 9 estrategias concretas sin acoplar a `GeneradorVoraz` ni a `ValidadorRestricciones`.
* **Índices Hash Incrementales en $O(1)$ (RNF1 / RNF2):** `Horario` encapsula como `private final` los mapas `sesiones`, `ocDocente`, `ocAula`, `carga` y `porCurso`, centralizando toda mutación en `colocar`, `quitar` y `mover`.
* **Poda Temprana por Bloque (`esValidaSinAula`):** Evalúa una sola vez por bloque las 7 reglas donde `usaAula == false` antes de iterar sobre las aulas compatibles (`RD2` y `RD5`), reduciendo el 95,8 % de las evaluaciones en pruebas de estrés ($k = 32$).
* **Modelo Relacional en 3FN:** Formalizado en `sql/schema_3fn.sql` para PostgreSQL 16 con 9 tablas normalizadas y restricciones declarativas `PK`, `FK`, `UK1`, `UK2` y `CHECK`.

---

## Catálogo de Reglas y Restricciones

| Código | Severidad por defecto | Clase en `reglas/` | Verificación en el dominio |
| :---: | :---: | :--- | :--- |
| **RD1** | Dura (Bloquea) | `CruceDocente` | Impide que un profesor dicte dos cursos en el mismo bloque horario. |
| **RD2** | Dura (Bloquea) | `CruceAula` | Impide que dos cursos ocupen el mismo salón en el mismo bloque. |
| **RD3** | Dura (Bloquea) | `DocenteHabilitado` | Verifica que el profesor pertenezca a la lista de docentes autorizados del curso. |
| **RD4** | Dura (Bloquea) | `FranjaHoraria` | Garantiza que ninguna sesión invada el almuerzo (12:00–13:00) ni quede fuera de jornada. |
| **RD5** | Dura (Bloquea) | `AulaCompatible` | Comprueba que el salón sea del tipo exigido (`estandar`, `computo`, `movil`) y cubra el cupo requerido. |
| **RD6** | Dura (Bloquea) | `DuracionSesion` | Exige que toda sesión ocupe exactamente un bloque completo de 2 horas continuas. |
| **RD7** | Dura (Bloquea) | `DisponibilidadDocente` | Impide programar a un profesor fuera de sus días u horas de disponibilidad. |
| **RD8** | Dura (Bloquea) | `TopeDedicacion` | Controla el límite semanal según vinculación: `TC = 8`, `TQ = 6`, `MT = 4`, `UQ = 2` sesiones. |
| **RB1** | Aviso (Permite con alerta) | `DiasConsecutivos` | Advierte si un curso de 2 o 3 sesiones semanales queda en días consecutivos (reclasificable a dura). |

---

## Requisitos del Sistema

* **Java Development Kit (JDK):** OpenJDK / Oracle JDK 21 LTS (compatible con JDK 17+).
* **Node.js y npm:** Node.js 20 LTS+ y npm 9+ (para cliente Angular 18 y empaquetado de `dist/motor.js`).
* **Base de Datos (Opcional para DDL):** PostgreSQL 16+ para ejecutar `sql/schema_3fn.sql` (el motor opera en memoria sin requerir instancia activa de BD).
* **Python (Opcional para pruebas en navegador):** Python 3.10+ con `matplotlib` y `selenium` para diagramas y recorridos E2E en Chromium.

---

## Instalación y Configuración

### 1. Clonar el Repositorio
```bash
git clone https://github.com/mrodriguezav/BosqueScheduler.git
cd BosqueScheduler
```

### 2. Compilar el Motor Java y Ejecutar las 7 Suites de Verificación

* **En Windows (PowerShell):**
  ```powershell
  .\ejecutar_todo.ps1
  ```

* **En Linux / macOS / Git Bash:**
  ```bash
  chmod +x ejecutar_todo.sh
  ./ejecutar_todo.sh
  ```

Si solo se desea compilar el backend manualmente sin ejecutar las pruebas de navegador:

* **Windows (PowerShell):**
  ```powershell
  New-Item -ItemType Directory -Force -Path "backend-springboot\target\classes", "bin\main" | Out-Null
  $mainFiles = (Get-ChildItem -Path backend-springboot\src\main\java -Filter *.java -Recurse).FullName
  javac -encoding UTF-8 -d backend-springboot\target\classes $mainFiles
  javac -encoding UTF-8 -d bin\main $mainFiles
  ```

* **Linux / macOS (Bash):**
  ```bash
  mkdir -p backend-springboot/target/classes bin/main
  javac -encoding UTF-8 -d backend-springboot/target/classes $(find backend-springboot/src/main/java -name "*.java")
  ```

### 3. Instalar Dependencias del Cliente Angular 18 (Opcional para SPA en puerto 4200)
```bash
cd frontend-angular
npm install
cd ..
```

---

## Guía de Ejecución y Despliegue Local

### Opción A: Consola Web Integrada en Backend (Puerto 8080)

El servidor HTTP embebido sirve directamente la consola de administración y los 5 endpoints REST en un solo proceso:

```powershell
java -cp backend-springboot/target/classes co.edu.unbosque.horarios.HorariosApplication --server.port=8080
```
*(También puede ejecutarse con `java -cp bin/main co.edu.unbosque.horarios.HorariosApplication`).*

Una vez activo, abrir en el navegador:
* **Consola de Administración:** `http://127.0.0.1:8080/index.html`
* **Estado JSON de la API:** `http://127.0.0.1:8080/api/horario`

### Opción B: Servidor de Desarrollo Angular 18 + Backend REST (Puertos 4200 y 8080)

Para ejecutar el entorno de desarrollo de Angular conectado al backend mediante proxy:

1. **Terminal 1 (Backend en puerto 8080):**
   ```powershell
   java -cp backend-springboot/target/classes co.edu.unbosque.horarios.HorariosApplication --server.port=8080
   ```
2. **Terminal 2 (Frontend Angular en puerto 4200):**
   ```powershell
   cd frontend-angular
   npm start
   ```
3. Abrir en el navegador: `http://localhost:4200`

### Operación de la Consola de Administración

La interfaz principal se organiza en cinco zonas operativas:
1. **Encabezado de estado y auditoría:** Muestra el conteo de sesiones ubicadas (`39/41 sesiones ubicadas · Auditor: 0 violaciones duras`) y el filtro desplegable **Ver por** para sombrear las celdas incompatibles de cada docente (RD7 / RD1).
2. **Barra de herramientas del administrador (4 módulos):**
   * **1. Carga de datos:** Botón `Cargar JSON (20 cursos / 12 doc / 12 aulas)` que invoca `POST /api/horario/cargar`.
   * **2. Parámetros (RF6):** Controles de `Cupo` (por defecto 40; al reducir a 30 habilita Bases de Datos K09 en salas de cómputo), severidad de `RB1` (`Aviso (blanda)` o `Dura (bloquea)`), hora de `Inicio` (`07:00` con 32 bloques o `06:00` con 37 bloques) y botón `Regenerar (RF1)`.
   * **3. Reasignar aula (RF3):** Selector de sesión ubicada, selector de aula de destino y botón `Cambiar aula` con validación inmediata de `RD5` y `RD2`.
   * **4. Exportar:** Botones `Exportar CSV` (`horario_bosquescheduler.csv` con 40 líneas) y `Exportar JSON` (`horario_bosquescheduler.json`).
3. **Panel lateral izquierdo «Sin ubicar (RF5)» y resumen del catálogo:** Lista asignaturas pendientes con su causa técnica exacta y el resumen del catálogo activo.
4. **Grilla semanal interactiva:** Tabla de lunes a sábado con tarjetas arrastrables para traslado manual de sesiones (RF2).
5. **Panel de diagnóstico y bitácora de auditoría:** Recuadro cromático (verde para aceptado, amarillo para aviso blando RB1, rojo para rechazo por regla dura) e historial cronológico de intentos.

---

## Suite de Pruebas y Benchmarks (7 Baterías)

| # | Clase / Script de Prueba | Propósito y Evidencia Generada | Resultado |
| :-: | :--- | :--- | :---: |
| **1** | `PruebaBasicaTest` | Verifica los 32 bloques semanales, 39/41 sesiones ubicadas, reporte de K09 por RD5 y 0 violaciones del Auditor. | 39/41 ubicadas, 0 violaciones |
| **2** | `EjecutarP2Test` | Evalúa 30 semillas aleatorias (v1 voraz vs. v2 con retroceso: 30/30 exitosas), 100 movimientos manuales (< 200 ms), benchmark Strategy vs. Monolítico y escalabilidad $k=1..32$. | `pruebas/resultados_p2.json` |
| **3** | `CambiosFTest` | Prueba los Cambios A (cupo 30 -> 41/41), B (nueva regla dura RD9 sin tocar motor), C (RB1 dura) y D (inicio 6:00 -> 37 bloques). | `pruebas/resultados_cambios.json` |
| **4** | `CuelloBotellaTest` | Perfilado de rechazos por regla y poda del 95,8 % con `esValidaSinAula` en $k=1, 8, 32$. | `pruebas/resultados_cuello.json` |
| **5** | `VerificarDiagramasTest` | 64 comprobaciones reflexivas sobre firmas, atributos `private final`, marcas `usaAula` y trazas de secuencia UML. | 64 / 64 superadas |
| **6** | `PruebasPersonasP1Test` | Verifica el protocolo de usabilidad con 4 participantes sobre los escenarios E1 a E4 (< 68 s). | `pruebas/resultados_p1_personas.json` |
| **7** | `PruebasEntregaFinalM3Test` | Suite QA integral del Módulo 3 con 15 aserciones deterministas (`RF1`, `RD1`–`RD8`, `RB1-A/B`, `RF3`, `M3-CARGA`, `M3-EXP`). | 15 / 15 superadas (`pruebas/qa_m3_execution.log`) |

---

## Referencia y Verificación de la API REST

Con el servidor activo en `http://127.0.0.1:8080`, los cinco endpoints pueden verificarse desde terminal:

```bash
# 1. Consultar el estado completo del horario y el certificado del Auditor:
curl -s http://127.0.0.1:8080/api/horario

# 2. Cargar el archivo de datos de prueba (20 asignaturas, 12 docentes, 12 aulas) y regenerar:
curl -s -X POST http://127.0.0.1:8080/api/horario/cargar -H "Content-Type: application/json" -d @datos/datos_prueba_20cursos.json

# 3. Ejecutar un movimiento o reasignación de aula válida (RF2 / RF3: K03#0 al aula E5):
curl -s -X POST http://127.0.0.1:8080/api/horario/mover -H "Content-Type: application/json" -d '{"sesionId":"K03#0","bloqueId":"0-17","aulaId":"E5"}'

# 4. Forzar una prueba de ruptura por línea de comandos (RD7: D4 a las 9:00 a. m.):
curl -s -X POST http://127.0.0.1:8080/api/horario/mover -H "Content-Type: application/json" -d '{"sesionId":"K03#0","bloqueId":"0-9","aulaId":"E4"}'

# 5. Exportar el horario validado a formato CSV (40 líneas) o JSON:
curl -s "http://127.0.0.1:8080/api/horario/exportar?formato=csv" -o horario_exportado.csv
curl -s "http://127.0.0.1:8080/api/horario/exportar?formato=json" -o horario_exportado.json
```

---

## Estructura del Proyecto

```text
BosqueScheduler/
├── backend-springboot/              # Núcleo combinatorio y servidor HTTP/REST (Java 21)
│   ├── pom.xml                      # Especificación Maven (Spring Boot 3.3.4)
│   └── src/
│       ├── main/java/co/edu/unbosque/horarios/
│       │   ├── HorariosApplication.java       # Punto de entrada JVM (--server.port)
│       │   ├── config/Configuracion.java      # Parámetros normativos (RF6)
│       │   ├── controller/                    # HorarioController y ServidorHttpSpringBoot
│       │   ├── datos/DatosEjemplo.java        # Catálogo sintético base y replicador (k=1..32)
│       │   ├── model/                         # Contexto, Horario, Sesion, Curso, Docente, Aula, Bloque, Violacion
│       │   ├── reglas/                        # Patrón Strategy: Regla, CatalogoReglas y 9 reglas concretas
│       │   └── service/                       # SistemaHorarios, GeneradorVoraz, ValidadorRestricciones, Auditor, HorarioService
│       └── test/java/co/edu/unbosque/horarios/ # 7 suites de pruebas automatizadas y benchmarks
├── frontend-angular/                # Cliente SPA en Angular 18
│   ├── package.json                 # Dependencias y scripts de construcción
│   ├── angular.json                 # Configuración de Angular CLI
│   ├── proxy.conf.json              # Proxy de desarrollo hacia 127.0.0.1:8080
│   └── src/app/                     # AppComponent, MotorP2Component, WireframeP1Component, HorarioApiService
├── datos/
│   └── datos_prueba_20cursos.json   # Catálogo de entrada validado (20 asignaturas, 12 docentes, 12 aulas)
├── sql/
│   └── schema_3fn.sql               # Script DDL PostgreSQL 16 (9 tablas normalizadas en 3FN)
├── src/
│   └── motor.js                     # Código fuente del motor autónomo para navegador
├── dist/
│   └── motor.js                     # Bundle empaquetado para pruebas autónomas en navegador (RNF4)
├── pruebas/                         # Resultados JSON, scripts Selenium y log QA M3 (qa_m3_execution.log)
├── evidencia/                       # Diagramas de secuencia y capturas de validación
├── index.html                       # Consola interactiva de administración (servida en :8080/index.html)
├── prueba_navegador.html            # Arnés de pruebas de rendimiento en navegador (RNF1 / RNF4)
├── build_bundle.js                  # Script de empaquetado de dist/motor.js
├── ejecutar_todo.ps1                # Script maestro de compilación y pruebas (Windows PowerShell)
├── ejecutar_todo.sh                 # Script maestro de compilación y pruebas (Linux / macOS / Bash)
└── README.md                        # Documentación general, instalación y despliegue
```

---

## Cumplimiento de Requerimientos (RF y RNF)

* **RF1 (Generar propuesta de horario sin cruces):** `GeneradorVoraz.generarConRetroceso` y `POST /api/horario/cargar` ubican las 39/39 sesiones factibles en 30/30 semillas con 0 violaciones.
* **RF2 (Mover sesión manualmente con validación inmediata):** `HorarioService.intentarMover` y `POST /api/horario/mover` bloquean violaciones duras en tiempo real.
* **RF3 (Reasignar aula validando tipo y capacidad):** Selector de reasignación de aula en la barra de administración conectado a `POST /api/horario/mover` validando `RD5` y `RD2`.
* **RF4 (Advertir días consecutivos sin bloquear):** `DiasConsecutivos` emite advertencia amarilla `RB1` cuando su severidad es `aviso`.
* **RF5 (Reportar asignaturas sin ubicar con su causa):** Árbol de diagnóstico `GeneradorVoraz.causa` y panel lateral `#pend` (reporta `K09` por `RD5`).
* **RF6 (Modificar parámetros de planeación):** `Configuracion.java` y `POST /api/horario/reiniciar` actualizan cupo, severidad RB1 e inicio semanal en caliente.
* **RF7 (Verificar tope de sesiones según dedicación):** `TopeDedicacion` (`RD8`) y `Contexto.topeDocente` controlan los límites `TC=8`, `TQ=6`, `MT=4`, `UQ=2`.
* **RNF1 (Validar ajuste manual en menos de 200 ms):** Mediana de 0,051 ms en JVM y < 35 ms en navegador con DOM.
* **RNF2 (Cero desincronizaciones en mapa de ocupación):** Colecciones `private final` en `Horario` y verificación mediante `Auditor.auditar`.
* **RNF3 (Agregar regla sin tocar motor ni interfaz):** Patrón GoF Strategy verificado empíricamente con la regla `RD9` en `CambiosFTest`.
* **RNF4 (Ejecución en navegador estándar y exportación):** Consola web HTML5/Angular 18 y endpoint `GET /api/horario/exportar?formato=csv|json`.

---

*Proyecto desarrollado para la Entrega Final de Proyecto Núcleo 2 — Universidad El Bosque.*
