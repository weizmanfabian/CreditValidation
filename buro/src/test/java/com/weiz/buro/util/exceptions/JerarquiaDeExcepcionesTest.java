package com.weiz.buro.util.exceptions;

import com.weiz.buro.util.exceptions.negocio.CodigoErrorNegocio;
import com.weiz.buro.util.exceptions.negocio.ExcepcionNegocioDePrueba;
import com.weiz.buro.util.exceptions.tecnica.ExcepcionTecnicaDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba lo que distingue a las dos bases de {@code docs/error-handling.md}
 * §4: la de negocio publica un codigo y la tecnica conserva siempre la causa
 * original, que es el unico dato que sirve para diagnosticar.
 */
class JerarquiaDeExcepcionesTest {

    private static final String MENSAJE = "El tipo de documento NIT no se puede consultar en el buro";

    @Test
    @DisplayName("Una excepcion de negocio publica su codigo y conserva su mensaje")
    void obtenerCodigo_enUnaExcepcionDeNegocio_devuelveElCodigoPublicado() {
        ExcepcionNegocioDePrueba excepcion = new ExcepcionNegocioDePrueba(MENSAJE);

        assertThat(excepcion)
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessage(MENSAJE)
                .extracting(ExcepcionNegocio::obtenerCodigo)
                .isEqualTo(CodigoErrorNegocio.TIPO_DOCUMENTO_NO_SOPORTADO);
    }

    @Test
    @DisplayName("Una excepcion tecnica conserva la causa original")
    void construir_unaExcepcionTecnica_conservaLaCausaOriginal() {
        IllegalStateException causaOriginal = new IllegalStateException("conexion rechazada");

        ExcepcionTecnicaDePrueba excepcion = new ExcepcionTecnicaDePrueba("Fallo la simulacion", causaOriginal);

        assertThat(excepcion)
                .isInstanceOf(ExcepcionTecnica.class)
                .hasMessage("Fallo la simulacion")
                .cause()
                .isSameAs(causaOriginal);
    }
}
