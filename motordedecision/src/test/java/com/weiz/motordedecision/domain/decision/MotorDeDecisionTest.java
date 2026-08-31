package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_ALTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fija las dos propiedades que hacen de esto un motor de reglas y no una
 * escalera de condicionales: decide la primera regla que aplica, y el orden es
 * el que recibe.
 *
 * La tabla del enunciado, fila por fila, se prueba contra el motor real armado
 * desde la configuracion en {@code config/ConfiguracionReglasDeDecisionTest}.
 * Aqui las reglas son de mentira a proposito: lo que se comprueba es el
 * recorrido, no los umbrales; y cada regla de verdad tiene su propia clase de
 * test en este mismo paquete.
 */
@DisplayName("Motor de decision")
class MotorDeDecisionTest {

    private final CasoDeDecision caso = CasosDeDecisionDePrueba.crearCasoConLaCadenaCompleta(
            SCORE_ALTO, MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS);

    @Test
    @DisplayName("Decide la primera regla que aplica, aunque las siguientes tambien apliquen")
    void decidirEstado_conVariasReglasQueAplican_devuelveElEstadoDeLaPrimera() {
        MotorDeDecision motor = new MotorDeDecision(List.of(
                new ReglaFija(true, EstadoSolicitud.RECHAZADO_FRAUDE),
                new ReglaFija(true, EstadoSolicitud.APROBADO)));

        EstadoSolicitud estado = motor.decidirEstado(caso);

        assertThat(estado).isEqualTo(EstadoSolicitud.RECHAZADO_FRAUDE);
    }

    @Test
    @DisplayName("Invertir el orden de las reglas cambia el estado: el orden es contrato")
    void decidirEstado_conLasReglasEnOrdenInverso_devuelveElOtroEstado() {
        MotorDeDecision motor = new MotorDeDecision(List.of(
                new ReglaFija(true, EstadoSolicitud.APROBADO),
                new ReglaFija(true, EstadoSolicitud.RECHAZADO_FRAUDE)));

        EstadoSolicitud estado = motor.decidirEstado(caso);

        assertThat(estado).isEqualTo(EstadoSolicitud.APROBADO);
    }

    @Test
    @DisplayName("Las reglas que no aplican se saltan sin decidir nada")
    void decidirEstado_conReglasQueNoAplican_devuelveElEstadoDeLaQueSiAplica() {
        MotorDeDecision motor = new MotorDeDecision(List.of(
                new ReglaFija(false, EstadoSolicitud.RECHAZADO_FRAUDE),
                new ReglaFija(false, EstadoSolicitud.RECHAZADO),
                new ReglaFija(true, EstadoSolicitud.PREAPROBADO)));

        EstadoSolicitud estado = motor.decidirEstado(caso);

        assertThat(estado).isEqualTo(EstadoSolicitud.PREAPROBADO);
    }

    @Test
    @DisplayName("Una lista sin regla final que se cumpla siempre es un defecto de configuracion")
    void decidirEstado_sinNingunaReglaQueAplique_fallaEnVozAlta() {
        MotorDeDecision motor = new MotorDeDecision(List.of(
                new ReglaFija(false, EstadoSolicitud.APROBADO)));

        assertThatThrownBy(() -> motor.decidirEstado(caso))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ultima de la lista");
    }

    @Test
    @DisplayName("Anadir una regla nueva no obliga a tocar el motor ni las existentes")
    void decidirEstado_conUnaReglaNuevaPorDelante_laNuevaDecide() {
        ReglaDecision reglaNueva = new ReglaDecision() {
            @Override
            public boolean cumpleCondicion(CasoDeDecision unCaso) {
                return unCaso.obtenerScore() > SCORE_ALTO - 1;
            }

            @Override
            public EstadoSolicitud obtenerEstado() {
                return EstadoSolicitud.PENDIENTE_REVISION;
            }
        };
        MotorDeDecision motor = new MotorDeDecision(List.of(
                reglaNueva,
                new ReglaFija(true, EstadoSolicitud.APROBADO)));

        EstadoSolicitud estado = motor.decidirEstado(caso);

        assertThat(estado).isEqualTo(EstadoSolicitud.PENDIENTE_REVISION);
    }

    /** Regla de prueba que aplica o no aplica segun se le diga, sin mirar el caso. */
    private record ReglaFija(boolean aplica, EstadoSolicitud estado) implements ReglaDecision {

        @Override
        public boolean cumpleCondicion(CasoDeDecision caso) {
            return aplica;
        }

        @Override
        public EstadoSolicitud obtenerEstado() {
            return estado;
        }
    }
}
