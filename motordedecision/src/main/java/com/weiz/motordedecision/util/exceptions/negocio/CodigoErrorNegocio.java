package com.weiz.motordedecision.util.exceptions.negocio;

/**
 * Catalogo de codigos de error de negocio publicados por el motor
 * ({@code docs/error-handling.md} §4.3).
 *
 * El codigo es contrato publico: una vez publicado no se renombra, porque el
 * frontend y las pruebas dependen de el. La descripcion si puede reescribirse,
 * y viaja al cliente como el {@code message} del error (D-012).
 *
 * El enum no conoce el estado HTTP a proposito. Esa traduccion vive en el
 * {@code ControllerExceptionHandler}, la unica clase con motivo para saber de
 * HTTP, de modo que el mismo catalogo sirve si el servicio se consume por otro
 * transporte.
 */
public enum CodigoErrorNegocio {

    SOLICITUD_NO_ENCONTRADA("No existe una solicitud con el identificador indicado");

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
