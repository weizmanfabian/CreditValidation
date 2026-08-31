package com.weiz.motordedecision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de arranque del motor de decision.
 *
 * Requiere un perfil activo que aporte el origen de datos: `dev` contra el
 * PostgreSQL de motordedecision/docker-compose.yml, o `docker` cuando la
 * aplicacion corre dentro del compose de la raiz.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MotordedecisionApplication {

	public static void main(String[] args) {
		SpringApplication.run(MotordedecisionApplication.class, args);
	}

}
