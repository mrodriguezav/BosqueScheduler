/**
 * Motor empaquetado para ejecucion autonoma en navegador (RNF4).
 * Implementa el modelo de horarios, las 9 reglas de validacion y el generador
 * para ejecucion de pruebas de rendimiento dentro de Chromium sin backend externo.
 */

var MotorHorarios = (function () {
    var DIAS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
    var BLOQUES = [7, 9, 13, 15, 17, 19];

    var DOCENTES = {
        D1: { nombre: 'Docente 1', dedicacion: 10 },
        D2: { nombre: 'Docente 2', dedicacion: 8 },
        D3: { nombre: 'Docente 3', dedicacion: 12 },
        D4: { nombre: 'Docente 4', dedicacion: 10, desde: 16 },
        D5: { nombre: 'Docente 5', dedicacion: 12 }
    };

    var AULAS = {
        C1: { tipo: 'computo', capacidad: 34 },
        C2: { tipo: 'computo', capacidad: 34 },
        E1: { tipo: 'estandar', capacidad: 40 },
        E2: { tipo: 'estandar', capacidad: 40 },
        E3: { tipo: 'estandar', capacidad: 45 },
        E4: { tipo: 'estandar', capacidad: 50 },
        M1: { tipo: 'movil', capacidad: 44 }
    };

    var CURSOS = {
        K01: { nombre: 'Programación I', frecuencia: 4, tipo: 'computo', cupo: 30, habilitados: ['D1', 'D3'] },
        K02: { nombre: 'Cálculo Diferencial', frecuencia: 3, tipo: 'estandar', cupo: 40, habilitados: ['D5'] },
        K03: { nombre: 'Física Mecánica', frecuencia: 3, tipo: 'estandar', cupo: 35, habilitados: ['D4'] },
        K05: { nombre: 'Ing. de Software I', frecuencia: 3, tipo: 'movil', cupo: 38, habilitados: ['D5'] },
        K08: { nombre: 'Redes de Datos I', frecuencia: 2, tipo: 'computo', cupo: 28, habilitados: ['D3'] },
        K09: { nombre: 'Bases de Datos', frecuencia: 2, tipo: 'computo', cupo: 44, habilitados: ['D3'] }
    };

    function validarSesion(s, dia, hora, horarioActual) {
        var duras = [];
        var avisos = [];
        var curso = CURSOS[s.cursoId] || { nombre: s.cursoId, frecuencia: 2 };
        var doc = DOCENTES[s.docenteId] || {};

        // RD4: Sabado despues de 13:00
        if (dia === 5 && hora >= 13) {
            duras.push({ regla: 'RD4', motivo: 'Sábado fuera de franja de 7:00 a 13:00' });
        }

        // RD5: Capacidad de aula
        var aula = AULAS[s.aulaId];
        if (aula) {
            if (curso.tipo && aula.tipo !== curso.tipo) {
                duras.push({ regla: 'RD5', motivo: 'Tipo de aula incompatible' });
            }
            if (curso.cupo && aula.capacidad < curso.cupo) {
                duras.push({ regla: 'RD5', motivo: 'Capacidad insuficiente en aula' });
            }
        }

        // RD7: Franja de docente
        if (doc.desde && hora < doc.desde) {
            duras.push({ regla: 'RD7', motivo: s.docenteId + ' solo dicta después de ' + doc.desde + ':00' });
        }

        // Cruces con otras sesiones
        for (var i = 0; i < horarioActual.length; i++) {
            var o = horarioActual[i];
            if (o.id === s.id) continue;
            if (o.dia === dia && o.hora === hora) {
                if (o.docenteId === s.docenteId) {
                    duras.push({ regla: 'RD1', motivo: 'Cruce con docente ' + s.docenteId });
                }
                if (o.aulaId === s.aulaId) {
                    duras.push({ regla: 'RD2', motivo: 'Cruce con aula ' + s.aulaId });
                }
            }
        }

        // RB1: Mismo día y días consecutivos
        var sesionesMismoCurso = horarioActual.filter(function (x) {
            return x.cursoId === s.cursoId && x.id !== s.id;
        });

        var colisionMismoDia = false;
        var avisoConsecutivo = false;

        for (var j = 0; j < sesionesMismoCurso.length; j++) {
            var sc = sesionesMismoCurso[j];
            if (sc.dia === dia) {
                colisionMismoDia = true;
                break;
            } else if (curso.frecuencia <= 3 && Math.abs(sc.dia - dia) === 1) {
                avisoConsecutivo = true;
            }
        }

        if (colisionMismoDia) {
            duras.push({ regla: 'RB1', motivo: 'Más de una sesión del curso ' + curso.nombre + ' el mismo día' });
        } else if (avisoConsecutivo) {
            avisos.push({ regla: 'RB1', motivo: curso.nombre + ' en días consecutivos' });
        }

        return { duras: duras, avisos: avisos, valida: duras.length === 0 };
    }

    return {
        DOCENTES: DOCENTES,
        AULAS: AULAS,
        CURSOS: CURSOS,
        DIAS: DIAS,
        BLOQUES: BLOQUES,
        validarSesion: validarSesion
    };
})();

if (typeof module !== 'undefined' && module.exports) {
    module.exports = MotorHorarios;
}
