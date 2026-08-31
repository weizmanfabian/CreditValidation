package com.weiz.buro.domain.informe;

import com.weiz.buro.api.models.response.InformeCrediticioResponse;
import com.weiz.buro.util.enums.EstadoTitular;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba las reglas del mock del enunciado en {@link GeneradorInformeCrediticio}:
 * rango del score y reporte negativo segun la paridad del documento, la
 * correspondencia con {@link EstadoTitular} (D-011) y el determinismo.
 *
 * Es JUnit puro: la clase de dominio no depende de Spring, asi que no hay
 * contexto que levantar.
 */
class GeneradorInformeCrediticioTest {

    private static final int SCORE_MINIMO_PAR = 600;
    private static final int SCORE_MAXIMO_PAR = 850;
    private static final int SCORE_MINIMO_IMPAR = 300;
    private static final int SCORE_MAXIMO_IMPAR = 550;

    private static final String DOCUMENTO_PAR = "1234567890";
    private static final String DOCUMENTO_IMPAR = "1234567891";
    private static final int SCORE_ESPERADO_DOCUMENTO_PAR = 635;
    private static final int SCORE_ESPERADO_DOCUMENTO_IMPAR = 381;

    private static final int DOCUMENTOS_DEL_BARRIDO = 10_000;

    private final GeneradorInformeCrediticio generador = new GeneradorInformeCrediticio();

    @ParameterizedTest(name = "documento {0} termina en par: score en [600,850] sin reporte negativo")
    @ValueSource(strings = {"1234567890", "1000000002", "9876543214", "0000000006", "123458"})
    @DisplayName("Un documento terminado en par produce score en [600,850], estado ACTIVO y sin reporte negativo")
    void generarInforme_conDocumentoTerminadoEnPar_devuelveScoreAltoSinReporteNegativo(String numeroDocumento) {
        InformeCrediticioResponse informe = generador.generarInforme(numeroDocumento);

        assertThat(informe.score()).isBetween(SCORE_MINIMO_PAR, SCORE_MAXIMO_PAR);
        assertThat(informe.estado()).isEqualTo(EstadoTitular.ACTIVO);
        assertThat(informe.reporteNegativo()).isFalse();
    }

    @ParameterizedTest(name = "documento {0} termina en impar: score en [300,550] con reporte negativo")
    @ValueSource(strings = {"1234567891", "1000000003", "9876543215", "0000000007", "123459"})
    @DisplayName("Un documento terminado en impar produce score en [300,550], estado EN_MORA y con reporte negativo")
    void generarInforme_conDocumentoTerminadoEnImpar_devuelveScoreBajoConReporteNegativo(String numeroDocumento) {
        InformeCrediticioResponse informe = generador.generarInforme(numeroDocumento);

        assertThat(informe.score()).isBetween(SCORE_MINIMO_IMPAR, SCORE_MAXIMO_IMPAR);
        assertThat(informe.estado()).isEqualTo(EstadoTitular.EN_MORA);
        assertThat(informe.reporteNegativo()).isTrue();
    }

    @ParameterizedTest(name = "documento {0} produce exactamente el extremo {1} del rango")
    @CsvSource({
            "1000000706, 600",
            "1000001554, 850",
            "1000001867, 300",
            "1000000529, 550"
    })
    @DisplayName("Los cuatro extremos de los dos rangos son alcanzables y el generador los produce exactos")
    void generarInforme_conDocumentosEnLosExtremos_devuelveElLimiteExacto(String numeroDocumento, int scoreEsperado) {
        InformeCrediticioResponse informe = generador.generarInforme(numeroDocumento);

        assertThat(informe.score()).isEqualTo(scoreEsperado);
    }

    @ParameterizedTest(name = "documento {0} devuelve siempre el score {1}")
    @CsvSource({
            "1234567890, 635",
            "1234567891, 381"
    })
    @DisplayName("El mismo documento devuelve siempre el mismo score, invocacion tras invocacion")
    void generarInforme_conElMismoDocumentoDosVeces_devuelveElMismoScore(String numeroDocumento, int scoreEsperado) {
        InformeCrediticioResponse primerInforme = generador.generarInforme(numeroDocumento);
        InformeCrediticioResponse segundoInforme = generador.generarInforme(numeroDocumento);

        assertThat(primerInforme.score())
                .isEqualTo(segundoInforme.score())
                .isEqualTo(scoreEsperado);
        assertThat(primerInforme.estado()).isEqualTo(segundoInforme.estado());
        assertThat(primerInforme.reporteNegativo()).isEqualTo(segundoInforme.reporteNegativo());
    }

    @Test
    @DisplayName("El score de un documento no depende de que se hayan consultado otros documentos antes")
    void generarInforme_trasConsultarOtrosDocumentos_devuelveElMismoScoreDeSiempre() {
        generador.generarInforme("0000000000");
        generador.generarInforme(DOCUMENTO_IMPAR);
        generador.generarInforme("9999999999");

        InformeCrediticioResponse informe = generador.generarInforme(DOCUMENTO_PAR);

        assertThat(informe.score()).isEqualTo(SCORE_ESPERADO_DOCUMENTO_PAR);
    }

    @Test
    @DisplayName("Una instancia recien creada del generador produce el mismo score que otra ya usada")
    void generarInforme_conOtraInstanciaDelGenerador_devuelveElMismoScore() {
        InformeCrediticioResponse informeDeOtraInstancia =
                new GeneradorInformeCrediticio().generarInforme(DOCUMENTO_IMPAR);

        InformeCrediticioResponse informe = generador.generarInforme(DOCUMENTO_IMPAR);

        assertThat(informe.score())
                .isEqualTo(informeDeOtraInstancia.score())
                .isEqualTo(SCORE_ESPERADO_DOCUMENTO_IMPAR);
    }

    @Test
    @DisplayName("Ningun documento valido produce un score fuera del rango que le corresponde")
    void generarInforme_conDiezMilDocumentos_nuncaDevuelveUnScoreFueraDeRango() {
        for (int numero = 0; numero < DOCUMENTOS_DEL_BARRIDO; numero++) {
            String numeroDocumento = String.format("%010d", numero);
            boolean esDocumentoPar = numero % 2 == 0;

            InformeCrediticioResponse informe = generador.generarInforme(numeroDocumento);

            int minimoEsperado = esDocumentoPar ? SCORE_MINIMO_PAR : SCORE_MINIMO_IMPAR;
            int maximoEsperado = esDocumentoPar ? SCORE_MAXIMO_PAR : SCORE_MAXIMO_IMPAR;
            assertThat(informe.score())
                    .as("score del documento %s", numeroDocumento)
                    .isBetween(minimoEsperado, maximoEsperado);
            assertThat(informe.reporteNegativo())
                    .as("reporte negativo del documento %s", numeroDocumento)
                    .isEqualTo(!esDocumentoPar);
        }
    }

    @Test
    @DisplayName("La fecha de consulta se sella con el reloj recibido, no con el de la maquina")
    void generarInforme_conRelojFijo_sellaLaFechaDeConsultaDeEseReloj() {
        LocalDateTime instanteFijo = LocalDateTime.of(2026, 8, 24, 10, 30, 0);
        Clock relojFijo = Clock.fixed(instanteFijo.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        InformeCrediticioResponse informe =
                new GeneradorInformeCrediticio(relojFijo).generarInforme(DOCUMENTO_PAR);

        assertThat(informe.fechaConsulta()).isEqualTo(instanteFijo);
    }

    @ParameterizedTest(name = "el documento {0} se rechaza por no ser una cadena de digitos")
    @CsvSource(nullValues = "NULO", value = {"NULO", "''", "12345A789", "1234 56789", "123456A"})
    @DisplayName("Un numero de documento que no es una cadena de digitos rompe el contrato del generador")
    void generarInforme_conNumeroDocumentoInvalido_lanzaIllegalArgumentException(String numeroDocumento) {
        assertThatThrownBy(() -> generador.generarInforme(numeroDocumento))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digitos");
    }
}
