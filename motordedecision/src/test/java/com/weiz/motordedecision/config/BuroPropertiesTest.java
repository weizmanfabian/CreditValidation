package com.weiz.motordedecision.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija que la validacion declarada en {@link BuroProperties} dispara de verdad.
 *
 * Sin este test, {@code @Validated} y {@code @NotBlank} serian una afirmacion no
 * comprobada: el motor arrancaria con una direccion de buro vacia y el fallo
 * aparecerian mas tarde, en la primera consulta, como un error de transporte
 * incomprensible en vez de como un error de configuracion al arrancar.
 *
 * Las aserciones no miran el texto del mensaje, que depende del idioma de la
 * maquina, sino el codigo de error que genera Bean Validation.
 */
@DisplayName("Validacion de la configuracion del buro")
class BuroPropertiesTest {

	private static final String URL_VALIDA = "http://localhost:8081";

	/** Codigo que Bean Validation asigna al @NotBlank incumplido de `buro.url`. */
	private static final String CODIGO_URL_EN_BLANCO = "NotBlank.buro.url";

	private final ApplicationContextRunner contexto = new ApplicationContextRunner()
			.withUserConfiguration(ConfiguracionDePrueba.class);

	@ParameterizedTest(name = "buro.url = \"{0}\"")
	@ValueSource(strings = {"", "   "})
	@DisplayName("Una direccion en blanco tumba el arranque en vez de dejar el motor mal configurado")
	void enlazar_conUrlEnBlanco_impideElArranqueDelContexto(String urlEnBlanco) {
		contexto.withPropertyValues("buro.url=" + urlEnBlanco)
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
		contexto.run(resultado -> assertThat(resultado)
				.hasFailed()
				.getFailure()
				.rootCause()
				.isInstanceOf(BindValidationException.class)
				.hasMessageContaining(CODIGO_URL_EN_BLANCO));
	}

	@Test
	@DisplayName("Con una direccion valida el contexto arranca y la propiedad queda enlazada")
	void enlazar_conUrlValida_dejaArrancarElContextoConElValorLeido() {
		contexto.withPropertyValues("buro.url=" + URL_VALIDA)
				.run(resultado -> assertThat(resultado)
						.hasNotFailed()
						.getBean(BuroProperties.class)
						.extracting(BuroProperties::url)
						.isEqualTo(URL_VALIDA));
	}

	@EnableConfigurationProperties(BuroProperties.class)
	static class ConfiguracionDePrueba {
	}

}
