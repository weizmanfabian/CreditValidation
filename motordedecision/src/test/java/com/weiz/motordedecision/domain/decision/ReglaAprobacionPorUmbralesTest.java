package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.IDENTIDAD;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_CINCO_VECES_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_OCHO_VECES_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_OCHO_VECES_LOS_INGRESOS_MAS_UNO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_SIETE_VECES_Y_MEDIA_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.REPORTE_NEGATIVO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_ALTO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_INTERMEDIO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_JUSTO_POR_DEBAJO_DE_APROBADO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_MINIMO_DE_APROBADO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_MINIMO_DE_PREAPROBADO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoConLaCadenaCompleta;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoRechazadoEn;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoSinInformeDeBuro;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija los dos bordes de la regla en el sitio donde de verdad se rompen: el
 * punto exacto en el que deja de cumplirse.
 *
 * Cada caso parametrizado lleva su pareja al otro lado de la linea —700 contra
 * 699, ocho veces los ingresos contra ocho veces mas un peso— porque es lo
 * unico que distingue un {@code >=} de un {@code >}. Los umbrales entran por
 * constructor, igual que en produccion desde {@code reglas.decision}.
 */
@DisplayName("Regla de aprobacion por umbrales")
class ReglaAprobacionPorUmbralesTest {

    private static final int MULTIPLO_MAXIMO_DE_APROBADO = 8;
    private static final int MULTIPLO_MAXIMO_DE_PREAPROBADO = 5;

    private final ReglaAprobacionPorUmbrales regla = new ReglaAprobacionPorUmbrales(
            EstadoSolicitud.APROBADO, SCORE_MINIMO_DE_APROBADO, MULTIPLO_MAXIMO_DE_APROBADO);

    static Stream<Arguments> generarScoresAlrededorDelMinimo() {
        return Stream.of(
                Arguments.of(SCORE_ALTO, true),
                Arguments.of(SCORE_MINIMO_DE_APROBADO, true),
                Arguments.of(SCORE_JUSTO_POR_DEBAJO_DE_APROBADO, false));
    }

    static Stream<Arguments> generarMontosAlrededorDelTope() {
        return Stream.of(
                Arguments.of(MONTO_DE_SIETE_VECES_Y_MEDIA_LOS_INGRESOS, true),
                Arguments.of(MONTO_DE_OCHO_VECES_LOS_INGRESOS, true),
                Arguments.of(MONTO_DE_OCHO_VECES_LOS_INGRESOS_MAS_UNO, false));
    }

    @ParameterizedTest(name = "score {0} -> aplica={1}")
    @MethodSource("generarScoresAlrededorDelMinimo")
    @DisplayName("El score minimo entra en la regla: 700 aplica, 699 ya no")
    void cumpleCondicion_segunElScore_aplicaDesdeElMinimoInclusive(int score, boolean esperado) {
        CasoDeDecision caso = crearCasoConLaCadenaCompleta(score, MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "monto {0} -> aplica={1}")
    @MethodSource("generarMontosAlrededorDelTope")
    @DisplayName("El monto tope entra en la regla: ocho veces los ingresos cabe, un peso mas no")
    void cumpleCondicion_segunElMonto_aplicaHastaElMultiploInclusive(BigDecimal monto, boolean esperado) {
        CasoDeDecision caso = crearCasoConLaCadenaCompleta(SCORE_ALTO, monto);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "rechazo en {0}")
    @ValueSource(strings = {IDENTIDAD, SCORE, REPORTE_NEGATIVO})
    @DisplayName("Con una validacion rechazada no aplica, por muy buenos que sean el score y el monto")
    void cumpleCondicion_conUnaValidacionRechazada_noAplica(String nombreValidacion) {
        CasoDeDecision caso = crearCasoRechazadoEn(nombreValidacion);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isFalse();
    }

    @Test
    @DisplayName("Sin informe del buro no aplica: no hay score que comparar")
    void cumpleCondicion_sinInformeDelBuro_noAplica() {
        CasoDeDecision caso = crearCasoSinInformeDeBuro(ResultadoEvaluacion.APROBADO);

        boolean cumple = regla.cumpleCondicion(caso);

        assertThat(cumple).isFalse();
    }

    @Test
    @DisplayName("Con los umbrales de PREAPROBADO la misma clase decide la otra fila de la tabla")
    void cumpleCondicion_conLosUmbralesDePreaprobado_aplicaConUnScoreQueNoAprueba() {
        ReglaAprobacionPorUmbrales reglaDePreaprobado = new ReglaAprobacionPorUmbrales(
                EstadoSolicitud.PREAPROBADO, SCORE_MINIMO_DE_PREAPROBADO, MULTIPLO_MAXIMO_DE_PREAPROBADO);
        CasoDeDecision caso = crearCasoConLaCadenaCompleta(SCORE_INTERMEDIO, MONTO_DE_CINCO_VECES_LOS_INGRESOS);

        boolean cumple = reglaDePreaprobado.cumpleCondicion(caso);

        assertThat(cumple).isTrue();
    }

    @Test
    @DisplayName("Devuelve el estado con el que se construyo, que es la fila de la tabla que representa")
    void obtenerEstado_conLaReglaDeAprobado_devuelveAprobado() {
        EstadoSolicitud estado = regla.obtenerEstado();

        assertThat(estado).isEqualTo(EstadoSolicitud.APROBADO);
    }
}
