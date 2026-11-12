-- =====================================================================
-- CONECTA - Sistema de Gestion de Eventos Comunitarios
-- Base de datos: bd_conecta_demo
-- Motor: TiDB Cloud compatible con MySQL
-- Ejecutar una sola vez en una base nueva. No incluye usuarios ni datos semilla.
-- Curso: Desarrollo de Aplicaciones Moviles 2 (iOS) - Cibertec
-- =====================================================================

CREATE DATABASE bd_conecta_demo
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE bd_conecta_demo;

-- =====================================================================
-- 1. USUARIOS Y ROLES
-- =====================================================================

CREATE TABLE usuario (
    id_usuario      INT             NOT NULL AUTO_INCREMENT,
    username        VARCHAR(100)    NOT NULL,
    password        VARCHAR(255)    NOT NULL,
    rol             VARCHAR(30)     NOT NULL DEFAULT 'ROLE_PARTICIPANTE',
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    firebase_uid    VARCHAR(128)    NULL,
    fecha_creacion  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uq_usuario_username (username),
    UNIQUE KEY uq_usuario_firebase_uid (firebase_uid),
    KEY idx_usuario_rol (rol),
    CONSTRAINT chk_usuario_rol
        CHECK (rol IN ('ROLE_ADMIN', 'ROLE_PARTICIPANTE'))
) ENGINE=InnoDB;

CREATE TABLE administrador (
    id_usuario  INT             NOT NULL,
    nombres     VARCHAR(100)    NOT NULL,
    apellidos   VARCHAR(100)    NOT NULL,
    email       VARCHAR(120)    NOT NULL,
    telefono    VARCHAR(20)     NULL,
    cargo       VARCHAR(60)     NULL,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uq_administrador_email (email),
    CONSTRAINT fk_administrador_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE participante (
    id_usuario          INT             NOT NULL,
    nombres             VARCHAR(100)    NOT NULL,
    apellidos           VARCHAR(100)    NOT NULL,
    email               VARCHAR(120)    NOT NULL,
    telefono            VARCHAR(20)     NULL,
    foto_url            VARCHAR(255)    NULL,
    fcm_token           VARCHAR(255)    NULL,
    fecha_nacimiento    DATE            NULL,
    fecha_actualizacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uq_participante_email (email),
    CONSTRAINT fk_participante_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 2. CATEGORIAS
-- =====================================================================

CREATE TABLE categoria (
    id_categoria    INT             NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(60)     NOT NULL,
    descripcion     VARCHAR(200)    NULL,
    color           VARCHAR(20)     NULL,
    icono           VARCHAR(60)     NULL,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_categoria),
    UNIQUE KEY uq_categoria_nombre (nombre),
    KEY idx_categoria_activo (activo)
) ENGINE=InnoDB;

-- =====================================================================
-- 3. EVENTOS
-- =====================================================================

CREATE TABLE evento (
    id_evento           INT             NOT NULL AUTO_INCREMENT,
    id_categoria        INT             NOT NULL,
    titulo              VARCHAR(120)    NOT NULL,
    descripcion         TEXT            NOT NULL,
    imagen_url          VARCHAR(255)    NULL,
    fecha               DATE            NOT NULL,
    hora_inicio         TIME            NOT NULL,
    hora_fin            TIME            NOT NULL,
    ubicacion           VARCHAR(150)    NOT NULL,
    direccion           VARCHAR(200)    NULL,
    latitud             DECIMAL(10,7)   NULL,
    longitud            DECIMAL(10,7)   NULL,
    aforo               INT             NOT NULL,
    inscritos           INT             NOT NULL DEFAULT 0,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'BORRADOR',
    destacado           BOOLEAN         NOT NULL DEFAULT FALSE,
    version             BIGINT          NOT NULL DEFAULT 0,
    fecha_creacion      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_evento),
    KEY idx_evento_estado (estado),
    KEY idx_evento_categoria (id_categoria),
    KEY idx_evento_fecha (fecha),
    KEY idx_evento_destacado (destacado),
    CONSTRAINT fk_evento_categoria
        FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria)
        ON DELETE RESTRICT,
    CONSTRAINT chk_evento_aforo CHECK (aforo > 0),
    CONSTRAINT chk_evento_inscritos CHECK (inscritos >= 0),
    CONSTRAINT chk_evento_cupo CHECK (inscritos <= aforo),
    CONSTRAINT chk_evento_horario CHECK (hora_fin > hora_inicio),
    CONSTRAINT chk_evento_estado
        CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'LLENO', 'REALIZADO', 'CANCELADO', 'INACTIVO'))
) ENGINE=InnoDB;

-- =====================================================================
-- 4. INSCRIPCIONES
-- =====================================================================

CREATE TABLE inscripcion (
    id_inscripcion      INT             NOT NULL AUTO_INCREMENT,
    id_participante     INT             NOT NULL,
    id_evento           INT             NOT NULL,
    codigo              VARCHAR(30)     NOT NULL,
    qr_token            VARCHAR(64)     NOT NULL,
    fecha_inscripcion   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'CONFIRMADA',
    fecha_asistencia    DATETIME        NULL,
    observacion         VARCHAR(255)    NULL,
    PRIMARY KEY (id_inscripcion),
    UNIQUE KEY uq_inscripcion_codigo (codigo),
    UNIQUE KEY uq_inscripcion_qr_token (qr_token),
    UNIQUE KEY uq_inscripcion_participante_evento (id_participante, id_evento),
    KEY idx_inscripcion_evento (id_evento),
    KEY idx_inscripcion_participante (id_participante),
    KEY idx_inscripcion_estado (estado),
    CONSTRAINT fk_inscripcion_participante
        FOREIGN KEY (id_participante) REFERENCES participante (id_usuario)
        ON DELETE CASCADE,
    CONSTRAINT fk_inscripcion_evento
        FOREIGN KEY (id_evento) REFERENCES evento (id_evento)
        ON DELETE RESTRICT,
    CONSTRAINT chk_inscripcion_estado
        CHECK (estado IN ('CONFIRMADA', 'ASISTIO', 'NO_ASISTIO', 'CANCELADA'))
) ENGINE=InnoDB;

-- =====================================================================
-- 5. ASISTENCIAS
-- =====================================================================

CREATE TABLE asistencia (
    id_asistencia       INT             NOT NULL AUTO_INCREMENT,
    id_inscripcion      INT             NOT NULL,
    fecha_hora          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metodo              VARCHAR(10)     NOT NULL,
    id_admin_validador  INT             NULL,
    observacion         VARCHAR(255)    NULL,
    PRIMARY KEY (id_asistencia),
    UNIQUE KEY uq_asistencia_inscripcion (id_inscripcion),
    KEY idx_asistencia_admin (id_admin_validador),
    CONSTRAINT fk_asistencia_inscripcion
        FOREIGN KEY (id_inscripcion) REFERENCES inscripcion (id_inscripcion)
        ON DELETE CASCADE,
    CONSTRAINT fk_asistencia_admin
        FOREIGN KEY (id_admin_validador) REFERENCES administrador (id_usuario)
        ON DELETE SET NULL,
    CONSTRAINT chk_asistencia_metodo
        CHECK (metodo IN ('QR', 'MANUAL'))
) ENGINE=InnoDB;

-- =====================================================================
-- 6. NOTIFICACIONES (OPCIONAL - FCM)
-- =====================================================================

CREATE TABLE notificacion (
    id_notificacion INT             NOT NULL AUTO_INCREMENT,
    titulo          VARCHAR(120)    NOT NULL,
    mensaje         VARCHAR(500)    NOT NULL,
    tipo            VARCHAR(20)     NOT NULL DEFAULT 'SISTEMA',
    id_evento       INT             NULL,
    id_admin        INT             NULL,
    fecha_creacion  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviada         BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_notificacion),
    KEY idx_notificacion_evento (id_evento),
    KEY idx_notificacion_enviada (enviada),
    CONSTRAINT fk_notificacion_evento
        FOREIGN KEY (id_evento) REFERENCES evento (id_evento)
        ON DELETE SET NULL,
    CONSTRAINT fk_notificacion_admin
        FOREIGN KEY (id_admin) REFERENCES administrador (id_usuario)
        ON DELETE SET NULL,
    CONSTRAINT chk_notificacion_tipo
        CHECK (tipo IN ('EVENTO', 'RECORDATORIO', 'SISTEMA'))
) ENGINE=InnoDB;


---------------------------------------------------------------------------
