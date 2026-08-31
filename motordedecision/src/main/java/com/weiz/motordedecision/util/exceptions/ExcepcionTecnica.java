package com.weiz.motordedecision.util.exceptions;

/**
 * Base de las excepciones tecnicas del motor: algo se rompio
 * ({@code docs/error-handling.md} §4).
 *
 * Se responde con 500 y un texto generico —nunca su propio mensaje, que
 * expondria detalles de implementacion— y se registra en {@code ERROR} con la
 * traza completa.
 *
 * El unico constructor exige la causa: perder el {@link Throwable} de origen es
 * perder el unico dato que sirve para diagnosticar. Ninguna excepcion de una
 * libreria de terceros escapa sin envolverse en una de estas.
 */
public abstract class ExcepcionTecnica extends RuntimeException {

    protected ExcepcionTecnica(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
