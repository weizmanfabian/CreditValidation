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
 * Comprueba que la regla condiciona sobre "alguna validacion rechazo" y no
 * sobre nombres concretos.
 *
 * El caso del rechazo de identidad esta a proposito: esta regla tambien aplica,
 * y lo que evita que un documento bloqueado acabe en RECHAZADO es ir detras de
 * {@link ReglaRechazoPorFraude} en la lista del motor, no una condicion escrita
 * aqui.
 */
@DisplayName("Regla de rechazo por validacion")
class ReglaRechazoPorValidacionTest {

    private final ReglaRechazoPorValidacion regla = new ReglaRechazoPorValidacion();

    @ParameterizedTest(name = "rechazo en {0}")
    @ValueSource(strings = {IDENTIDAD, SCORE, REPORTE_NEGATIVO})
    @DisplayName("Aplica ante el rechazo de cualquier validacion, sin nombrar a ninguna")
    void cumpleCondicion_conElRechazoDeCualquierValidacion_aplica(String nombreValidacion) {
        CasoDeDecision caso = crearCasoRechazadoEn(nombreValidacion);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isTrue();
    }

    @Test
    @DisplayName("No aplica cuando la cadena termino sin rechazos")
    void cumpleCondicion_sinNingunRechazo_noAplica() {
        CasoDeDecision caso = crearCasoConLaCadenaCompleta(SCORE_ALTO, MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isFalse();
    }

    @Test
    @DisplayName("No aplica con el buro caido si nadie rechazo: una cadena incompleta no es un rechazo")
    void cumpleCondicion_sinInformeDelBuroYSinRechazos_noAplica() {
        CasoDeDecision caso = crearCasoSinInformeDeBuro(ResultadoEvaluacion.APROBADO);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isFalse();
    }

    @Test
    @DisplayName("Lleva al rechazo corriente del enunciado")
    void obtenerEstado_siempre_devuelveRechazado() {
        EstadoSolicitud estado = regla.obtenerEstado();

        assertThat(estado).isEqualTo(EstadoSolicitud.RECHAZADO);
    }
}
