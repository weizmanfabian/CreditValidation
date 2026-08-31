package com.weiz.motordedecision.api.models.response;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba el constructor por secciones: que el sobre viaje siempre, que cada
 * seccion se agregue por su cuenta y que lo que no se agrega quede fuera.
 *
 * Es lo que sostiene el criterio de la feature —agregar una seccion no obliga a
 * tocar las existentes—: si agregar una seccion tuviera efecto sobre otra,
 * estos tests se pondrian rojos.
 */
@DisplayName("Constructor por secciones de la respuesta")
class SolicitudCreditoResponseTest {

    private static final String ID_SOLICITUD = "SOL-20260824-001";
    private static final LocalDateTime FECHA_CREACION = LocalDateTime.of(2026, 8, 24, 10, 30);
    private static final String SIGUIENTE_PASO = "Se enviará contrato al correo registrado en 24 horas";

    private static final SeccionSolicitante SOLICITANTE =
            new SeccionSolicitante("Juan Pérez", "CC 1234567890");
    private static final SeccionDetalleFinanciero DETALLE =
            new SeccionDetalleFinanciero(new BigDecimal("15000000"), (short) 36, new BigDecimal("1.2"));
    private static final SeccionEvaluacion EVALUACION =
            new SeccionEvaluacion(750, List.of(new ValidacionRealizada("Identidad", "APROBADO", "Documento no bloqueado")));

    @Test
    @DisplayName("Sin agregar ninguna seccion: la respuesta trae solo el sobre")
    void construir_sinSecciones_devuelveSoloElSobre() {
        SolicitudCreditoResponse respuesta = crearConstructor(EstadoSolicitud.RECHAZADO_FRAUDE).construir();

        assertThat(respuesta)
                .returns(ID_SOLICITUD, SolicitudCreditoResponse::idSolicitud)
                .returns(FECHA_CREACION, SolicitudCreditoResponse::fechaCreacion)
                .returns("RECHAZADO_FRAUDE", SolicitudCreditoResponse::estado)
                .returns(null, SolicitudCreditoResponse::solicitante)
                .returns(null, SolicitudCreditoResponse::detalle)
                .returns(null, SolicitudCreditoResponse::evaluacion)
                .returns(null, SolicitudCreditoResponse::siguientePaso);
    }

    @Test
    @DisplayName("Con las cuatro secciones: la respuesta las trae todas")
    void construir_conLasCuatroSecciones_lasDevuelveTodas() {
        SolicitudCreditoResponse respuesta = crearConstructor(EstadoSolicitud.APROBADO)
                .agregarSolicitante(SOLICITANTE)
                .agregarDetalle(DETALLE)
                .agregarEvaluacion(EVALUACION)
                .agregarSiguientePaso(SIGUIENTE_PASO)
                .construir();

        assertThat(respuesta)
                .returns(SOLICITANTE, SolicitudCreditoResponse::solicitante)
                .returns(DETALLE, SolicitudCreditoResponse::detalle)
                .returns(EVALUACION, SolicitudCreditoResponse::evaluacion)
                .returns(SIGUIENTE_PASO, SolicitudCreditoResponse::siguientePaso);
    }

    @Test
    @DisplayName("Agregar una seccion no altera las demas: sin siguiente paso, el resto sigue en pie")
    void construir_sinSiguientePaso_conservaLasOtrasSecciones() {
        SolicitudCreditoResponse respuesta = crearConstructor(EstadoSolicitud.RECHAZADO_FRAUDE)
                .agregarSolicitante(SOLICITANTE)
                .agregarEvaluacion(EVALUACION)
                .construir();

        assertThat(respuesta)
                .returns(SOLICITANTE, SolicitudCreditoResponse::solicitante)
                .returns(EVALUACION, SolicitudCreditoResponse::evaluacion)
                .returns(null, SolicitudCreditoResponse::detalle)
                .returns(null, SolicitudCreditoResponse::siguientePaso);
    }

    @Test
    @DisplayName("El orden en que se agregan las secciones no cambia la respuesta")
    void construir_conLasSeccionesEnOtroOrden_devuelveLaMismaRespuesta() {
        SolicitudCreditoResponse enUnOrden = crearConstructor(EstadoSolicitud.APROBADO)
                .agregarSolicitante(SOLICITANTE)
                .agregarDetalle(DETALLE)
                .construir();

        SolicitudCreditoResponse enElOtro = crearConstructor(EstadoSolicitud.APROBADO)
                .agregarDetalle(DETALLE)
                .agregarSolicitante(SOLICITANTE)
                .construir();

        assertThat(enUnOrden).isEqualTo(enElOtro);
    }

    @Test
    @DisplayName("La lista de validaciones queda inmutable dentro de la seccion")
    void seccionEvaluacion_conUnaListaMutable_laCopiaYNoDejaModificarla() {
        List<ValidacionRealizada> mutable = new ArrayList<>();
        mutable.add(new ValidacionRealizada("Identidad", "APROBADO", "Documento no bloqueado"));
        SeccionEvaluacion evaluacion = new SeccionEvaluacion(750, mutable);

        mutable.clear();

        List<ValidacionRealizada> copiadas = evaluacion.validaciones();
        assertThat(copiadas).hasSize(1);
        assertThatThrownBy(copiadas::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("La seccion admite una lista nula de validaciones y la guarda vacia")
    void seccionEvaluacion_conValidacionesNulas_guardaUnaListaVacia() {
        SeccionEvaluacion evaluacion = new SeccionEvaluacion(750, null);

        assertThat(evaluacion.validaciones()).isEmpty();
    }

    private SolicitudCreditoResponse.Builder crearConstructor(EstadoSolicitud estado) {
        return SolicitudCreditoResponse.crearConstructorPara(ID_SOLICITUD, FECHA_CREACION, estado);
    }
}
