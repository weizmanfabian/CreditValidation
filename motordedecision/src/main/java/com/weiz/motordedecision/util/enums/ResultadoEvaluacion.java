package com.weiz.motordedecision.util.enums;

/**
 * Como termino una validacion de la cadena.
 *
 * Son los dos unicos valores que el enunciado publica dentro de
 * {@code evaluacion.validaciones}, y viajan tal cual a la respuesta y a la
 * columna {@code resultado} de {@code resultado_validacion}.
 *
 * Un rechazo no es un error: es el resultado esperado de evaluar una regla
 * (docs/conventions.md §6).
 */
public enum ResultadoEvaluacion {

    APROBADO,

    RECHAZADO
}
