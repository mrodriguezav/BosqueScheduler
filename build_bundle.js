/**
 * Script de empaquetado del motor autónomo en navegador (RNF4).
 * Copia y valida src/motor.js hacia dist/motor.js.
 */
const fs = require('fs');
const path = require('path');

const srcPath = path.join(__dirname, 'src', 'motor.js');
const distDir = path.join(__dirname, 'dist');
const distPath = path.join(distDir, 'motor.js');

if (!fs.existsSync(distDir)) {
  fs.mkdirSync(distDir, { recursive: true });
}

const contenido = fs.readFileSync(srcPath, 'utf8');
fs.writeFileSync(distPath, contenido, 'utf8');
console.log('Bundle autónomo generado en dist/motor.js (' + contenido.length + ' bytes)');

