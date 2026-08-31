package com.weiz.buro.domain.informe;

import com.weiz.buro.api.models.response.InformeCrediticioResponse;
import com.weiz.buro.util.enums.EstadoTitular;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba la tercera regla del mock del enunciado: el documento reservado para
 * simular el servicio caido retrasa la respuesta, y ningun otro lo hace.
 *
 * La pausa se sustituye por un doble que solo anota lo que se le pidio: asi se
 * comprueba que se solicita exactamente la duracion configurada sin esperarla,
 * y el test no depende del reloj de pared ({@code docs/verification.md} §3).
 */
class SimuladorConsultaBuroTest {

    private static final String DOCUMENTO_SERVICIO_CAIDO = "0000000000";
    private static final Duration RETARDO_CONFIGURADO = Duration.ofSeconds(7);
    private static final String DOCUMENTO_PAR = "1234567890";
    private static final String DOCUMENTO_IMPAR = "1234567891";

    private final GeneradorInformeCrediticio generador = new GeneradorInformeCrediticio();
    private final PausaAnotada pausa = new PausaAnotada();
    private final SimuladorConsultaBuro simulador =
            new SimuladorConsultaBuro(generador, pausa, DOCUMENTO_SERVICIO_CAIDO, RETARDO_CONFIGURADO);

    @Test
    @DisplayName("El documento reservado para el servicio caido pausa exactamente el retardo configurado")
    void consultarInforme_conElDocumentoDelServicioCaido_pausaElRetardoConfigurado() {
        simulador.consultarInforme(DOCUMENTO_SERVICIO_CAIDO);

        assertThat(pausa.duracionesSolicitadas).containsExactly(RETARDO_CONFIGURADO);
    }

    @Test
    @DisplayName("Tras el retardo, el documento del servicio caido devuelve igualmente su informe")
    void consultarInforme_conElDocumentoDelServicioCaido_devuelveElInformeDelGenerador() {
        InformeCrediticioResponse informe = simulador.consultarInforme(DOCUMENTO_SERVICIO_CAIDO);

        assertThat(informe).isEqualTo(generador.generarInforme(DOCUMENTO_SERVICIO_CAIDO));
    }

    @ParameterizedTest(name = "el documento {0} responde sin retardo")
    @ValueSource(strings = {DOCUMENTO_PAR, DOCUMENTO_IMPAR, "000000000", "00000000000", "0000000002"})
    @DisplayName("Cualquier documento distinto del reservado responde sin pausa alguna")
    void consultarInforme_conUnDocumentoDistintoDelReservado_noPausa(String numeroDocumento) {
        simulador.consultarInforme(numeroDocumento);

        assertThat(pausa.duracionesSolicitadas).isEmpty();
    }

    @Test
    @DisplayName("Un documento par se delega al generador y conserva sus reglas de paridad")
    void consultarInforme_conDocumentoPar_devuelveElInformeSinReporteNegativo() {
        InformeCrediticioResponse informe = simulador.consultarInforme(DOCUMENTO_PAR);

        assertThat(informe.estado()).isEqualTo(EstadoTitular.ACTIVO);
        assertThat(informe.reporteNegativo()).isFalse();
    }

    @Test
    @DisplayName("Un documento impar se delega al generador y conserva sus reglas de paridad")
    void consultarInforme_conDocumentoImpar_devuelveElInformeConReporteNegativo() {
        InformeCrediticioResponse informe = simulador.consultarInforme(DOCUMENTO_IMPAR);

        assertThat(informe.estado()).isEqualTo(EstadoTitular.EN_MORA);
        assertThat(informe.reporteNegativo()).isTrue();
    }

    @Test
    @DisplayName("El simulador no se puede construir sin el documento que dispara la caida")
    void construir_sinDocumentoDelServicioCaido_lanzaNullPointerException() {
        assertThatThrownBy(() -> new SimuladorConsultaBuro(generador, pausa, null, RETARDO_CONFIGURADO))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("documento");
    }

    @Test
    @DisplayName("El simulador no se puede construir sin el retardo del servicio caido")
    void construir_sinRetardoDelServicioCaido_lanzaNullPointerException() {
        assertThatThrownBy(() -> new SimuladorConsultaBuro(generador, pausa, DOCUMENTO_SERVICIO_CAIDO, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("retardo");
    }

    /**
     * Doble de {@link PausaDeSimulacion} que anota las duraciones que le piden en
     * vez de esperarlas.
     */
    private static final class PausaAnotada implements PausaDeSimulacion {

        private final List<Duration> duracionesSolicitadas = new ArrayList<>();

        @Override
        public void pausar(Duration duracion) {
            duracionesSolicitadas.add(duracion);
        }
    }
}
