package com.weiz.motordedecision.util.enums;

/**
 * En que termina una solicitud de credito una vez evaluada.
 *
 * Son los cinco valores que publica la tabla de decision del enunciado y los
 * unicos que pueden llegar a la columna {@code estado} de la tabla
 * {@code solicitud}, donde ya viven los cinco en los datos de arranque de
 * {@code db/sql/02-data.sql}.
 *
 * No se confunde con {@code ResultadoEvaluacion}, que es como termina una
 * validacion suelta dentro de la cadena. Una solicitud con todas sus
 * validaciones en {@code APROBADO} todavia puede quedar {@code PREAPROBADO}:
 * quien traduce lo uno en lo otro son las reglas de {@code domain/decision}.
 */
public enum EstadoSolicitud {

    APROBADO,

    PREAPROBADO,

    RECHAZADO,

    RECHAZADO_FRAUDE,

    PENDIENTE_REVISION
}
