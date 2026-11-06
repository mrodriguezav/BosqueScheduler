# ==============================================================================
# Script de Reproducibilidad Total (PowerShell / Windows)
# Compila backend en backend-springboot/target/classes y bin/, ejecuta las 7
# suites de prueba (M1, M2 y M3), diagramas y recorridos en Chromium.
# ==============================================================================
$ErrorActionPreference = "Stop"

Write-Host "=== 1. Compilando Backend Java 21 ===" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "backend-springboot\target\classes", "backend-springboot\target\test-classes", "bin\main", "bin\test" | Out-Null
$mainFiles = (Get-ChildItem -Path backend-springboot\src\main\java -Filter *.java -Recurse).FullName
javac -encoding UTF-8 -d backend-springboot\target\classes $mainFiles
javac -encoding UTF-8 -d bin\main $mainFiles

$testFiles = (Get-ChildItem -Path backend-springboot\src\test\java -Filter *.java -Recurse).FullName
javac -encoding UTF-8 -cp backend-springboot\target\classes -d backend-springboot\target\test-classes $testFiles
javac -encoding UTF-8 -cp bin\main -d bin\test $testFiles

$CP = "backend-springboot/target/classes;backend-springboot/target/test-classes"

Write-Host "=== 2. [Suite 1/7] Ejecutando PruebaBasicaTest ===" -ForegroundColor Cyan
java -cp $CP co.edu.unbosque.horarios.PruebaBasicaTest

Write-Host "=== 3. [Suite 2/7] Ejecutando EjecutarP2Test (Benchmark Strategy vs Monolítico y k=1..32) ===" -ForegroundColor Cyan
java -cp $CP co.edu.unbosque.horarios.EjecutarP2Test

Write-Host "=== 4. [Suite 3/7] Ejecutando CambiosFTest (Prueba de cambio SOLID / RNF3) ===" -ForegroundColor Cyan
java -cp $CP co.edu.unbosque.horarios.CambiosFTest

Write-Host "=== 5. [Suite 4/7] Ejecutando CuelloBotellaTest (Prefiltro y escalabilidad) ===" -ForegroundColor Cyan
java -cp $CP co.edu.unbosque.horarios.CuelloBotellaTest

Write-Host "=== 6. [Suite 5/7] Ejecutando VerificarDiagramasTest (64 comprobaciones) ===" -ForegroundColor Cyan
java -cp $CP co.edu.unbosque.horarios.VerificarDiagramasTest

Write-Host "=== 7. [Suite 6/7] Ejecutando PruebasPersonasP1Test (Protocolo usabilidad P1) ===" -ForegroundColor Cyan
java -cp $CP co.edu.unbosque.horarios.PruebasPersonasP1Test

Write-Host "=== 8. [Suite 7/7] Ejecutando PruebasEntregaFinalM3Test (15 pruebas QA Módulo 3) ===" -ForegroundColor Cyan
java -cp $CP co.edu.unbosque.horarios.PruebasEntregaFinalM3Test

Write-Host "=== 9. Generando Diagrama de Secuencia UML ===" -ForegroundColor Cyan
python evidencia/diagrama_secuencia.py

Write-Host "=== 10. Ejecutando Recorrido P1 en Chromium ===" -ForegroundColor Cyan
python pruebas/p1_recorrido.py

Write-Host "=== 11. Ejecutando Prueba de Navegador Chromium (RNF1/RNF4) ===" -ForegroundColor Cyan
python pruebas/p3_navegador.py

Write-Host "=== Todas las 7 suites y pruebas completadas con exito. Resultados en pruebas/*.json, pruebas/qa_m3_execution.log y evidencia/ ===" -ForegroundColor Green
