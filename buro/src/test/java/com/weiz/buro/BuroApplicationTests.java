package com.weiz.buro;

import jakarta.validation.Validator;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BuroApplicationTests {

	@Autowired
	private ApplicationContext contexto;

	@Autowired
	private Environment entorno;

	@Test
	@DisplayName("El contexto arranca sin datasource: el buro no persiste nada")
	void cargarContexto_sinJpaNiDriverDeBaseDeDatos_noRegistraDataSource() {
		String[] datasources = contexto.getBeanNamesForType(DataSource.class);

		assertThat(datasources).isEmpty();
	}

	@Test
	@DisplayName("La validacion de bean queda disponible en el contexto")
	void cargarContexto_conStarterDeValidacion_registraElValidador() {
		String[] validadores = contexto.getBeanNamesForType(Validator.class);

		assertThat(validadores).isNotEmpty();
	}

	@Test
	@DisplayName("La configuracion expone el puerto 8081 y el nombre de la aplicacion")
	void cargarContexto_conApplicationYml_exponePuertoPropioYNombre() {
		assertThat(entorno.getProperty("server.port")).isEqualTo("8081");
		assertThat(entorno.getProperty("spring.application.name")).isEqualTo("buro");
	}

}
