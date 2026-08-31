-- =====================================================================
-- Datos iniciales de desarrollo.
--
-- Existen para que la pantalla de consulta de estado tenga algo que mostrar
-- desde el primer arranque, sin tener que radicar solicitudes a mano. No son
-- datos de prueba automatizada: los tests corren contra H2 y se siembran sus
-- propios datos (docs/architecture.md §5).
--
-- Los documentos elegidos ejercitan los cinco estados del enunciado y son
-- coherentes con las reglas del buro simulado:
--   1234567890  termina en par   -> score alto, sin reporte negativo
--   1098765431  termina en impar -> score bajo, con reporte negativo
--   0000000000  documento reservado para el caso de servicio caido
--   1010101010  documento de la lista bloqueada (ver docs/decisions.md)
--
-- Los `id` se dejan a la secuencia; las filas hijas referencian la solicitud
-- por su identificador de negocio para no depender de valores autoincrementales.
-- =====================================================================

INSERT INTO solicitud (
    id_solicitud, tipo_documento, numero_documento, nombres, apellidos,
    correo_electronico, telefono_celular, monto_solicitado, plazo_meses,
    ingresos_mensuales, estado, score_buro, tasa_estimada, fecha_creacion
) VALUES
    ('SOL-20260828-001', 'CC', '1234567890', 'Juan', 'Perez',
     'juan.perez@example.com', '3001234567', 15000000, 36,
     4000000, 'APROBADO', 750, 1.20, TIMESTAMP '2026-08-28 10:30:00'),

    ('SOL-20260829-001', 'CC', '1234567890', 'Juan', 'Perez',
     'juan.perez@example.com', '3001234567', 30000000, 48,
     4000000, 'PREAPROBADO', 640, 1.85, TIMESTAMP '2026-08-29 09:15:00'),

    ('SOL-20260829-002', 'CC', '1098765431', 'Maria', 'Gomez',
     'maria.gomez@example.com', '3109876543', 20000000, 24,
     5000000, 'RECHAZADO', 420, NULL, TIMESTAMP '2026-08-29 11:00:00'),

    ('SOL-20260830-001', 'CE', '1010101010', 'Carlos', 'Ramirez',
     'carlos.ramirez@example.com', '3201112233', 10000000, 12,
     3000000, 'RECHAZADO_FRAUDE', NULL, NULL, TIMESTAMP '2026-08-30 08:05:00'),

    ('SOL-20260830-002', 'CC', '0000000000', 'Ana', 'Torres',
     'ana.torres@example.com', '3157778899', 25000000, 36,
     6000000, 'PENDIENTE_REVISION', NULL, NULL, TIMESTAMP '2026-08-30 08:40:00');

-- Rastro de validaciones. Nota el corte temprano: la solicitud rechazada por
-- score no llega a evaluar capacidad de pago ni reporte negativo, y la de
-- fraude se detiene en la primera validacion.
INSERT INTO resultado_validacion (solicitud_id, orden, nombre, resultado, detalle)
SELECT s.id, v.orden, v.nombre, v.resultado, v.detalle
FROM (VALUES
    ('SOL-20260828-001', 1::SMALLINT, 'Identidad',         'APROBADO',  'Documento no bloqueado'),
    ('SOL-20260828-001', 2::SMALLINT, 'Score',             'APROBADO',  'Score 750 >= 700'),
    ('SOL-20260828-001', 3::SMALLINT, 'Capacidad de pago', 'APROBADO',  'Monto dentro del rango permitido'),
    ('SOL-20260828-001', 4::SMALLINT, 'Reporte negativo',  'APROBADO',  'Sin reportes negativos'),

    ('SOL-20260829-001', 1::SMALLINT, 'Identidad',         'APROBADO',  'Documento no bloqueado'),
    ('SOL-20260829-001', 2::SMALLINT, 'Score',             'APROBADO',  'Score 640 >= 600'),
    ('SOL-20260829-001', 3::SMALLINT, 'Capacidad de pago', 'APROBADO',  'Monto dentro del rango permitido'),
    ('SOL-20260829-001', 4::SMALLINT, 'Reporte negativo',  'APROBADO',  'Sin reportes negativos'),

    ('SOL-20260829-002', 1::SMALLINT, 'Identidad',         'APROBADO',  'Documento no bloqueado'),
    ('SOL-20260829-002', 2::SMALLINT, 'Score',             'RECHAZADO', 'Score 420 por debajo del minimo de 600'),

    ('SOL-20260830-001', 1::SMALLINT, 'Identidad',         'RECHAZADO', 'Documento reportado en la lista de bloqueados'),

    ('SOL-20260830-002', 1::SMALLINT, 'Identidad',         'APROBADO',  'Documento no bloqueado')
) AS v (id_solicitud, orden, nombre, resultado, detalle)
JOIN solicitud s ON s.id_solicitud = v.id_solicitud;
