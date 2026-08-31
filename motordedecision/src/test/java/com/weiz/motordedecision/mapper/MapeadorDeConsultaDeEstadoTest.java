package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.EstadoDeSolicitudResponse;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El mapeador de la consulta de estado copia de la entidad exactamente lo que
 * el elemento de la lista publica: el sobre de la solicitud y sus cifras.
 *
 * La tasa se copia de la columna, donde quedo sellada al radicar (D-051): aqui
 * no hay configuracion de la que leer, asi que un cambio de tasas vigentes no
 * puede tocar lo que responde la consulta.
 */
@DisplayName("Mapeador de la consulta de estado")
class MapeadorDeConsultaDeEstadoTest {

    private static final BigDecimal TASA_SELLADA_AL_RADICAR = new BigDecimal("1.20");

    private final MapeadorDeConsultaDeEstado mapeador = new MapeadorDeConsultaDeEstado();

    @Test
    @DisplayName("Una solicitud aprobada se mapea con sus seis campos, tasa incluida")
    void mapearAEstado_conSolicitudAprobadaConTasa_copiaLosSeisCampos() {
        Solicitud solicitud = SolicitudesPersistidasDePrueba
                .crearSolicitudConLaCadenaCompleta(EstadoSolicitud.APROBADO);
        solicitud.setTasaEstimada(TASA_SELLADA_AL_RADICAR);

        EstadoDeSolicitudResponse elemento = mapeador.mapearAEstado(solicitud);

        assertThat(elemento)
                .returns(SolicitudesPersistidasDePrueba.ID_SOLICITUD, EstadoDeSolicitudResponse::idSolicitud)
                .returns(SolicitudesPersistidasDePrueba.FECHA_CREACION, EstadoDeSolicitudResponse::fechaCreacion)
                .returns(EstadoSolicitud.APROBADO.name(), EstadoDeSolicitudResponse::estado)
                .returns(SolicitudesPersistidasDePrueba.MONTO_SOLICITADO,
                        EstadoDeSolicitudResponse::montoSolicitado)
                .returns(SolicitudesPersistidasDePrueba.PLAZO_MESES, EstadoDeSolicitudResponse::plazoMeses)
                .returns(TASA_SELLADA_AL_RADICAR, EstadoDeSolicitudResponse::tasaEstimada);
    }

    @Test
    @DisplayName("Una solicitud sin oferta se mapea con la tasa nula, que el JSON omite")
    void mapearAEstado_conSolicitudSinTasa_dejaLaTasaNula() {
        Solicitud solicitud = SolicitudesPersistidasDePrueba.crearSolicitudRechazadaPorDocumentoBloqueado();

        EstadoDeSolicitudResponse elemento = mapeador.mapearAEstado(solicitud);

        assertThat(elemento)
                .returns(EstadoSolicitud.RECHAZADO_FRAUDE.name(), EstadoDeSolicitudResponse::estado)
                .returns(null, EstadoDeSolicitudResponse::tasaEstimada);
    }
}
