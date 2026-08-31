package com.weiz.motordedecision.domain.validacion;

import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.DOCUMENTO_LIMPIO;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.SCORE_ALTO;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija la relacion entre monto e ingresos, con el borde exacto del multiplo:
 * ocho veces los ingresos todavia pasa; un peso mas, no.
 */
@DisplayName("Validacion de capacidad de pago")
class ValidacionCapacidadDePagoTest {

    private static final int MULTIPLO_MAXIMO = 8;
    private static final BigDecimal INGRESOS_MENSUALES = new BigDecimal("4000000");

    private final ValidacionCapacidadDePago validacion = new ValidacionCapacidadDePago(MULTIPLO_MAXIMO);

    @ParameterizedTest(name = "monto {0} sobre ingresos 4000000 -> {1}")
    @CsvSource({
            "15000000, APROBADO",
            "32000000, APROBADO",
            "32000001, RECHAZADO",
            "50000000, RECHAZADO"})
    @DisplayName("El monto se compara contra el multiplo de los ingresos, con el borde incluido")
    void validar_segunLaRelacionMontoIngresos_devuelveElVeredictoDelMultiplo(String monto,
                                                                            ResultadoEvaluacion esperado) {
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudConInforme(
                DOCUMENTO_LIMPIO, new BigDecimal(monto), INGRESOS_MENSUALES, SCORE_ALTO, false);

        Veredicto veredicto = validacion.validar(solicitud);

        assertThat(veredicto.resultado()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("El detalle del rechazo nombra el multiplo configurado")
    void validar_conMontoExcesivo_dejaUnDetalleQueNombraElMultiplo() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudConInforme(
                DOCUMENTO_LIMPIO, new BigDecimal("50000000"), INGRESOS_MENSUALES, SCORE_ALTO, false);

        Veredicto veredicto = validacion.validar(solicitud);

        assertThat(veredicto.detalle()).isEqualTo("Monto solicitado supera 8 veces los ingresos mensuales");
    }

    @Test
    @DisplayName("No depende del buro: los ingresos y el monto los trae la solicitud")
    void puedeEvaluarse_sinInformeDelBuro_devuelveVerdadero() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudSinInformeDeBuro(DOCUMENTO_LIMPIO);

        boolean puedeEvaluarse = validacion.puedeEvaluarse(solicitud);

        assertThat(puedeEvaluarse).isTrue();
    }

    @Test
    @DisplayName("Su nombre es el que publica el enunciado en la respuesta")
    void obtenerNombre_siempre_devuelveElNombrePublicadoEnLaRespuesta() {
        String nombre = validacion.obtenerNombre();

        assertThat(nombre).isEqualTo("Capacidad de pago");
    }
}
