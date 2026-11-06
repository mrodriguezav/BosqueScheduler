#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"
if [ ! -d "backend-springboot/src/main/java" ] && [ -d "../backend-springboot/src/main/java" ]; then
  cd ..
fi
echo "[INFO] Compilando BosqueScheduler Backend (Java 21)..."
mkdir -p backend-springboot/target/classes bin/main
javac -encoding UTF-8 -d backend-springboot/target/classes $(find backend-springboot/src/main/java -name "*.java")
cp -r backend-springboot/target/classes/* bin/main/
echo "[INFO] Iniciando BosqueScheduler Spring Boot REST Server..."
java -cp backend-springboot/target/classes co.edu.unbosque.horarios.HorariosApplication --server.port=8080

