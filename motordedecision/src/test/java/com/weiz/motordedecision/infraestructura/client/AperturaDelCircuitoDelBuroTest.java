package com.weiz.motordedecision.infraestructura.client;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que el circuit breaker deja de llamar al buro cuando lleva fallando.
 *
 * La configuracion se estrecha respecto a la de produccion —dos llamadas bastan
 * para que el circuito juzgue y no hay reintento— porque de otro modo el test
 * necesitaria decenas de llamadas para demostrar lo mismo.
 *
 * La prueba no mira el estado del registro de Resilience4j sino el efecto
 * observable: despues de la apertura, el servidor simulado deja de recibir
 * peticiones y el cliente sigue devolviendo un resultado, no una excepcion.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "buro.tiempo-espera-conexion=200ms",
        "buro.tiempo-espera-lectura=300ms",
        "resilience4j.retry.instances.buro.max-attempts=1",
        "resilience4j.circuitbreaker.instances.buro.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.buro.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.buro.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.buro.wait-duration-in-open-state=1m",
        "resilience4j.circuitbreaker.instances.buro.automatic-transition-from-open-to-half-open-enabled=false"
})
@DisplayName("Circuit breaker del buro")
class AperturaDelCircuitoDelBuroTest {

    private static final String TIPO_DOCUMENTO = "CC";
    private static final String NUMERO_DOCUMENTO = "1234567890";

    private static final int CODIGO_ERROR_DEL_SERVIDOR = 500;
    private static final String CUERPO_DE_ERROR = "{\"message\":\"Error interno\"}";

    /** Las que fija `minimum-number-of-calls`: con eso el circuito ya puede juzgar. */
    private static final int LLAMADAS_PARA_ABRIR_EL_CIRCUITO = 2;

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

    @Test
    @DisplayName("Con el circuito abierto la llamada ni sale, y el resultado sigue siendo buro no disponible")
    void consultarInforme_conElCircuitoAbierto_devuelveElFallbackSinLlamarAlBuro() {
        SERVIDOR.responderCon(CODIGO_ERROR_DEL_SERVIDOR, CUERPO_DE_ERROR);
        for (int llamada = 0; llamada < LLAMADAS_PARA_ABRIR_EL_CIRCUITO; llamada++) {
            buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);
        }
        SERVIDOR.reiniciarContadorDePeticiones();

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoBuroNoDisponible());
        assertThat(SERVIDOR.contarPeticionesRecibidas()).isZero();
    }

}
