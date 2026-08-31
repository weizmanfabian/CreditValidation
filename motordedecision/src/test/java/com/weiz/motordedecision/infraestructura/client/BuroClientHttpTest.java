package com.weiz.motordedecision.infraestructura.client;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el cliente del buro contra un servidor HTTP real
 * ({@link ServidorBuroSimulado}) en los tres caminos que exige la feature:
 * respuesta correcta, error 5xx y buro que tarda mas de la cuenta.
 *
 * El circuito se deja fuera de juego a proposito —hacen falta muchas mas
 * llamadas de las que hace esta clase para que juzgue— porque aqui se mide el
 * reintento y el fallback. La apertura del circuito se prueba aparte, en
 * {@link AperturaDelCircuitoDelBuroTest}, que necesita otra configuracion.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "buro.tiempo-espera-conexion=200ms",
        "buro.tiempo-espera-lectura=300ms",
        "resilience4j.retry.instances.buro.wait-duration=20ms",
        "resilience4j.circuitbreaker.instances.buro.minimum-number-of-calls=1000"
})
@DisplayName("Cliente HTTP del buro")
class BuroClientHttpTest {

    private static final String TIPO_DOCUMENTO = "CC";
    private static final String NUMERO_DOCUMENTO = "1234567890";

    /** Debe coincidir con `resilience4j.retry.instances.buro.max-attempts`. */
    private static final int INTENTOS_CONFIGURADOS = 3;

    private static final int CODIGO_ERROR_DEL_SERVIDOR = 500;
    private static final String CUERPO_DE_ERROR = "{\"message\":\"Error interno\"}";

    private static final int SCORE_ESPERADO = 750;
    private static final String ESTADO_ESPERADO = "ACTIVO";
    private static final LocalDateTime FECHA_ESPERADA = LocalDateTime.of(2026, 8, 31, 10, 15, 30);
    private static final String INFORME_JSON = """
            {"score":750,"estado":"ACTIVO","reporteNegativo":false,"fechaConsulta":"2026-08-31 10:15:30"}""";

    /** Mas del tiempo de espera de lectura, para que la peticion no llegue a completarse. */
    private static final Duration RETARDO_MAYOR_QUE_LA_ESPERA = Duration.ofMillis(900);

    private static final ServidorBuroSimulado SERVIDOR = ServidorBuroSimulado.iniciar();

    @Autowired
    private BuroClient buroClient;

    @DynamicPropertySource
    static void registrarLaDireccionDelBuroSimulado(DynamicPropertyRegistry registro) {
        registro.add("buro.url", SERVIDOR::obtenerUrl);
    }

    @AfterAll
    static void detenerElServidorSimulado() {
        SERVIDOR.detener();
    }

    @BeforeEach
    void reiniciarElContadorDePeticiones() {
        SERVIDOR.reiniciarContadorDePeticiones();
    }

    @Test
    @DisplayName("Con 200 del buro devuelve el informe mapeado a dominio, sin reintentar")
    void consultarInforme_cuandoElBuroResponde200_devuelveElInformeMapeado() {
        SERVIDOR.responderCon(200, INFORME_JSON);

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoConInforme(
                SCORE_ESPERADO, ESTADO_ESPERADO, false, FECHA_ESPERADA));
        assertThat(SERVIDOR.contarPeticionesRecibidas()).isOne();
    }

    @Test
    @DisplayName("Con 500 del buro reintenta hasta el maximo y cae en el fallback, sin lanzar")
    void consultarInforme_cuandoElBuroDevuelve500_reintentaYCaeEnElFallback() {
        SERVIDOR.responderCon(CODIGO_ERROR_DEL_SERVIDOR, CUERPO_DE_ERROR);

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoBuroNoDisponible());
        assertThat(SERVIDOR.contarPeticionesRecibidas()).isEqualTo(INTENTOS_CONFIGURADOS);
    }

    @Test
    @DisplayName("Con un buro que tarda mas que la espera de lectura, el resultado es buro no disponible")
    void consultarInforme_cuandoElBuroTardaMasQueLaEsperaDeLectura_caeEnElFallback() {
        SERVIDOR.responderConRetardo(RETARDO_MAYOR_QUE_LA_ESPERA, INFORME_JSON);

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoBuroNoDisponible());
        assertThat(SERVIDOR.contarPeticionesRecibidas()).isEqualTo(INTENTOS_CONFIGURADOS);
    }

    @Test
    @DisplayName("Un 200 con el cuerpo vacio se trata como buro caido, no devuelve un resultado nulo")
    void consultarInforme_cuandoElBuroResponde200SinCuerpo_caeEnElFallback() {
        SERVIDOR.responderCon(200, "");

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoBuroNoDisponible());
    }

}
