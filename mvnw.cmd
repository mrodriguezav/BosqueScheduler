@echo off
setlocal
cd /d "%~dp0"
if exist "backend-springboot\src\main\java" (
    set "ROOT_DIR=%CD%"
) else (
    cd ..
    set "ROOT_DIR=%CD%"
)
echo [INFO] Compilando BosqueScheduler Backend (Java 21)...
powershell -NoProfile -Command "New-Item -ItemType Directory -Force -Path 'backend-springboot\target\classes', 'bin\main' | Out-Null; $f = (Get-ChildItem -Path backend-springboot\src\main\java -Filter *.java -Recurse).FullName; javac -encoding UTF-8 -d backend-springboot\target\classes $f; javac -encoding UTF-8 -d bin\main $f"
echo [INFO] Iniciando BosqueScheduler Spring Boot REST Server...
java -cp backend-springboot/target/classes co.edu.unbosque.horarios.HorariosApplication --server.port=8080

