-- ============================================================================
-- BosqueScheduler - Esquema Relacional Normalizado en Tercera Forma Normal (3FN)
-- Motor destino: PostgreSQL 16+
-- Artefacto A.4 (Anexos Técnicos) y Sección 2 (Manual Técnico)
-- ============================================================================

DROP TABLE IF EXISTS SESION_HORARIO CASCADE;
DROP TABLE IF EXISTS CURSO_DOCENTE_HABILITADO CASCADE;
DROP TABLE IF EXISTS DISPONIBILIDAD_DOCENTE CASCADE;
DROP TABLE IF EXISTS BLOQUE CASCADE;
DROP TABLE IF EXISTS AULA CASCADE;
DROP TABLE IF EXISTS CURSO CASCADE;
DROP TABLE IF EXISTS DOCENTE CASCADE;
DROP TABLE IF EXISTS TIPO_DEDICACION CASCADE;
DROP TABLE IF EXISTS PARAMETRO_PLANEACION CASCADE;

-- 1. Parámetros normativos de planeación (RF6)
CREATE TABLE PARAMETRO_PLANEACION (
    clave VARCHAR(40) PRIMARY KEY NOT NULL,
    valor VARCHAR(40) NOT NULL
);

-- 2. Catálogo en 3FN de vinculaciones docentes (RF7, RD8)
CREATE TABLE TIPO_DEDICACION (
    codigo_dedicacion CHAR(2) PRIMARY KEY NOT NULL,
    descripcion VARCHAR(40) NOT NULL,
    tope_sesiones_sem SMALLINT NOT NULL CHECK (tope_sesiones_sem > 0)
);

-- 3. Docentes (D1 a D12)
CREATE TABLE DOCENTE (
    docente_id VARCHAR(10) PRIMARY KEY NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    codigo_dedicacion CHAR(2) NOT NULL,
    CONSTRAINT fk_docente_dedicacion
        FOREIGN KEY (codigo_dedicacion) REFERENCES TIPO_DEDICACION(codigo_dedicacion)
);

-- 4. Ventanas horarias de disponibilidad permitidas por docente (RD7)
CREATE TABLE DISPONIBILIDAD_DOCENTE (
    disponibilidad_id SERIAL PRIMARY KEY,
    docente_id VARCHAR(10) NOT NULL,
    dia SMALLINT NOT NULL CHECK (dia BETWEEN 0 AND 5),
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CONSTRAINT fk_disp_docente
        FOREIGN KEY (docente_id) REFERENCES DOCENTE(docente_id) ON DELETE CASCADE,
    CONSTRAINT chk_disp_rango
        CHECK (hora_fin > hora_inicio)
);

-- 5. Catálogo de asignaturas a programar (K01 a K20)
CREATE TABLE CURSO (
    curso_id VARCHAR(10) PRIMARY KEY NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    tipo_aula_req VARCHAR(10) NOT NULL CHECK (tipo_aula_req IN ('estandar', 'computo', 'movil')),
    inscritos INT NOT NULL CHECK (inscritos >= 0),
    frecuencia_sem SMALLINT NOT NULL CHECK (frecuencia_sem BETWEEN 1 AND 4)
);

-- 6. Tabla asociativa en 1FN/2FN de habilitación docente por asignatura (RD3)
CREATE TABLE CURSO_DOCENTE_HABILITADO (
    curso_id VARCHAR(10) NOT NULL,
    docente_id VARCHAR(10) NOT NULL,
    PRIMARY KEY (curso_id, docente_id),
    CONSTRAINT fk_hab_curso
        FOREIGN KEY (curso_id) REFERENCES CURSO(curso_id) ON DELETE CASCADE,
    CONSTRAINT fk_hab_docente
        FOREIGN KEY (docente_id) REFERENCES DOCENTE(docente_id) ON DELETE CASCADE
);

-- 7. Catálogo de espacios físicos / aulas (RD5)
CREATE TABLE AULA (
    aula_id VARCHAR(10) PRIMARY KEY NOT NULL,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('estandar', 'computo', 'movil')),
    capacidad INT NOT NULL CHECK (capacidad > 0)
);

-- 8. Franjas semanales de 2 horas (RD4, RD6)
CREATE TABLE BLOQUE (
    bloque_id VARCHAR(8) PRIMARY KEY NOT NULL,
    dia SMALLINT NOT NULL CHECK (dia BETWEEN 0 AND 5),
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    orden INT UNIQUE NOT NULL,
    CONSTRAINT chk_bloque_rango
        CHECK (hora_fin > hora_inicio)
);

-- 9. Sesiones asignadas en el horario con invariantes declarativas (RD1, RD2, RD3, RD4, RD5, RD6)
CREATE TABLE SESION_HORARIO (
    sesion_id VARCHAR(16) PRIMARY KEY NOT NULL,
    curso_id VARCHAR(10) NOT NULL,
    docente_id VARCHAR(10) NOT NULL,
    aula_id VARCHAR(10) NOT NULL,
    bloque_id VARCHAR(8) NOT NULL,
    CONSTRAINT uk1_docente_bloque UNIQUE (docente_id, bloque_id),
    CONSTRAINT uk2_aula_bloque UNIQUE (aula_id, bloque_id),
    CONSTRAINT fk_sesion_curso_docente_hab
        FOREIGN KEY (curso_id, docente_id) REFERENCES CURSO_DOCENTE_HABILITADO(curso_id, docente_id),
    CONSTRAINT fk_sesion_curso
        FOREIGN KEY (curso_id) REFERENCES CURSO(curso_id),
    CONSTRAINT fk_sesion_aula
        FOREIGN KEY (aula_id) REFERENCES AULA(aula_id),
    CONSTRAINT fk_sesion_bloque
        FOREIGN KEY (bloque_id) REFERENCES BLOQUE(bloque_id)
);

-- ============================================================================
-- Carga semilla del caso de estudio base (20 cursos, 12 docentes, 12 aulas)
-- ============================================================================

INSERT INTO PARAMETRO_PLANEACION (clave, valor) VALUES
    ('cupoMaximo', '40'),
    ('tolerancia', '0.10'),
    ('duracionSesion', '2'),
    ('franja.semana.inicio', '7'),
    ('franja.semana.fin', '22'),
    ('franja.sabado.inicio', '7'),
    ('franja.sabado.fin', '13'),
    ('almuerzo.inicio', '12'),
    ('almuerzo.fin', '13'),
    ('severidad.RB1', 'aviso');

INSERT INTO TIPO_DEDICACION (codigo_dedicacion, descripcion, tope_sesiones_sem) VALUES
    ('TC', 'Tiempo Completo', 8),
    ('TQ', 'Tres Cuartos', 6),
    ('MT', 'Medio Tiempo', 4),
    ('UQ', 'Cátedra (Un Cuarto)', 2);

INSERT INTO DOCENTE (docente_id, nombre, codigo_dedicacion) VALUES
    ('D1', 'Docente D1', 'TC'),
    ('D2', 'Docente D2', 'TC'),
    ('D3', 'Docente D3', 'TC'),
    ('D4', 'Docente D4', 'TC'),
    ('D5', 'Docente D5', 'TQ'),
    ('D6', 'Docente D6', 'TQ'),
    ('D7', 'Docente D7', 'TQ'),
    ('D8', 'Docente D8', 'MT'),
    ('D9', 'Docente D9', 'MT'),
    ('D10', 'Docente D10', 'MT'),
    ('D11', 'Docente D11', 'UQ'),
    ('D12', 'Docente D12', 'UQ');

INSERT INTO AULA (aula_id, tipo, capacidad) VALUES
    ('E1', 'estandar', 45),
    ('E2', 'estandar', 45),
    ('E3', 'estandar', 45),
    ('E4', 'estandar', 50),
    ('E5', 'estandar', 50),
    ('C1', 'computo', 34),
    ('C2', 'computo', 34),
    ('C3', 'computo', 32),
    ('C4', 'computo', 30),
    ('M1', 'movil', 44),
    ('M2', 'movil', 40),
    ('M3', 'movil', 40);

INSERT INTO CURSO (curso_id, nombre, tipo_aula_req, inscritos, frecuencia_sem) VALUES
    ('K01', 'Programación I', 'computo', 30, 4),
    ('K02', 'Cálculo Diferencial', 'estandar', 42, 3),
    ('K03', 'Física Mecánica', 'estandar', 44, 3),
    ('K04', 'Estructuras de Datos', 'computo', 32, 3),
    ('K05', 'Ingeniería de Software I', 'movil', 40, 3),
    ('K06', 'Álgebra Lineal', 'estandar', 40, 2),
    ('K07', 'Matemáticas Discretas', 'estandar', 38, 2),
    ('K08', 'Redes de Datos I', 'computo', 34, 2),
    ('K09', 'Bases de Datos', 'computo', 44, 2),
    ('K10', 'Sistemas Operativos', 'estandar', 40, 2),
    ('K11', 'Probabilidad y Estadística', 'estandar', 44, 2),
    ('K12', 'Dinámica de Sistemas', 'movil', 38, 2),
    ('K13', 'Modelos y Simulación', 'movil', 36, 2),
    ('K14', 'Complejidad Algorítmica', 'estandar', 38, 2),
    ('K15', 'Arquitectura de Computadores', 'computo', 30, 2),
    ('K16', 'Ética Profesional', 'estandar', 44, 1),
    ('K17', 'Inglés Técnico', 'estandar', 35, 1),
    ('K18', 'Investigación Tecnológica', 'movil', 40, 1),
    ('K19', 'Electiva Humanística', 'estandar', 40, 1),
    ('K20', 'Seminario de Grado', 'estandar', 25, 1);

INSERT INTO CURSO_DOCENTE_HABILITADO (curso_id, docente_id) VALUES
    ('K01','D1'),('K01','D3'),('K01','D7'),
    ('K02','D1'),('K02','D5'),('K02','D6'),
    ('K03','D4'),
    ('K04','D1'),('K04','D3'),('K04','D7'),
    ('K05','D3'),('K05','D5'),('K05','D8'),
    ('K06','D2'),('K06','D6'),('K06','D9'),
    ('K07','D2'),('K07','D5'),('K07','D9'),
    ('K08','D3'),('K08','D7'),('K08','D10'),
    ('K09','D3'),('K09','D7'),('K09','D8'),
    ('K10','D1'),('K10','D7'),('K10','D10'),
    ('K11','D2'),('K11','D6'),('K11','D12'),
    ('K12','D5'),('K12','D8'),('K12','D11'),
    ('K13','D5'),('K13','D8'),('K13','D12'),
    ('K14','D1'),('K14','D3'),('K14','D10'),
    ('K15','D7'),('K15','D10'),('K15','D12'),
    ('K16','D9'),('K16','D11'),('K16','D12'),
    ('K17','D9'),('K17','D11'),('K17','D12'),
    ('K18','D5'),('K18','D8'),('K18','D11'),
    ('K19','D6'),('K19','D11'),('K19','D12'),
    ('K20','D1'),('K20','D8'),('K20','D10');

