-- =====================================================================
-- Esquema del motor de decision.
--
-- El enunciado pide "script de inicializacion o migraciones": este es el
-- script, versionado en git y ejecutado por el contenedor de PostgreSQL al
-- crearse. No se usa `ddl-auto: update` (docs/architecture.md §5).
--
-- Convencion: la base de datos habla snake_case; el contrato JSON habla
-- camelCase. El puente son las anotaciones @Column de las entidades.
-- =====================================================================

-- ---------------------------------------------------------------------
-- solicitud: una solicitud de credito radicada y ya evaluada.
--
-- Lleva dos identificadores a proposito:
--   id           clave tecnica, la que referencian las tablas hijas.
--   id_solicitud identificador de negocio SOL-yyyyMMdd-NNN, el que viaja al
--                cliente. Se separa para no exponer un autoincremental ni
--                atar las llaves foraneas a un formato de presentacion.
-- ---------------------------------------------------------------------
CREATE TABLE solicitud (
    id                  BIGSERIAL     PRIMARY KEY,
    id_solicitud        VARCHAR(20)   NOT NULL UNIQUE,
    tipo_documento      VARCHAR(2)    NOT NULL,
    numero_documento    VARCHAR(20)   NOT NULL,
    nombres             VARCHAR(100)  NOT NULL,
    apellidos           VARCHAR(100)  NOT NULL,
    correo_electronico  VARCHAR(255)  NOT NULL,
    telefono_celular    VARCHAR(20)   NOT NULL,
    monto_solicitado    NUMERIC(15,2) NOT NULL,
    plazo_meses         SMALLINT      NOT NULL,
    ingresos_mensuales  NUMERIC(15,2) NOT NULL,
    estado              VARCHAR(20)   NOT NULL,
    -- Nulos cuando el buro no respondio (PENDIENTE_REVISION) o cuando la
    -- solicitud se corto antes de consultarlo (RECHAZADO_FRAUDE).
    score_buro          INTEGER,
    tasa_estimada       NUMERIC(5,2),
    fecha_creacion      TIMESTAMP     NOT NULL,

    CONSTRAINT ck_solicitud_tipo_documento
        CHECK (tipo_documento IN ('CC', 'CE', 'PA')),
    CONSTRAINT ck_solicitud_monto
        CHECK (monto_solicitado BETWEEN 1000000 AND 50000000),
    CONSTRAINT ck_solicitud_plazo
        CHECK (plazo_meses IN (12, 24, 36, 48)),
    CONSTRAINT ck_solicitud_ingresos
        CHECK (ingresos_mensuales > 0),
    CONSTRAINT ck_solicitud_estado
        CHECK (estado IN ('APROBADO', 'PREAPROBADO', 'RECHAZADO',
                          'RECHAZADO_FRAUDE', 'PENDIENTE_REVISION'))
);

-- La consulta de estado (feature 14) siempre filtra por este par.
CREATE INDEX idx_solicitud_documento
    ON solicitud (tipo_documento, numero_documento);

-- ---------------------------------------------------------------------
-- resultado_validacion: el rastro de la cadena de validaciones.
--
-- `orden` conserva la secuencia en que se ejecutaron, que es informacion de
-- negocio: la cadena se detiene en la primera que falla, asi que el numero de
-- filas dice hasta donde llego la evaluacion.
-- ---------------------------------------------------------------------
CREATE TABLE resultado_validacion (
    id            BIGSERIAL    PRIMARY KEY,
    solicitud_id  BIGINT       NOT NULL,
    orden         SMALLINT     NOT NULL,
    nombre        VARCHAR(50)  NOT NULL,
    resultado     VARCHAR(20)  NOT NULL,
    detalle       VARCHAR(255) NOT NULL,

    CONSTRAINT fk_resultado_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES solicitud (id) ON DELETE CASCADE,
    CONSTRAINT uq_resultado_orden
        UNIQUE (solicitud_id, orden),
    CONSTRAINT ck_resultado_valor
        CHECK (resultado IN ('APROBADO', 'RECHAZADO'))
);

CREATE INDEX idx_resultado_solicitud
    ON resultado_validacion (solicitud_id);
