package com.weiz.motordedecision.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba que Resilience4j 2.3.0 arranca de verdad sobre Spring Boot 4.1.1.
 *
 * El artefacto publicado se llama `resilience4j-spring-boot3` y no existe uno
 * para Spring Boot 4, asi que la compatibilidad no se puede dar por supuesta:
 * se demuestra aqui. La configuracion real del buro -- ventanas, umbrales,
 * tiempos -- es de la feature 9; este test solo fija que el arranque funciona,
 * con una instancia de prueba.
 */
@SpringBootTest
@Import(ArranqueDeResilienciaTest.ConfiguracionDePrueba.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"resilience4j.retry.instances.prueba.max-attempts=3",
		"resilience4j.retry.instances.prueba.wait-duration=10ms",
		"resilience4j.retry.instances.prueba.enable-exponential-backoff=true",
		"resilience4j.retry.instances.prueba.enable-randomized-wait=true",
		"resilience4j.circuitbreaker.instances.prueba.sliding-window-size=10"
})
@DisplayName("Arranque de la resiliencia")
class ArranqueDeResilienciaTest {

	private static final String INSTANCIA_DE_PRUEBA = "prueba";
	private static final int INTENTOS_CONFIGURADOS = 3;

	@Autowired
	private RetryRegistry registroDeReintentos;

	@Autowired
	private CircuitBreakerRegistry registroDeCircuitBreakers;

	@Autowired
	private ServicioQueSiempreFalla servicioQueSiempreFalla;

	@Test
	@DisplayName("Los registros se autoconfiguran y leen las instancias declaradas en la configuracion")
	void cargarContexto_conResilience4jEnElClasspath_autoconfiguraLosRegistros() {
		assertThat(registroDeReintentos.retry(INSTANCIA_DE_PRUEBA).getRetryConfig().getMaxAttempts())
				.isEqualTo(INTENTOS_CONFIGURADOS);
		assertThat(registroDeCircuitBreakers.circuitBreaker(INSTANCIA_DE_PRUEBA)).isNotNull();
	}

	@Test
	@DisplayName("La anotacion @Retry reintenta de verdad: no basta con que el bean exista")
	void consultar_cuandoLaLlamadaFalla_reintentaHastaElMaximoConfigurado() {
		assertThatThrownBy(() -> servicioQueSiempreFalla.consultar())
				.isInstanceOf(IllegalStateException.class);

		assertThat(servicioQueSiempreFalla.contarIntentos()).isEqualTo(INTENTOS_CONFIGURADOS);
	}

	/**
	 * Doble de prueba: existe solo para que el aspecto de Resilience4j tenga algo
	 * que envolver. No es una pieza del motor.
	 */
	static class ServicioQueSiempreFalla {

		private final AtomicInteger intentos = new AtomicInteger();

		@Retry(name = INSTANCIA_DE_PRUEBA)
		String consultar() {
			intentos.incrementAndGet();
			throw new IllegalStateException("El servicio simulado siempre falla");
		}

		/**
		 * El bean llega envuelto en un proxy, asi que el contador se consulta por
		 * metodo: leer el campo directamente devolveria el del proxy, que es nulo.
		 */
		int contarIntentos() {
			return intentos.get();
		}
	}

	@TestConfiguration
	static class ConfiguracionDePrueba {

		@Bean
		ServicioQueSiempreFalla servicioQueSiempreFalla() {
			return new ServicioQueSiempreFalla();
		}
	}

}
