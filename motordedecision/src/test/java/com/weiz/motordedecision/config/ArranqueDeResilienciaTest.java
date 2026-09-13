package com.weiz.motordedecision.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.retry.RetryConfig;
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

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba que Resilience4j 2.3.0 arranca de verdad sobre Spring Boot 4.1.1 y
 * que la instancia `buro` de application.yml llega al registro tal como esta
 * escrita.
 *
 * El artefacto publicado se llama `resilience4j-spring-boot3` y no existe uno
 * para Spring Boot 4, asi que la compatibilidad no se puede dar por supuesta:
 * se demuestra aqui, con una instancia de prueba.
 *
 * Lo segundo lo pide la feature 23: el backoff exponencial con jitter que
 * docs/conventions.md §7 declara obligatorio era hasta ahora configuracion que
 * ninguna prueba protegia, y apagarlo dejaba la suite entera en verde
 * (progress/review_motor.md). Se afirma calculando la espera, no midiendola con
 * el reloj (docs/decisions.md D-070).
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

	/** La instancia real del motor, declarada en src/main/resources/application.yml. */
	private static final String INSTANCIA_DEL_BURO = "buro";

	/** Lo que dice application.yml: `max-attempts: 3`. */
	private static final int INTENTOS_DEL_BURO = 3;

	private static final int PRIMER_INTENTO = 1;

	/**
	 * Ventana del primer intento con `wait-duration: 300ms` y
	 * `randomized-wait-factor: 0.5`: la espera cae al azar entre la mitad y el
	 * doble y medio de la base, nunca en un punto fijo.
	 */
	private static final long ESPERA_MINIMA_DEL_PRIMER_INTENTO = 150L;
	private static final long ESPERA_MAXIMA_DEL_PRIMER_INTENTO = 450L;

	/**
	 * Suficientes para que dos esperas iguales por azar sean imposibles en la
	 * practica: hay 301 valores posibles en la ventana del primer intento.
	 */
	private static final int MUESTRAS_DE_LA_ESPERA = 30;

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

	@Test
	@DisplayName("El reintento del buro trae del archivo los intentos y la ventana de espera declarados")
	void obtenerLaConfiguracionDelBuro_desdeElRegistro_traeLosIntentosYLaVentanaDeclarados() {
		RetryConfig configuracionDelBuro = obtenerLaConfiguracionDeReintentoDelBuro();

		Set<Long> esperas = muestrearLaEsperaDelPrimerIntento(configuracionDelBuro);

		assertThat(configuracionDelBuro.getMaxAttempts()).isEqualTo(INTENTOS_DEL_BURO);
		assertThat(esperas).allSatisfy(espera -> assertThat(espera)
				.isBetween(ESPERA_MINIMA_DEL_PRIMER_INTENTO, ESPERA_MAXIMA_DEL_PRIMER_INTENTO));
	}

	@Test
	@DisplayName("El reintento del buro lleva jitter: dos esperas del mismo intento no coinciden")
	void calcularLaEsperaDelBuro_conElJitterDeclarado_devuelveEsperasDistintas() {
		RetryConfig configuracionDelBuro = obtenerLaConfiguracionDeReintentoDelBuro();

		Set<Long> esperas = muestrearLaEsperaDelPrimerIntento(configuracionDelBuro);

		assertThat(esperas)
				.as("con enable-randomized-wait en false las %d muestras serian identicas",
						MUESTRAS_DE_LA_ESPERA)
				.hasSizeGreaterThan(1);
	}

	/**
	 * Se usa {@code find} y no {@code retry} porque el segundo crea la instancia
	 * con la configuracion por omision cuando no existe: el test pasaria aunque
	 * alguien renombrara la instancia en application.yml.
	 */
	private RetryConfig obtenerLaConfiguracionDeReintentoDelBuro() {
		return registroDeReintentos.find(INSTANCIA_DEL_BURO)
				.orElseThrow(() -> new AssertionError(
						"application.yml no declara la instancia de reintento " + INSTANCIA_DEL_BURO))
				.getRetryConfig();
	}

	/**
	 * Calcula muchas veces la espera del primer intento. La funcion de intervalo
	 * es la que Resilience4j usara entre reintento y reintento, asi que consultarla
	 * dice lo mismo que cronometrar los reintentos, sin depender del reloj.
	 */
	private Set<Long> muestrearLaEsperaDelPrimerIntento(RetryConfig configuracion) {
		IntervalBiFunction<Object> calculoDeLaEspera = configuracion.getIntervalBiFunction();

		// El segundo argumento es el resultado o la excepcion del intento anterior;
		// la funcion configurada aqui solo mira el numero de intento.
		return IntStream.range(0, MUESTRAS_DE_LA_ESPERA)
				.mapToObj(muestra -> calculoDeLaEspera.apply(PRIMER_INTENTO, Either.right(null)))
				.collect(Collectors.toSet());
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
