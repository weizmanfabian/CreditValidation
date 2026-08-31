package com.weiz.motordedecision.domain.validacion;

import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.DOCUMENTO_LIMPIO;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.INGRESOS;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.MONTO;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.SCORE_ALTO;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija que la validacion de reporte negativo mira solo esa bandera del informe.
 *
 * El caso del score alto con reporte negativo es el que importa: demuestra que
 * es independiente de la validacion de score y no repite su criterio.
 */
@DisplayName("Validacion de reporte negativo")
class ValidacionReporteNegativoTest {

    private final ValidacionReporteNegativo validacion = new ValidacionReporteNegativo();

    @Test
    @DisplayName("Sin antecedentes en el buro, la solicitud pasa")
    void validar_sinReporteNegativo_devuelveVeredictoAprobado() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudImpecable();

        Veredicto veredicto = validacion.validar(solicitud);

        assertThat(veredicto)
                .returns(ResultadoEvaluacion.APROBADO, Veredicto::resultado)
                .returns("Sin reportes negativos", Veredicto::detalle);
    }

    @Test
    @DisplayName("Con antecedentes se rechaza aunque el score sea alto: no mira el score")
    void validar_conReporteNegativoYScoreAlto_devuelveVeredictoRechazado() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba
                .crearSolicitudConInforme(DOCUMENTO_LIMPIO, MONTO, INGRESOS, SCORE_ALTO, true);

        Veredicto veredicto = validacion.validar(solicitud);

        assertThat(veredicto)
                .returns(ResultadoEvaluacion.RECHAZADO, Veredicto::resultado)
                .returns("El buro reporta antecedentes negativos", Veredicto::detalle);
    }

    @Test
    @DisplayName("Sin informe del buro no se pronuncia: la ausencia de informe no es ausencia de reportes")
    void puedeEvaluarse_sinInformeDelBuro_devuelveFalso() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudSinInformeDeBuro(DOCUMENTO_LIMPIO);

        boolean puedeEvaluarse = validacion.puedeEvaluarse(solicitud);

        assertThat(puedeEvaluarse).isFalse();
    }

    @Test
    @DisplayName("Su nombre es el que publica el enunciado en la respuesta")
    void obtenerNombre_siempre_devuelveElNombrePublicadoEnLaRespuesta() {
        String nombre = validacion.obtenerNombre();

        assertThat(nombre).isEqualTo("Reporte negativo");
    }
}
