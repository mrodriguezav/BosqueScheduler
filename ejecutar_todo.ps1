# ==============================================================================
# Script de Reproducibilidad Total (PowerShell / Windows)
# ==============================================================================
$ErrorActionPreference = "Stop"

Write-Host "=== 1. Compilando Backend Java 21 ===" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "bin\main", "bin\test" | Out-Null
$mainFiles = (Get-ChildItem -Path backend-springboot\src\main\java -Filter *.java -Recurse).FullName
javac -encoding UTF-8 -d bin\main $mainFiles

$testFiles = (Get-ChildItem -Path backend-springboot\src\test\java -Filter *.java -Recurse).FullName
javac -encoding UTF-8 -cp bin\main -d bin\test $testFiles

Write-Host "=== 2. Ejecutando VerificarDiagramasTest (64 comprobaciones) ===" -ForegroundColor Cyan
java -cp "bin/main;bin/test" co.edu.unbosque.horarios.VerificarDiagramasTest

Write-Host "=== 3. Ejecutando PruebaBasicaTest ===" -ForegroundColor Cyan
java -cp "bin/main;bin/test" co.edu.unbosque.horarios.PruebaBasicaTest

Write-Host "=== 4. Ejecutando CambiosFTest (Prueba de cambio SOLID / RNF3) ===" -ForegroundColor Cyan
java -cp "bin/main;bin/test" co.edu.unbosque.horarios.CambiosFTest

Write-Host "=== 5. Ejecutando CuelloBotellaTest (Prefiltro y escalabilidad) ===" -ForegroundColor Cyan
java -cp "bin/main;bin/test" co.edu.unbosque.horarios.CuelloBotellaTest

Write-Host "=== 6. Ejecutando EjecutarP2Test (Benchmark Strategy vs Monolítico) ===" -ForegroundColor Cyan
java -cp "bin/main;bin/test" co.edu.unbosque.horarios.EjecutarP2Test

Write-Host "=== 7. Generando Diagrama de Secuencia UML ===" -ForegroundColor Cyan
python evidencia/diagrama_secuencia.py

Write-Host "=== 8. Ejecutando Recorrido P1 en Chromium ===" -ForegroundColor Cyan
python pruebas/p1_recorrido.py

Write-Host "=== 9. Ejecutando Prueba de Navegador Chromium (RNF1/RNF4) ===" -ForegroundColor Cyan
python pruebas/p3_navegador.py

Write-Host "=== Todas las pruebas completadas con exito. Resultados en pruebas/*.json y evidencia/ ===" -ForegroundColor Green
