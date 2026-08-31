package com.weiz.motordedecision.domain.dataaccessors;

import com.weiz.motordedecision.domain.entities.ResultadoValidacion;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.exceptions.tecnica.ErrorDePersistencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistencia de solicitudes contra H2 en memoria, que es el motor de pruebas
 * del proyecto (docs/architecture.md §5).
 *
 * Aqui se comprueba el comportamiento: el consecutivo por dia, la consulta por
 * documento y el envoltorio de los fallos de la base. Que el mapeo cuadre con
 * el esquema versionado es otra pregunta y la responde
 * {@link EsquemaVersionadoTest}, que corre contra PostgreSQL real.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({SolicitudDataAccessor.class, GeneradorIdSolicitud.class})
@DisplayName("Persistencia de solicitudes")
class SolicitudDataAccessorTest {

    private static final String TIPO_DOCUMENTO = "CC";
    private static final String NUMERO_DOCUMENTO = "1234567890";
    private static final LocalDateTime RADICACION = LocalDateTime.of(2026, 8, 30, 10, 30);

    @Autowired
    private SolicitudDataAccessor solicitudDataAccessor;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Una solicitud nueva recibe el primer consecutivo del dia")
    void guardarSolicitud_conLaPrimeraSolicitudDelDia_asignaElConsecutivoUno() {
        Solicitud solicitud = crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION);

        Solicitud guardada = solicitudDataAccessor.guardarSolicitud(solicitud);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getIdSolicitud()).isEqualTo("SOL-20260830-001");
    }

    @Test
    @DisplayName("Dos radicaciones del mismo dia llevan consecutivos distintos y correlativos")
    void guardarSolicitud_conDosRadicacionesDelMismoDia_incrementaElConsecutivo() {
        solicitudDataAccessor.guardarSolicitud(crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION));

        Solicitud segunda = solicitudDataAccessor.guardarSolicitud(
                crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION.plusHours(2)));

        assertThat(segunda.getIdSolicitud()).isEqualTo("SOL-20260830-002");
    }

    @Test
    @DisplayName("El consecutivo es por dia: al dia siguiente vuelve a empezar en 001")
    void guardarSolicitud_conRadicacionDeOtroDia_reiniciaElConsecutivo() {
        solicitudDataAccessor.guardarSolicitud(crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION));

        Solicitud delDiaSiguiente = solicitudDataAccessor.guardarSolicitud(
                crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION.plusDays(1)));

        assertThat(delDiaSiguiente.getIdSolicitud()).isEqualTo("SOL-20260831-001");
    }

    @Test
    @DisplayName("Sin fecha de creacion se radica hoy, y el identificador habla del mismo dia")
    void guardarSolicitud_sinFechaDeCreacion_radicaConLaFechaDeHoy() {
        String prefijoDeHoy = "SOL-%s-".formatted(DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now()));

        Solicitud guardada = solicitudDataAccessor.guardarSolicitud(
                crearSolicitudBase(NUMERO_DOCUMENTO, null));

        assertThat(guardada.getFechaCreacion()).isNotNull();
        assertThat(guardada.getIdSolicitud()).startsWith(prefijoDeHoy);
    }

    @Test
    @DisplayName("Un identificador de negocio repetido no se duplica: se envuelve en ErrorDePersistencia")
    void guardarSolicitud_conIdentificadorDeNegocioRepetido_lanzaErrorDePersistencia() {
        Solicitud radicada = solicitudDataAccessor.guardarSolicitud(
                crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION));
        Solicitud repetida = crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION);
        repetida.setIdSolicitud(radicada.getIdSolicitud());

        assertThatThrownBy(() -> solicitudDataAccessor.guardarSolicitud(repetida))
                .isInstanceOf(ErrorDePersistencia.class)
                .hasCauseInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("El rastro de validaciones se guarda en cascada y se recupera en orden")
    void guardarSolicitud_conCadenaDeValidaciones_persisteElRastroEnOrden() {
        Solicitud solicitud = crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION);
        solicitud.agregarResultado(crearResultado((short) 2, "Score", "RECHAZADO", "Score 420 por debajo del minimo"));
        solicitud.agregarResultado(crearResultado((short) 1, "Identidad", "APROBADO", "Documento no bloqueado"));

        Long id = solicitudDataAccessor.guardarSolicitud(solicitud).getId();

        entityManager.flush();
        entityManager.clear();
        Solicitud recuperada = entityManager.find(Solicitud.class, id);
        assertThat(recuperada.getResultados())
                .extracting(ResultadoValidacion::getOrden, ResultadoValidacion::getNombre)
                .containsExactly(
                        tuple((short) 1, "Identidad"),
                        tuple((short) 2, "Score"));
    }

    @Test
    @DisplayName("La consulta por documento devuelve las solicitudes de la mas reciente a la mas antigua")
    void buscarPorDocumento_conVariasSolicitudes_devuelveLaMasRecientePrimero() {
        solicitudDataAccessor.guardarSolicitud(crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION));
        solicitudDataAccessor.guardarSolicitud(crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION.plusDays(1)));

        List<Solicitud> encontradas = solicitudDataAccessor.buscarPorDocumento(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(encontradas)
                .extracting(Solicitud::getIdSolicitud)
                .containsExactly("SOL-20260831-001", "SOL-20260830-001");
    }

    @Test
    @DisplayName("Un documento sin solicitudes devuelve lista vacia, nunca nulo")
    void buscarPorDocumento_conDocumentoSinSolicitudes_devuelveListaVacia() {
        solicitudDataAccessor.guardarSolicitud(crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION));

        List<Solicitud> encontradas = solicitudDataAccessor.buscarPorDocumento(TIPO_DOCUMENTO, "9999999999");

        assertThat(encontradas).isEmpty();
    }

    @Test
    @DisplayName("El mismo numero con otro tipo de documento es otra persona: no se devuelve")
    void buscarPorDocumento_conOtroTipoDeDocumento_noDevuelveLaSolicitud() {
        solicitudDataAccessor.guardarSolicitud(crearSolicitudBase(NUMERO_DOCUMENTO, RADICACION));

        List<Solicitud> encontradas = solicitudDataAccessor.buscarPorDocumento("CE", NUMERO_DOCUMENTO);

        assertThat(encontradas).isEmpty();
    }

    private Solicitud crearSolicitudBase(String numeroDocumento, LocalDateTime fechaCreacion) {
        return Solicitud.builder()
                .tipoDocumento(TIPO_DOCUMENTO)
                .numeroDocumento(numeroDocumento)
                .nombres("Juan")
                .apellidos("Perez")
                .correoElectronico("juan.perez@example.com")
                .telefonoCelular("3001234567")
                .montoSolicitado(new BigDecimal("15000000.00"))
                .plazoMeses((short) 36)
                .ingresosMensuales(new BigDecimal("4000000.00"))
                .estado("APROBADO")
                .scoreBuro(750)
                .tasaEstimada(new BigDecimal("1.20"))
                .fechaCreacion(fechaCreacion)
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
