package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_ALTO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoConLaCadenaCompleta;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoSinInformeDeBuro;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que la caida del buro termina en PENDIENTE_REVISION y no en un
 * rechazo al solicitante, que es la fila que el enunciado le reserva.
 */
@DisplayName("Regla de buro no disponible")
class ReglaBuroNoDisponibleTest {

    private final ReglaBuroNoDisponible regla = new ReglaBuroNoDisponible();

    @Test
    @DisplayName("Aplica cuando la consulta al buro no trajo informe")
    void cumpleCondicion_sinInformeDelBuro_aplica() {
        CasoDeDecision caso = crearCasoSinInformeDeBuro(ResultadoEvaluacion.APROBADO);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isTrue();
    }

    @Test
    @DisplayName("No aplica cuando el buro respondio, aunque la solicitud vaya a rechazarse por otra cosa")
    void cumpleCondicion_conInformeDelBuro_noAplica() {
        CasoDeDecision caso = crearCasoConLaCadenaCompleta(SCORE_ALTO, MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isFalse();
    }

    @Test
    @DisplayName("Lleva a revision manual, no a un rechazo: la falla es del servicio, no del solicitante")
    void obtenerEstado_siempre_devuelvePendienteRevision() {
        EstadoSolicitud estado = regla.obtenerEstado();

        assertThat(estado).isEqualTo(EstadoSolicitud.PENDIENTE_REVISION);
    }
}
