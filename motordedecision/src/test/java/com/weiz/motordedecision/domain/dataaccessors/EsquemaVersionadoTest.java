package com.weiz.motordedecision.domain.dataaccessors;

import com.weiz.motordedecision.domain.entities.ResultadoValidacion;
import com.weiz.motordedecision.domain.entities.Solicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cierra el hueco que dejo abierto D-027: el resto de la suite corre sobre un
 * esquema que genera Hibernate en H2, asi que ningun test se enteraria de que
 * las entidades y {@code db/sql/01-create_schema.sql} se separan.
 *
 * Aqui pasa lo contrario. El esquema lo pone el script versionado —el mismo
 * archivo que ejecuta el contenedor de PostgreSQL en desarrollo, copiado al
 * classpath de pruebas por el {@code testResource} del pom— e Hibernate arranca
 * con {@code ddl-auto: validate}: si una entidad declara una columna, un tipo o
 * un nombre que el script no tiene, el contexto no levanta y el test se pone en
 * rojo.
 *
 * En una maquina sin Docker se SALTA, no falla
 * ({@code @Testcontainers(disabledWithoutDocker = true)},
 * {@code docs/verification.md} §3).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SolicitudDataAccessor.class, GeneradorIdSolicitud.class})
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Las entidades cuadran con el esquema versionado")
class EsquemaVersionadoTest {

    /** La misma imagen que levanta motordedecision/docker-compose.yml. */
    private static final String IMAGEN_DE_POSTGRES = "postgres:17-alpine";

    /** Ruta en el classpath de pruebas; el original vive en db/sql del modulo. */
    private static final String SCRIPT_DEL_ESQUEMA = "db/sql/01-create_schema.sql";

    private static final String ESTADO_APROBADO = "APROBADO";
    private static final String RESULTADO_APROBADO = "APROBADO";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(IMAGEN_DE_POSTGRES).withInitScript(SCRIPT_DEL_ESQUEMA);

    @DynamicPropertySource
    static void registrarOrigenDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        // Sin generar nada: Hibernate solo comprueba que el mapeo cuadre con lo
        // que dejo el script. Es toda la guarda de este test.
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private SolicitudDataAccessor solicitudDataAccessor;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("El esquema bajo prueba es el del script, no uno generado por Hibernate")
    void validarEsquema_alLevantarElContexto_usaElEsquemaDelScriptVersionado() {
        Number restriccionesConNombreDelScript = (Number) entityManager.getEntityManager()
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM information_schema.table_constraints
                        WHERE constraint_name IN ('ck_solicitud_estado', 'uq_resultado_orden')
                        """)
                .getSingleResult();

        assertThat(restriccionesConNombreDelScript.intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("Una solicitud con su cadena de validaciones va y vuelve de PostgreSQL real")
    void guardarSolicitud_contraElEsquemaVersionado_persisteYRecuperaLaSolicitudConSuCadena() {
        Solicitud solicitud = crearSolicitudBase();
        solicitud.agregarResultado(crearResultado((short) 1, "Identidad", RESULTADO_APROBADO, "Documento no bloqueado"));
        solicitud.agregarResultado(crearResultado((short) 2, "Score", RESULTADO_APROBADO, "Score 750 >= 700"));

        Long id = solicitudDataAccessor.guardarSolicitud(solicitud).getId();

        entityManager.flush();
        entityManager.clear();
        Solicitud recuperada = entityManager.find(Solicitud.class, id);
        assertThat(recuperada.getIdSolicitud()).isEqualTo("SOL-20260830-001");
        assertThat(recuperada.getResultados())
                .extracting(ResultadoValidacion::getNombre)
                .containsExactly("Identidad", "Score");
    }

    private Solicitud crearSolicitudBase() {
        return Solicitud.builder()
                .tipoDocumento("CC")
                .numeroDocumento("1234567890")
                .nombres("Juan")
                .apellidos("Perez")
                .correoElectronico("juan.perez@example.com")
                .telefonoCelular("3001234567")
                .montoSolicitado(new BigDecimal("15000000.00"))
                .plazoMeses((short) 36)
                .ingresosMensuales(new BigDecimal("4000000.00"))
                .estado(ESTADO_APROBADO)
                .scoreBuro(750)
                .tasaEstimada(new BigDecimal("1.20"))
                .fechaCreacion(LocalDateTime.of(2026, 8, 30, 10, 30))
                .build();
    }

    private ResultadoValidacion crearResultado(short orden, String nombre, String resultado, String detalle) {
        return ResultadoValidacion.builder()
                .orden(orden)
                .nombre(nombre)
                .resultado(resultado)
                .detalle(detalle)
                .build();
    }
}
