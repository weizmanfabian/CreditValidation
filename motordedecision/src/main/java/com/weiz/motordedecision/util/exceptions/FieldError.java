package com.weiz.motordedecision.util.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Un campo rechazado dentro de un {@link ErrorResponse}.
 *
 * El valor de {@code location} es lo que permite identificar el error de un
 * vistazo al probar por Postman: dice si hay que corregir el JSON del cuerpo,
 * una cabecera o un dato de la URL ({@code docs/error-handling.md} §2).
 *
 * No se construye con el constructor canonico: se usa la factoria que
 * corresponde al origen del dato, y asi el catalogo de localizaciones vive en un
 * solo sitio.
 *
 * @param field nombre del campo que fallo
 * @param message mensaje en espanol, escrito en la anotacion del DTO
 * @param location origen del dato: {@code body}, {@code header}, {@code query} o {@code path}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldError(String field, String message, String location) {

    private static final String LOCALIZACION_CUERPO = "body";
    private static final String LOCALIZACION_HEADER = "header";
    private static final String LOCALIZACION_PARAMETRO = "query";
    private static final String LOCALIZACION_RUTA = "path";

    /**
     * Crea el error de un campo que viajaba en el cuerpo JSON de la peticion.
     */
    public static FieldError crearDeCuerpo(String campo, String mensaje) {
        return new FieldError(campo, mensaje, LOCALIZACION_CUERPO);
    }

    /**
     * Crea el error de un dato que viajaba en una cabecera HTTP.
     */
    public static FieldError crearDeHeader(String campo, String mensaje) {
        return new FieldError(campo, mensaje, LOCALIZACION_HEADER);
    }

    /**
     * Crea el error de un dato que viajaba como parametro de consulta de la URL.
     */
    public static FieldError crearDeParametro(String campo, String mensaje) {
        return new FieldError(campo, mensaje, LOCALIZACION_PARAMETRO);
    }

    /**
     * Crea el error de un dato que viajaba como variable de la ruta.
     */
    public static FieldError crearDeRuta(String campo, String mensaje) {
        return new FieldError(campo, mensaje, LOCALIZACION_RUTA);
    }
}
