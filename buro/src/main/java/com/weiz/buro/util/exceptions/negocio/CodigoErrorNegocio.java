package com.weiz.buro.util.exceptions.negocio;

/**
 * Catalogo de codigos de error de negocio publicados por el buro
 * ({@code docs/error-handling.md} §4.3).
 *
 * El codigo es contrato publico: una vez publicado no se renombra, porque el
 * frontend y las pruebas dependen de el. La descripcion si puede reescribirse.
 *
 * El enum no conoce el estado HTTP a proposito. Esa traduccion vive en el
 * {@code ControllerExceptionHandler}, la unica clase con motivo para saber de
 * HTTP, de modo que el mismo catalogo sirve si el servicio se consume por otro
 * transporte.
 */
public enum CodigoErrorNegocio {

    TIPO_DOCUMENTO_NO_SOPORTADO("El tipo de documento no es CC, CE ni PA");

    private final String descripcion;

    CodigoErrorNegocio(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el texto estable que se publica como {@code message} del error.
     */
    public String obtenerDescripcion() {
        return descripcion;
    }
}
