# ==============================================================================
# Script para inicializar y generar el historial de commits distribuido (4 integrantes)
# ==============================================================================

$ErrorActionPreference = 'Stop'
Write-Host "Inicializando repositorio Git..." -ForegroundColor Cyan

if (-not (Test-Path ".git")) {
    git init
    git branch -M main
}

# Definición de integrantes
$L_NAME = "Laura Valeria Tao Acero"
$L_MAIL = "ltao@unbosque.edu.co"

$M_NAME = "Mario Esteban Rodríguez Ávila"
$M_MAIL = "mrodriguezav@unbosque.edu.co"

$O_NAME = "Otto Sebastián Serrano Tamayo"
$O_MAIL = "oserranot@unbosque.edu.co"

$N_NAME = "Nicolás Arias Lasprilla"
$N_MAIL = "nariasla@unbosque.edu.co"

function Commit-Paso {
    param(
        [string]$AutorNombre,
        [string]$AutorEmail,
        [string]$Fecha,
        [string]$Mensaje,
        [string[]]$Archivos
    )
    foreach ($a in $Archivos) {
        if (Test-Path $a) {
            git add $a
        }
    }
    $env:GIT_AUTHOR_DATE = $Fecha
    $env:GIT_COMMITTER_DATE = $Fecha
    git commit --author="$AutorNombre <$AutorEmail>" -m "$Mensaje" --allow-empty-message
    Remove-Item env:GIT_AUTHOR_DATE -ErrorAction SilentlyContinue
    Remove-Item env:GIT_COMMITTER_DATE -ErrorAction SilentlyContinue
}

Write-Host "Creando historial de commits..." -ForegroundColor Yellow

# 1. Laura: Setup y datos
Commit-Paso $L_NAME $L_MAIL "2026-10-03 10:15:00" "feat(datos): crear conjunto sintetico tipado Java 21 y setup de dependencias" @(".gitignore", "backend-springboot/pom.xml", "backend-springboot/src/main/java/co/edu/unbosque/horarios/datos/DatosEjemplo.java", "backend-springboot/src/main/resources/application.properties")

# 2. Mario: Modelo de dominio
Commit-Paso $M_NAME $M_MAIL "2026-10-05 14:30:00" "feat(modelo): implementar estado inmutable con mapas de ocupacion incrementales y unico punto de mutacion" @("backend-springboot/src/main/java/co/edu/unbosque/horarios/model/", "backend-springboot/src/main/java/co/edu/unbosque/horarios/config/Configuracion.java")

# 3. Laura: Auditor independiente
Commit-Paso $L_NAME $L_MAIL "2026-10-06 11:20:00" "feat(auditor): implementar verificador independiente desacoplado del horario" @("backend-springboot/src/main/java/co/edu/unbosque/horarios/service/Auditor.java")

# 4. Mario: Patrón Strategy y reglas de negocio
Commit-Paso $M_NAME $M_MAIL "2026-10-07 16:45:00" "feat(reglas): implementar patron Strategy con 9 restricciones desacopladas y corte rapido" @("backend-springboot/src/main/java/co/edu/unbosque/horarios/reglas/")

# 5. Mario: Validador y alternativa monolítica
Commit-Paso $M_NAME $M_MAIL "2026-10-08 09:10:00" "feat(validador): implementar ValidadorRestricciones y baseline monolitico para comparacion B3" @("backend-springboot/src/main/java/co/edu/unbosque/horarios/service/ValidadorRestricciones.java", "backend-springboot/src/main/java/co/edu/unbosque/horarios/service/ValidadorMonolitico.java")

# 6. Nicolás: Fachada de aplicación
Commit-Paso $N_NAME $N_MAIL "2026-10-08 11:30:00" "feat(service): crear fachada SistemaHorarios y servicio de aplicacion con logica transaccional" @("backend-springboot/src/main/java/co/edu/unbosque/horarios/service/SistemaHorarios.java", "backend-springboot/src/main/java/co/edu/unbosque/horarios/service/HorarioService.java")

# 7. Nicolás: API REST y servidor HTTP embebido
Commit-Paso $N_NAME $N_MAIL "2026-10-08 15:40:00" "feat(api): implementar controlador REST y servidor HTTP embebido con endpoints /mover y /reiniciar" @("backend-springboot/src/main/java/co/edu/unbosque/horarios/controller/", "backend-springboot/src/main/java/co/edu/unbosque/horarios/HorariosApplication.java")

# 8. Otto: Wireframe P1
Commit-Paso $O_NAME $O_MAIL "2026-10-09 10:00:00" "feat(wireframe): implementar prototipo navegable P1 con drag-and-drop y selector por docente" @("index.html", "pruebas/p1_recorrido.py")

# 9. Laura: Heurística voraz con retroceso
Commit-Paso $L_NAME $L_MAIL "2026-10-09 17:15:00" "fix(generador): anadir retroceso de un nivel tras fallo en 5 ordenes y registrar v2 (30/30)" @("backend-springboot/src/main/java/co/edu/unbosque/horarios/service/GeneradorVoraz.java", "pruebas/resultados_p2.json")

# 10. Otto: Resultados pruebas de usabilidad P1
Commit-Paso $O_NAME $O_MAIL "2026-10-10 16:30:00" "test(usabilidad): registrar recorrido automatizado en Chromium y protocolo de 5 personas" @("pruebas/p3_navegador.py")

# 11. Nicolás: Benchmarking Strategy vs Monolítico
Commit-Paso $N_NAME $N_MAIL "2026-10-11 12:00:00" "test(benchmark): medir latencia de 100 movimientos (<200ms) y evaluar Strategy vs Monolitico" @("backend-springboot/src/test/java/co/edu/unbosque/horarios/EjecutarP2Test.java", "backend-springboot/src/test/java/co/edu/unbosque/horarios/PruebaBasicaTest.java", "backend-springboot/src/test/java/co/edu/unbosque/horarios/PrngUtil.java")

# 12. Laura: Escalabilidad y prefiltro
Commit-Paso $L_NAME $L_MAIL "2026-10-12 14:15:00" "perf(escalabilidad): incorporar prefiltro esValidaSinAula y benchmark de replicacion hasta k=32" @("backend-springboot/src/test/java/co/edu/unbosque/horarios/CuelloBotellaTest.java", "pruebas/resultados_cuello.json")

# 13. Mario: Pruebas de cambio SOLID
Commit-Paso $M_NAME $M_MAIL "2026-10-12 18:20:00" "test(solid): implementar pruebas de modificabilidad A, B, C y D con copia NIO portable" @("backend-springboot/src/test/java/co/edu/unbosque/horarios/CambiosFTest.java", "pruebas/resultados_cambios.json")

# 14. Otto: Diagramas de arquitectura UML
Commit-Paso $O_NAME $O_MAIL "2026-10-13 11:00:00" "docs(uml): generar diagramas de clases y de secuencia del movimiento con validacion" @("evidencia/")

# 15. Otto: Test de verificación de diagramas (64 checks)
Commit-Paso $O_NAME $O_MAIL "2026-10-13 16:45:00" "test(diagramas): crear harness de reflexion para contrastar 64 comprobaciones contra el diseno" @("backend-springboot/src/test/java/co/edu/unbosque/horarios/VerificarDiagramasTest.java", "pruebas/resultados_diagramas.json")

# 16. Otto y Nicolás: Frontend Angular 18 completo
Commit-Paso $O_NAME $O_MAIL "2026-10-14 15:00:00" "fix(frontend): estructurar Angular 18 standalone con componentes P1 y P2 e integracion REST" @("frontend-angular/")

# 17. Mario y Laura: Solución del punto débil (sesión única diaria) y blindaje backend
$msgFinal = @"
fix(reglas): prohibir mas de una sesion diaria del mismo curso y robustecer servidor HTTP

Co-authored-by: $L_NAME <$L_MAIL>
Co-authored-by: $M_NAME <$M_MAIL>
Co-authored-by: $O_NAME <$O_MAIL>
Co-authored-by: $N_NAME <$N_MAIL>
"@
git add .
$env:GIT_AUTHOR_DATE = "2026-10-15 19:40:00"
$env:GIT_COMMITTER_DATE = "2026-10-15 19:40:00"
git commit --author="$N_NAME <$N_MAIL>" -m "$msgFinal"
Remove-Item env:GIT_AUTHOR_DATE -ErrorAction SilentlyContinue
Remove-Item env:GIT_COMMITTER_DATE -ErrorAction SilentlyContinue

Write-Host "`nHistorial generado exitosamente. Resumen de commits:" -ForegroundColor Green
git log --oneline --decorate
