package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_CINCO_VECES_LOS_INGRESOS_MAS_UNO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_INTERMEDIO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoConLaCadenaCompleta;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoSinInformeDeBuro;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba la propiedad de la que depende que el motor nunca lance
 * {@code IllegalStateException}: esta regla se cumple siempre, sea cual sea el
 * caso.
 *
 * Es la fila que el enunciado no escribio (D-043) y la que cierra la lista.
 */
@DisplayName("Regla de revision manual")
class ReglaRevisionManualTest {

    private final ReglaRevisionManual regla = new ReglaRevisionManual();

    @Test
    @DisplayName("Se cumple con el hueco de la tabla: score entre 600 y 699 con un monto de mas de cinco veces")
    void cumpleCondicion_conElHuecoDeLaTabla_aplica() {
        CasoDeDecision caso = crearCasoConLaCadenaCompleta(
                SCORE_INTERMEDIO, MONTO_DE_CINCO_VECES_LOS_INGRESOS_MAS_UNO);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isTrue();
    }

    @Test
    @DisplayName("Se cumple tambien con el buro caido, que es un caso sin score que mirar")
    void cumpleCondicion_sinInformeDelBuro_aplica() {
        CasoDeDecision caso = crearCasoSinInformeDeBuro(ResultadoEvaluacion.APROBADO);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isTrue();
    }

    @Test
    @DisplayName("Lleva a revision manual: alguien mira a mano lo que la tabla no resuelve")
    void obtenerEstado_siempre_devuelvePendienteRevision() {
        EstadoSolicitud estado = regla.obtenerEstado();

        assertThat(estado).isEqualTo(EstadoSolicitud.PENDIENTE_REVISION);
    }
}
