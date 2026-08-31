package com.weiz.motordedecision.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba lo que declara src/main/resources/application.yml sin levantar la
 * aplicacion: solo se resuelve el entorno de cada perfil, asi que ningun test
 * intenta abrir una conexion contra PostgreSQL.
 *
 * El caso importante es {@link #resolverConfiguracion_conElPerfilDev_coincideConElComposeDelModulo}:
 * D-024 duplica a proposito las credenciales entre el compose y el perfil
 * `dev`, y este test es lo que impide que las dos copias se separen.
 */
@DisplayName("Configuracion por perfiles del motor")
class ConfiguracionPerfilesTest {

	private static final String CLAVE_URL = "spring.datasource.url";
	private static final String CLAVE_USUARIO = "spring.datasource.username";
	private static final String CLAVE_CONTRASENA = "spring.datasource.password";

	/** Relativa al directorio del modulo, que es el de trabajo tanto en Maven como en el IDE. */
	private static final Path RUTA_DEL_COMPOSE = Path.of("docker-compose.yml");

	@Test
	@DisplayName("El perfil dev apunta al PostgreSQL que levanta el compose del modulo")
	void resolverConfiguracion_conElPerfilDev_coincideConElComposeDelModulo() throws IOException {
		ParametrosDeLaBaseDeDatos compose = leerParametrosDelCompose();
		String urlEsperada = "jdbc:postgresql://localhost:%s/%s"
				.formatted(compose.puertoEnElHost(), compose.base());

		comprobarConPerfil("dev", entorno -> {
			assertThat(entorno.getProperty(CLAVE_URL)).isEqualTo(urlEsperada);
			assertThat(entorno.getProperty(CLAVE_USUARIO)).isEqualTo(compose.usuario());
			assertThat(entorno.getProperty(CLAVE_CONTRASENA)).isEqualTo(compose.contrasena());
		});
	}

	@Test
	@DisplayName("El perfil docker alcanza la base por el nombre del servicio y su puerto interno")
	void resolverConfiguracion_conElPerfilDocker_apuntaAlServicioDbEnElPuertoInterno() throws IOException {
		String urlEsperada = "jdbc:postgresql://db:5432/%s".formatted(leerParametrosDelCompose().base());

		comprobarConPerfil("docker", entorno -> assertThat(entorno.getProperty(CLAVE_URL))
				.isEqualTo(urlEsperada));
	}

	@Test
	@DisplayName("El motor escucha en el 8080 y conoce la direccion del buro por configuracion")
	void resolverConfiguracion_sinPerfilActivo_declaraElPuertoDelMotorYLaUrlDelBuro() {
		comprobarConPerfil("", entorno -> {
			assertThat(entorno.getProperty("server.port")).isEqualTo("8080");
			assertThat(entorno.getProperty("buro.url")).isEqualTo("http://localhost:8081");
		});
	}

	@Test
	@DisplayName("Hibernate no genera el esquema: lo pone db/sql/01-create_schema.sql")
	void resolverConfiguracion_sinPerfilActivo_dejaHibernateEnValidate() {
		comprobarConPerfil("", entorno -> assertThat(entorno.getProperty("spring.jpa.hibernate.ddl-auto"))
				.isEqualTo("validate"));
	}

	/**
	 * Resuelve application.yml para el perfil indicado y entrega el entorno ya
	 * poblado. No se registra ninguna autoconfiguracion, de modo que no llega a
	 * crearse un origen de datos ni a abrirse una conexion.
	 *
	 * @param perfil perfil a activar, o cadena vacia para el documento por defecto
	 * @param comprobacion aserciones sobre el entorno resultante
	 */
	private void comprobarConPerfil(String perfil, Consumer<Environment> comprobacion) {
		new ApplicationContextRunner()
				.withInitializer(new ConfigDataApplicationContextInitializer())
				.withPropertyValues("spring.profiles.active=" + perfil)
				.run(contexto -> comprobacion.accept(contexto.getEnvironment()));
	}

	private ParametrosDeLaBaseDeDatos leerParametrosDelCompose() throws IOException {
		assertThat(RUTA_DEL_COMPOSE)
				.as("El test se ejecuta desde el directorio del modulo motordedecision")
				.exists();

		try (InputStream flujo = Files.newInputStream(RUTA_DEL_COMPOSE)) {
			Map<String, Object> servicioDb = obtenerMapa(obtenerMapa(new Yaml().load(flujo), "services"), "db");
			Map<String, Object> variables = obtenerMapa(servicioDb, "environment");

			return new ParametrosDeLaBaseDeDatos(
					String.valueOf(variables.get("POSTGRES_DB")),
					String.valueOf(variables.get("POSTGRES_USER")),
					String.valueOf(variables.get("POSTGRES_PASSWORD")),
					extraerPuertoDelHost(servicioDb));
		}
	}

	/** El compose publica los puertos como "host:contenedor"; interesa el primero. */
	private String extraerPuertoDelHost(Map<String, Object> servicioDb) {
		List<?> publicaciones = (List<?>) servicioDb.get("ports");
		return String.valueOf(publicaciones.get(0)).split(":")[0];
	}

	// SnakeYAML devuelve los mapas anidados sin genericos; la forma del compose la
	// fija el propio archivo del modulo, que se versiona junto a este test.
	@SuppressWarnings("unchecked")
	private Map<String, Object> obtenerMapa(Map<String, Object> contenedor, String clave) {
		return (Map<String, Object>) contenedor.get(clave);
	}

	private record ParametrosDeLaBaseDeDatos(String base, String usuario, String contrasena,
											 String puertoEnElHost) {
	}

}
