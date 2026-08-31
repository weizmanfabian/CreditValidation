package com.weiz.motordedecision.api.models.response;

/**
 * Una validacion de la cadena, tal como se le muestra al solicitante.
 *
 * Son los tres campos que pide el enunciado —nombre, resultado y detalle— y ni
 * uno mas: el {@code orden} con que la cadena registro cada paso es
 * informacion interna y no sale por HTTP.
 *
 * El {@code resultado} viaja como texto, que es lo que ya guarda la entidad
 * {@code ResultadoValidacion}. La respuesta no reinterpreta lo que la cadena
 * escribio: lo repite.
 *
 * @param nombre nombre de la validacion
 * @param resultado {@code APROBADO} o {@code RECHAZADO}
 * @param detalle explicacion en lenguaje de negocio
 */
public record ValidacionRealizada(String nombre, String resultado, String detalle) {
}
