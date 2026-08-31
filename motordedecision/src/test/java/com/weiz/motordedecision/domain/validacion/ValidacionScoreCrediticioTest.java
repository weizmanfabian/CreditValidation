package com.weiz.motordedecision.domain.validacion;

import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.DOCUMENTO_LIMPIO;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.INGRESOS;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.MONTO;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija el umbral de la validacion de score y, sobre todo, sus bordes: 599
 * rechaza y 600 aprueba. El minimo entra por constructor, asi que el test lo
 * elige y no depende de la configuracion del entorno.
 */
@DisplayName("Validacion de score crediticio")
class ValidacionScoreCrediticioTest {

    private static final int SCORE_MINIMO = 600;

    private final ValidacionScoreCrediticio validacion = new ValidacionScoreCrediticio(SCORE_MINIMO);

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource({"300, RECHAZADO", "599, RECHAZADO", "600, APROBADO", "750, APROBADO"})
    @DisplayName("El score se compara contra el minimo configurado, con el borde incluido")
    void validar_segunElScoreDelBuro_devuelveElVeredictoDelUmbral(int score, ResultadoEvaluacion esperado) {
        SolicitudEvaluada solicitud = SolicitudesDePrueba
                .crearSolicitudConInforme(DOCUMENTO_LIMPIO, MONTO, INGRESOS, score, false);

        Veredicto veredicto = validacion.validar(solicitud);

        assertThat(veredicto.resultado()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("El detalle del rechazo nombra el score y el minimo incumplido")
    void validar_conScoreInsuficiente_dejaUnDetalleQueNombraElUmbral() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba
                .crearSolicitudConInforme(DOCUMENTO_LIMPIO, MONTO, INGRESOS, 420, true);

        Veredicto veredicto = validacion.validar(solicitud);

        assertThat(veredicto.detalle()).isEqualTo("Score 420 por debajo del minimo de 600");
    }

    @Test
    @DisplayName("Sin informe del buro no se pronuncia: un servicio caido no es un rechazo")
    void puedeEvaluarse_sinInformeDelBuro_devuelveFalso() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudSinInformeDeBuro(DOCUMENTO_LIMPIO);

        boolean puedeEvaluarse = validacion.puedeEvaluarse(solicitud);

        assertThat(puedeEvaluarse).isFalse();
    }

    @Test
    @DisplayName("Su nombre es el que publica el enunciado en la respuesta")
    void obtenerNombre_siempre_devuelveElNombrePublicadoEnLaRespuesta() {
        String nombre = validacion.obtenerNombre();

        assertThat(nombre).isEqualTo("Score");
    }
}
