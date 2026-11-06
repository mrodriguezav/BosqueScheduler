#!/usr/bin/env bash
# ==============================================================================
# Script de Reproducibilidad Total - Proyecto Núcleo 2 (Universidad El Bosque)
# Compila backend en backend-springboot/target/classes y bin/, ejecuta las 7
# suites de prueba y benchmarks (M1, M2 y M3), y ejecuta recorridos en Chromium.
# ==============================================================================
set -e

echo "=== 1. Compilando Backend Java 21 ==="
mkdir -p backend-springboot/target/classes backend-springboot/target/test-classes bin/main bin/test
javac -encoding UTF-8 -d backend-springboot/target/classes $(find backend-springboot/src/main/java -name "*.java")
javac -encoding UTF-8 -cp backend-springboot/target/classes -d backend-springboot/target/test-classes $(find backend-springboot/src/test/java -name "*.java")
cp -r backend-springboot/target/classes/* bin/main/
cp -r backend-springboot/target/test-classes/* bin/test/

CP="backend-springboot/target/classes:backend-springboot/target/test-classes"

echo "=== 2. [Suite 1/7] Ejecutando PruebaBasicaTest ==="
java -cp "$CP" co.edu.unbosque.horarios.PruebaBasicaTest

echo "=== 3. [Suite 2/7] Ejecutando EjecutarP2Test (Benchmark Strategy vs Monolítico y k=1..32) ==="
java -cp "$CP" co.edu.unbosque.horarios.EjecutarP2Test

echo "=== 4. [Suite 3/7] Ejecutando CambiosFTest (Prueba de cambio SOLID / RNF3) ==="
java -cp "$CP" co.edu.unbosque.horarios.CambiosFTest

echo "=== 5. [Suite 4/7] Ejecutando CuelloBotellaTest (Prefiltro y escalabilidad) ==="
java -cp "$CP" co.edu.unbosque.horarios.CuelloBotellaTest

echo "=== 6. [Suite 5/7] Ejecutando VerificarDiagramasTest (64 comprobaciones) ==="
java -cp "$CP" co.edu.unbosque.horarios.VerificarDiagramasTest

echo "=== 7. [Suite 6/7] Ejecutando PruebasPersonasP1Test (Protocolo usabilidad P1) ==="
java -cp "$CP" co.edu.unbosque.horarios.PruebasPersonasP1Test

echo "=== 8. [Suite 7/7] Ejecutando PruebasEntregaFinalM3Test (15 pruebas QA Módulo 3) ==="
java -cp "$CP" co.edu.unbosque.horarios.PruebasEntregaFinalM3Test

echo "=== 9. Generando Diagrama de Secuencia UML ==="
python3 evidencia/diagrama_secuencia.py 2>/dev/null || python evidencia/diagrama_secuencia.py

echo "=== 10. Ejecutando Recorrido P1 en Chromium ==="
python3 pruebas/p1_recorrido.py 2>/dev/null || python pruebas/p1_recorrido.py

echo "=== 11. Ejecutando Prueba de Navegador Chromium (RNF1/RNF4) ==="
python3 pruebas/p3_navegador.py 2>/dev/null || python pruebas/p3_navegador.py

echo "=== Todas las 7 suites y pruebas completadas con éxito. Resultados en pruebas/*.json, pruebas/qa_m3_execution.log y evidencia/ ==="
