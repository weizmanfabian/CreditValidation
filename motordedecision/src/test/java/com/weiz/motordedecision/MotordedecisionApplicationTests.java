package com.weiz.motordedecision;

import com.weiz.motordedecision.config.BuroProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Arranque del motor de decision")
class MotordedecisionApplicationTests {

	private static final String URL_ESPERADA_DEL_BURO = "http://localhost:8081";

	@Autowired
	private DataSource origenDeDatos;

	@Autowired
	private BuroProperties buroProperties;

	@Test
	@DisplayName("El contexto levanta contra H2 en memoria: no hace falta PostgreSQL ni Docker")
	void cargarContexto_conElPerfilDeTest_conectaContraH2EnMemoria() throws SQLException {
		try (Connection conexion = origenDeDatos.getConnection()) {
			assertThat(conexion.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
		}
	}

	@Test
	@DisplayName("La direccion del buro se enlaza desde la configuracion, no desde el codigo")
	void cargarContexto_alArrancar_enlazaLaDireccionDelBuroDesdeLaConfiguracion() {
		assertThat(buroProperties.url()).isEqualTo(URL_ESPERADA_DEL_BURO);
	}

}
