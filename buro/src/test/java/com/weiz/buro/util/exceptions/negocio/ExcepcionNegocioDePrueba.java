package com.weiz.buro.util.exceptions.negocio;

import com.weiz.buro.util.exceptions.ExcepcionNegocio;

/**
 * Doble de prueba de la rama de negocio del manejador.
 *
 * El buro todavia no publica ninguna subclase concreta de negocio (D-013 de
 * {@code docs/decisions.md}): esta vive en {@code src/test} para ejercitar el
 * contrato —4xx con codigo y log WARN— sin dejar codigo muerto en produccion.
 */
public class ExcepcionNegocioDePrueba extends ExcepcionNegocio {

    public ExcepcionNegocioDePrueba(String mensaje) {
        super(CodigoErrorNegocio.TIPO_DOCUMENTO_NO_SOPORTADO, mensaje);
    }
}
