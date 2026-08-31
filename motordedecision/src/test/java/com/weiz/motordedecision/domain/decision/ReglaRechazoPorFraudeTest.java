package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.IDENTIDAD;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.REPORTE_NEGATIVO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_ALTO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoConLaCadenaCompleta;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoRechazadoEn;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoSinInformeDeBuro;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que solo el rechazo de la validacion de identidad significa
 * fraude, y que significa fraude tambien con el buro caido.
 *
 * Ese ultimo caso es el que justifica que esta regla vaya la primera de la
 * lista: la validacion de identidad no consulta el buro, asi que su rechazo
 * esta en el rastro aunque la cadena se detuviera despues.
 */
@DisplayName("Regla de rechazo por fraude")
class ReglaRechazoPorFraudeTest {

    private final ReglaRechazoPorFraude regla = new ReglaRechazoPorFraude(IDENTIDAD);

    @Test
    @DisplayName("Aplica cuando la validacion de identidad rechazo la solicitud")
    void cumpleCondicion_conElRechazoDeIdentidad_aplica() {
        CasoDeDecision caso = crearCasoRechazadoEn(IDENTIDAD);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isTrue();
    }

    @Test
    @DisplayName("Aplica con el buro caido: un documento bloqueado se rechaza aunque no haya informe")
    void cumpleCondicion_conElRechazoDeIdentidadYSinInformeDelBuro_aplica() {
        CasoDeDecision caso = crearCasoSinInformeDeBuro(ResultadoEvaluacion.RECHAZADO);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isTrue();
    }

    @ParameterizedTest(name = "rechazo en {0}")
    @ValueSource(strings = {SCORE, REPORTE_NEGATIVO})
    @DisplayName("No aplica cuando quien rechazo fue otra validacion: eso es un rechazo corriente")
    void cumpleCondicion_conElRechazoDeOtraValidacion_noAplica(String nombreValidacion) {
        CasoDeDecision caso = crearCasoRechazadoEn(nombreValidacion);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isFalse();
    }

    @Test
    @DisplayName("No aplica cuando ninguna validacion rechazo")
    void cumpleCondicion_sinNingunRechazo_noAplica() {
        CasoDeDecision caso = crearCasoConLaCadenaCompleta(SCORE_ALTO, MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isFalse();
    }

    @Test
    @DisplayName("Lleva al estado que el enunciado reserva al fraude, no a un rechazo corriente")
    void obtenerEstado_siempre_devuelveRechazadoFraude() {
        EstadoSolicitud estado = regla.obtenerEstado();

        assertThat(estado).isEqualTo(EstadoSolicitud.RECHAZADO_FRAUDE);
    }
}
