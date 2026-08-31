package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.domain.validacion.RegistroValidacion;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.IDENTIDAD;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.REPORTE_NEGATIVO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_ALTO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoConLaCadenaCompleta;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoRechazadoEn;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoSinInformeDeBuro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba la lectura del rastro, que es de donde salen los predicados con los
 * que estan escritas las seis reglas.
 *
 * Si {@code existeRechazoDe} confundiera una validacion con otra, o
 * {@code superaTodasLasValidaciones} diera cierto con el buro caido, ninguna
 * regla estaria mal escrita y todas decidirian mal.
 */
@DisplayName("Caso de decision")
class CasoDeDecisionTest {

    private final CasoDeDecision casoCompleto =
            crearCasoConLaCadenaCompleta(SCORE_ALTO, MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS);

    @Test
    @DisplayName("Con la cadena completa: hay informe, no hay rechazo y se superan todas las validaciones")
    void superaTodasLasValidaciones_conLaCadenaCompleta_devuelveVerdadero() {
        boolean supera = casoCompleto.superaTodasLasValidaciones();

        assertThat(supera).isTrue();
        assertThat(casoCompleto.hayInformeDelBuro()).isTrue();
        assertThat(casoCompleto.existeRechazo()).isFalse();
    }

    @ParameterizedTest(name = "rechazo en {0}")
    @ValueSource(strings = {IDENTIDAD, SCORE, REPORTE_NEGATIVO})
    @DisplayName("Un rechazo en el rastro impide superar la cadena, venga de la validacion que venga")
    void superaTodasLasValidaciones_conUnRechazoEnElRastro_devuelveFalso(String nombreValidacion) {
        CasoDeDecision caso = crearCasoRechazadoEn(nombreValidacion);

        boolean supera = caso.superaTodasLasValidaciones();

        assertThat(supera).isFalse();
    }

    @Test
    @DisplayName("Sin informe del buro no se superan todas las validaciones, aunque nadie haya rechazado")
    void superaTodasLasValidaciones_sinInformeDelBuro_devuelveFalso() {
        CasoDeDecision caso = crearCasoSinInformeDeBuro(ResultadoEvaluacion.APROBADO);

        boolean supera = caso.superaTodasLasValidaciones();

        assertThat(supera).isFalse();
    }

    @Test
    @DisplayName("Reconoce de que validacion vino el rechazo")
    void existeRechazoDe_conElRechazoDeEsaValidacion_devuelveVerdadero() {
        CasoDeDecision caso = crearCasoRechazadoEn(IDENTIDAD);

        boolean existe = caso.existeRechazoDe(IDENTIDAD);

        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("No confunde el rechazo de una validacion con el de otra")
    void existeRechazoDe_conElRechazoDeOtraValidacion_devuelveFalso() {
        CasoDeDecision caso = crearCasoRechazadoEn(SCORE);

        boolean existe = caso.existeRechazoDe(IDENTIDAD);

        assertThat(existe).isFalse();
    }

    @Test
    @DisplayName("Una validacion aprobada no cuenta como rechazo de esa validacion")
    void existeRechazoDe_conEsaValidacionAprobada_devuelveFalso() {
        boolean existe = casoCompleto.existeRechazoDe(IDENTIDAD);

        assertThat(existe).isFalse();
    }

    @Test
    @DisplayName("Devuelve el score del informe del buro")
    void obtenerScore_conInformeDelBuro_devuelveElScoreDelInforme() {
        int score = casoCompleto.obtenerScore();

        assertThat(score).isEqualTo(SCORE_ALTO);
    }

    @Test
    @DisplayName("Sin informe del buro el score es cero y no revienta: el relleno del resultado no disponible")
    void obtenerScore_sinInformeDelBuro_devuelveCeroYNoLanza() {
        CasoDeDecision caso = crearCasoSinInformeDeBuro(ResultadoEvaluacion.APROBADO);

        int score = caso.obtenerScore();

        assertThat(score).isZero();
    }

    @Test
    @DisplayName("El rastro se copia al construir el caso: nadie puede modificarlo despues")
    void construir_conUnaListaModificable_guardaUnaCopiaInmutable() {
        List<RegistroValidacion> rastroModificable = new ArrayList<>(casoCompleto.validaciones());
        CasoDeDecision caso = new CasoDeDecision(casoCompleto.solicitud(), rastroModificable);
        RegistroValidacion registroIntruso = rastroModificable.get(0);
        int cantidadDeRegistros = rastroModificable.size();

        rastroModificable.clear();

        assertThat(caso.validaciones()).hasSize(cantidadDeRegistros);
        assertThatThrownBy(() -> caso.validaciones().add(registroIntruso))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
