package com.weiz.motordedecision.infraestructura.client;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demuestra que el tiempo de espera de conexion de
 * {@code ConfiguracionClienteBuro} se aplica de verdad, y no solo se enlaza y
 * se valida (progress/review_motor.md, menor diferido nº 2 de la feature 9).
 *
 * El buro se apunta a 10.255.255.1, una direccion privada que no esta enrutada
 * en ninguna maquina de desarrollo: la conexion TCP no se completa nunca. La
 * espera de LECTURA se deja larga a proposito, asi que lo unico que puede
 * cortar la llamada en menos de un segundo es la espera de CONEXION. Sin ella,
 * el sistema operativo tardaria decenas de segundos en rendirse y el margen de
 * este test se pasaria de largo (docs/decisions.md D-071).
 *
 * El reloj solo se usa para ese margen, que es lo que la feature mide.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "buro.url=http://10.255.255.1:8081",
        "buro.tiempo-espera-conexion=200ms",
        "buro.tiempo-espera-lectura=30s",
        "resilience4j.retry.instances.buro.wait-duration=20ms",
        // El circuito se deja fuera de juego; las dos propiedades van juntas por
        // el recorte de la ventana COUNT_BASED (docs/decisions.md D-072).
        "resilience4j.circuitbreaker.instances.buro.sliding-window-size=1000",
        "resilience4j.circuitbreaker.instances.buro.minimum-number-of-calls=1000"
})
@DisplayName("Tiempo de espera de conexion al buro")
class TiempoDeEsperaDeConexionAlBuroTest {

    private static final String TIPO_DOCUMENTO = "CC";
    private static final String NUMERO_DOCUMENTO = "1234567890";

    /**
     * Holgado frente a los tres intentos de 200 ms que deberian bastar, y muy
     * por debajo de lo que tarda el sistema operativo en abandonar una conexion
     * por su cuenta: entre eso y esto esta la diferencia que el test mide.
     */
    private static final Duration MARGEN_MAXIMO = Duration.ofSeconds(5);

    @Autowired
    private BuroClient buroClient;

    @Test
    @DisplayName("Con el buro en una direccion no enrutable el resultado es buro no disponible, y pronto")
    void consultarInforme_conElBuroInalcanzable_caeEnElFallbackDentroDelMargen() {
        long inicioEnNanosegundos = System.nanoTime();

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        Duration transcurrido = Duration.ofNanos(System.nanoTime() - inicioEnNanosegundos);
        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoBuroNoDisponible());
        assertThat(transcurrido)
                .as("sin la espera de conexion aplicada, la llamada tardaria decenas de segundos")
                .isLessThan(MARGEN_MAXIMO);
    }
}
