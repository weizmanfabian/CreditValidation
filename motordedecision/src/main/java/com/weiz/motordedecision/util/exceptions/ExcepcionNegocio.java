package com.weiz.motordedecision.util.exceptions;

import com.weiz.motordedecision.util.exceptions.negocio.CodigoErrorNegocio;

/**
 * Base de las excepciones de negocio del motor: el sistema funciona bien y la
 * respuesta es que no ({@code docs/error-handling.md} §4).
 *
 * Se responde con 4xx, lleva un {@link CodigoErrorNegocio} publicado y se
 * registra en {@code WARN} sin traza de pila. Repetir la misma peticion daria el
 * mismo resultado, asi que nunca se reintenta.
 *
 * En este sistema son pocas a proposito: un credito rechazado, un documento en
 * lista negra o el buro caido son resultados con 200, no errores. Las
 * validaciones de {@code domain/validacion} devuelven un resultado y no lanzan
 * nada.
 *
 * Es no comprobada: obligar a declarar {@code throws} en cada firma intermedia
 * ensucia el codigo sin aportar, porque ninguna capa intermedia puede hacer algo
 * util con ella.
 */
public abstract class ExcepcionNegocio extends RuntimeException {

    private final transient CodigoErrorNegocio codigo;

    protected ExcepcionNegocio(CodigoErrorNegocio codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    /**
     * Devuelve el codigo publicado que identifica la regla incumplida.
     */
    public CodigoErrorNegocio obtenerCodigo() {
        return codigo;
    }
}
