package com.weiz.motordedecision.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija que la validacion declarada en {@link BuroProperties} dispara de verdad.
 *
 * Sin este test, {@code @Validated}, {@code @NotBlank} y {@code @NotNull} serian
 * afirmaciones no comprobadas: el motor arrancaria con una direccion de buro
 * vacia o sin tiempos de espera, y el fallo aparecerian mas tarde, en la primera
 * consulta, como un error de transporte incomprensible —o como una espera
 * indefinida— en vez de como un error de configuracion al arrancar.
 *
 * Las aserciones no miran el texto del mensaje, que depende del idioma de la
 * maquina, sino el codigo de error que genera Bean Validation.
 */
@DisplayName("Validacion de la configuracion del buro")
class BuroPropertiesTest {

	private static final String URL_VALIDA = "http://localhost:8081";
	private static final String ESPERA_CONEXION_VALIDA = "buro.tiempo-espera-conexion=2s";
	private static final String ESPERA_LECTURA_VALIDA = "buro.tiempo-espera-lectura=3s";

	/** Codigo que Bean Validation asigna al @NotBlank incumplido de `buro.url`. */
	private static final String CODIGO_URL_EN_BLANCO = "NotBlank.buro.url";

	/** Codigo del @NotNull incumplido de la espera de lectura. */
	private static final String CODIGO_ESPERA_LECTURA_AUSENTE = "NotNull.buro.tiempoEsperaLectura";

	private final ApplicationContextRunner contexto = new ApplicationContextRunner()
			.withUserConfiguration(ConfiguracionDePrueba.class);

	@ParameterizedTest(name = "buro.url = \"{0}\"")
	@ValueSource(strings = {"", "   "})
	@DisplayName("Una direccion en blanco tumba el arranque en vez de dejar el motor mal configurado")
	void enlazar_conUrlEnBlanco_impideElArranqueDelContexto(String urlEnBlanco) {
		contexto.withPropertyValues("buro.url=" + urlEnBlanco, ESPERA_CONEXION_VALIDA, ESPERA_LECTURA_VALIDA)
				.run(resultado -> assertThat(resultado)
						.hasFailed()
						.getFailure()
						.rootCause()
						.isInstanceOf(BindValidationException.class)
						.hasMessageContaining(CODIGO_URL_EN_BLANCO));
	}

	@Test
	@DisplayName("Sin la propiedad declarada, el arranque tampoco sigue adelante")
	void enlazar_sinLaPropiedadDeclarada_impideElArranqueDelContexto() {
		contexto.withPropertyValues(ESPERA_CONEXION_VALIDA, ESPERA_LECTURA_VALIDA)
				.run(resultado -> assertThat(resultado)
						.hasFailed()
						.getFailure()
						.rootCause()
						.isInstanceOf(BindValidationException.class)
						.hasMessageContaining(CODIGO_URL_EN_BLANCO));
	}

	@Test
	@DisplayName("Sin tiempo de espera de lectura el motor no arranca: esperar sin limite no es una opcion")
	void enlazar_sinTiempoDeEsperaDeLectura_impideElArranqueDelContexto() {
		contexto.withPropertyValues("buro.url=" + URL_VALIDA, ESPERA_CONEXION_VALIDA)
				.run(resultado -> assertThat(resultado)
						.hasFailed()
						.getFailure()
						.rootCause()
						.isInstanceOf(BindValidationException.class)
						.hasMessageContaining(CODIGO_ESPERA_LECTURA_AUSENTE));
	}

	@Test
	@DisplayName("Con la configuracion completa el contexto arranca y los tres valores quedan enlazados")
	void enlazar_conLaConfiguracionCompleta_dejaArrancarElContextoConLosValoresLeidos() {
		contexto.withPropertyValues("buro.url=" + URL_VALIDA, ESPERA_CONEXION_VALIDA, ESPERA_LECTURA_VALIDA)
				.run(resultado -> assertThat(resultado)
						.hasNotFailed()
						.getBean(BuroProperties.class)
						.isEqualTo(new BuroProperties(URL_VALIDA, Duration.ofSeconds(2), Duration.ofSeconds(3))));
	}

	@EnableConfigurationProperties(BuroProperties.class)
	static class ConfiguracionDePrueba {
	}

}
