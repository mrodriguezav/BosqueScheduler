#!/usr/bin/env bash
# ==============================================================================
# Script de Reproducibilidad Total - Proyecto Núcleo 2 (Universidad El Bosque)
# Compila backend, ejecuta todas las suites de prueba y benchmarks P2, y
# ejecuta el recorrido automatizado de P1 en Chromium.
# ==============================================================================
set -e

echo "=== 1. Compilando Backend Java 21 ==="
mkdir -p bin/main bin/test
javac -encoding UTF-8 -d bin/main $(find backend-springboot/src/main/java -name "*.java")
javac -encoding UTF-8 -cp bin/main -d bin/test $(find backend-springboot/src/test/java -name "*.java")

echo "=== 2. Ejecutando VerificarDiagramasTest (64 comprobaciones) ==="
java -cp "bin/main:bin/test" co.edu.unbosque.horarios.VerificarDiagramasTest

echo "=== 3. Ejecutando PruebaBasicaTest ==="
java -cp "bin/main:bin/test" co.edu.unbosque.horarios.PruebaBasicaTest

echo "=== 4. Ejecutando CambiosFTest (Prueba de cambio SOLID / RNF3) ==="
java -cp "bin/main:bin/test" co.edu.unbosque.horarios.CambiosFTest

echo "=== 5. Ejecutando CuelloBotellaTest (Prefiltro y escalabilidad) ==="
java -cp "bin/main:bin/test" co.edu.unbosque.horarios.CuelloBotellaTest

echo "=== 6. Ejecutando EjecutarP2Test (Benchmark Strategy vs Monolitico) ==="
java -cp "bin/main:bin/test" co.edu.unbosque.horarios.EjecutarP2Test

echo "=== 7. Generando Diagrama de Secuencia UML ==="
python3 evidencia/diagrama_secuencia.py 2>/dev/null || python evidencia/diagrama_secuencia.py

echo "=== 8. Ejecutando Recorrido P1 en Chromium ==="
python3 pruebas/p1_recorrido.py 2>/dev/null || python pruebas/p1_recorrido.py

echo "=== Todas las pruebas completadas con éxito. Resultados en pruebas/*.json y evidencia/ ==="
