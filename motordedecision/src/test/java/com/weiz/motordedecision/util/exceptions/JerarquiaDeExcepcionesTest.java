package com.weiz.motordedecision.util.exceptions;

import com.weiz.motordedecision.util.exceptions.negocio.CodigoErrorNegocio;
import com.weiz.motordedecision.util.exceptions.negocio.SolicitudNoEncontrada;
import com.weiz.motordedecision.util.exceptions.tecnica.BuroNoDisponible;
import com.weiz.motordedecision.util.exceptions.tecnica.ErrorDePersistencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba lo que distingue a las dos bases de {@code docs/error-handling.md}
 * §4: la de negocio publica un codigo y la tecnica conserva siempre la causa
 * original, que es el unico dato que sirve para diagnosticar.
 *
 * Las dos tecnicas del motor envuelven una excepcion de terceros
 * ({@link RestClientException} y {@link DataAccessException}) y su constructor
 * la exige por tipo: no hay forma de construirlas perdiendo el origen del
 * fallo, que es la regla de {@code docs/conventions.md} §6.
 */
class JerarquiaDeExcepcionesTest {

    private static final String TIPO_DOCUMENTO = "CC";
    private static final String NUMERO_DOCUMENTO = "1234567890";
    private static final String MENSAJE_BURO = "El servicio de buro no respondio";
    private static final String MENSAJE_PERSISTENCIA = "Fallo al guardar la solicitud radicada";

    @Test
    @DisplayName("SolicitudNoEncontrada publica su codigo y detalla el documento consultado")
    void obtenerCodigo_enSolicitudNoEncontrada_devuelveElCodigoPublicado() {
        SolicitudNoEncontrada excepcion = new SolicitudNoEncontrada(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(excepcion)
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessage("No hay solicitudes para CC 1234567890")
                .extracting(ExcepcionNegocio::obtenerCodigo)
                .isEqualTo(CodigoErrorNegocio.SOLICITUD_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("BuroNoDisponible envuelve la excepcion del cliente HTTP conservando la causa")
    void construir_conUnFalloDelClienteHttp_envuelveLaCausaOriginal() {
        RestClientException causaOriginal = new ResourceAccessException("connect timed out");

        BuroNoDisponible excepcion = new BuroNoDisponible(MENSAJE_BURO, causaOriginal);

        assertThat(excepcion)
                .isInstanceOf(ExcepcionTecnica.class)
                .hasMessage(MENSAJE_BURO)
                .cause()
                .isSameAs(causaOriginal);
    }

    @Test
    @DisplayName("ErrorDePersistencia envuelve la excepcion de acceso a datos conservando la causa")
    void construir_conUnFalloDeAccesoADatos_envuelveLaCausaOriginal() {
        DataAccessException causaOriginal =
                new DataAccessResourceFailureException("no se pudo obtener una conexion del pool");

        ErrorDePersistencia excepcion = new ErrorDePersistencia(MENSAJE_PERSISTENCIA, causaOriginal);

        assertThat(excepcion)
                .isInstanceOf(ExcepcionTecnica.class)
                .hasMessage(MENSAJE_PERSISTENCIA)
                .cause()
                .isSameAs(causaOriginal);
    }
}
