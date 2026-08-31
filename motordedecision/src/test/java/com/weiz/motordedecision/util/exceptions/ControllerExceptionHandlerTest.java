package com.weiz.motordedecision.util.exceptions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.weiz.motordedecision.util.exceptions.tecnica.BuroNoDisponible;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un test por familia de error de la tabla de {@code docs/error-handling.md} §3,
 * ejercitada contra {@link ControladorDeErroresDePrueba}.
 *
 * Un manejador sin test es un manejador que nadie ha visto funcionar: aqui se
 * comprueba el estado HTTP, el {@code message} y —cuando aplica— el {@code field}
 * y el {@code location} que son lo que identifica el fallo en Postman.
 */
@WebMvcTest(ControladorDeErroresDePrueba.class)
class ControllerExceptionHandlerTest {

    private static final String MENSAJE_DATOS_INVALIDOS = "Error en los datos proporcionados";
    private static final String MENSAJE_CUERPO_ILEGIBLE = "El cuerpo de la peticion no es un JSON valido";
    private static final String MENSAJE_ERROR_INTERNO = "Servicio temporalmente no disponible";
    private static final String MENSAJE_PETICION_NO_ATENDIBLE = "La peticion no se puede atender en este recurso";

    private static final String RUTA_MENSAJE = "$.message";
    private static final String RUTA_CODIGO = "$.codigo";
    private static final String RUTA_DETALLES = "$.detalles";
    private static final String RUTA_PRIMER_CAMPO = "$.errors[0].field";
    private static final String RUTA_PRIMERA_LOCALIZACION = "$.errors[0].location";

    /** Marca con la que empieza cada linea de una traza de pila de Java. */
    private static final String MARCA_DE_TRAZA_DE_PILA = "\tat ";

    private static final String CUERPO_CON_DOS_CAMPOS_INVALIDOS = """
            {"numeroDocumento": "abc", "montoSolicitado": 500000}
            """;
    private static final String CUERPO_CON_JSON_MALFORMADO = """
            {"numeroDocumento": "1234567890", "montoSolicitado": 2000000,}
            """;

    @Autowired
    private MockMvc mockMvc;

    private Logger loggerDelManejador;
    private ListAppender<ILoggingEvent> eventosDelManejador;

    /**
     * Engancha un appender en memoria al logger del manejador. El nivel del log
     * es la unica salida observable de la mitad del contrato que no viaja en la
     * respuesta HTTP: sin esto, degradar el WARN a ERROR o perder la traza de la
     * excepcion tecnica no rompe ningun test.
     */
    @BeforeEach
    void engancharElAppenderDeLogs() {
        eventosDelManejador = new ListAppender<>();
        eventosDelManejador.start();
        loggerDelManejador = (Logger) LoggerFactory.getLogger(ControllerExceptionHandler.class);
        loggerDelManejador.addAppender(eventosDelManejador);
    }

    @AfterEach
    void desengancharElAppenderDeLogs() {
        loggerDelManejador.detachAppender(eventosDelManejador);
        eventosDelManejador.stop();
    }

    @Test
    @DisplayName("Un cuerpo con dos campos invalidos devuelve 400 con los dos en errors[]")
    void manejarErroresDeValidacion_conDosCamposInvalidos_devuelveAmbosEnErrors() throws Exception {
        mockMvc.perform(post(ControladorDeErroresDePrueba.RUTA_BASE + "/cuerpo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_CON_DOS_CAMPOS_INVALIDOS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_DATOS_INVALIDOS))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[*].field")
                        .value(containsInAnyOrder("numeroDocumento", "montoSolicitado")))
                .andExpect(jsonPath("$.errors[*].location").value(everyItem(is("body"))))
                .andExpect(jsonPath(RUTA_CODIGO).doesNotExist());
    }

    @Test
    @DisplayName("Un JSON malformado devuelve 400 con mensaje claro, no 500")
    void manejarCuerpoIlegible_conJsonMalformado_devuelve400ConMensajeClaro() throws Exception {
        mockMvc.perform(post(ControladorDeErroresDePrueba.RUTA_BASE + "/cuerpo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_CON_JSON_MALFORMADO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_CUERPO_ILEGIBLE))
                .andExpect(jsonPath(RUTA_DETALLES).isNotEmpty());
    }

    @Test
    @DisplayName("Una cabecera obligatoria ausente devuelve 400 con location=header")
    void manejarHeaderFaltante_sinLaCabecera_devuelveElCampoConLocationHeader() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_DATOS_INVALIDOS))
                .andExpect(jsonPath(RUTA_PRIMER_CAMPO).value(ControladorDeErroresDePrueba.NOMBRE_HEADER))
                .andExpect(jsonPath(RUTA_PRIMERA_LOCALIZACION).value("header"));
    }

    @Test
    @DisplayName("Un parametro de consulta obligatorio ausente devuelve 400 con location=query")
    void manejarParametroFaltante_sinElParametro_devuelveElCampoConLocationQuery() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_PRIMER_CAMPO).value(ControladorDeErroresDePrueba.NOMBRE_PARAMETRO))
                .andExpect(jsonPath(RUTA_PRIMERA_LOCALIZACION).value("query"));
    }

    @Test
    @DisplayName("Una variable de ruta con tipo incorrecto devuelve 400 con location=path")
    void manejarTipoDeArgumentoInvalido_conVariableDeRutaNoNumerica_devuelveLocationPath() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/ruta/{identificador}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_PRIMER_CAMPO).value("identificador"))
                .andExpect(jsonPath(RUTA_PRIMERA_LOCALIZACION).value("path"));
    }

    @Test
    @DisplayName("Un parametro anotado que incumple su restriccion devuelve 400 con su mensaje")
    void manejarErroresDeParametrosAnotados_conParametroEnBlanco_devuelveElCampoConSuMensaje() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/parametro-anotado").param("correo", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_DATOS_INVALIDOS))
                .andExpect(jsonPath("$.errors[0].message").value("Correo es requerido"))
                .andExpect(jsonPath(RUTA_PRIMERA_LOCALIZACION).value("query"));
    }

    @Test
    @DisplayName("Una ConstraintViolationException devuelve 400 con el ultimo tramo del path como field")
    void manejarViolacionesDeRestriccion_conViolacionDeServicio_devuelveElNombreDelCampo() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/restriccion"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_DATOS_INVALIDOS))
                .andExpect(jsonPath(RUTA_PRIMER_CAMPO).value("numeroDocumento"))
                .andExpect(jsonPath(RUTA_PRIMERA_LOCALIZACION).value("body"));
    }

    @Test
    @DisplayName("Una IllegalArgumentException devuelve 400 con la causa en detalles")
    void manejarArgumentoIlegal_conArgumentoInvalido_devuelve400ConDetalles() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/argumento-ilegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_DATOS_INVALIDOS))
                .andExpect(jsonPath(RUTA_DETALLES).value(ControladorDeErroresDePrueba.MENSAJE_ARGUMENTO_ILEGAL));
    }

    @Test
    @DisplayName("SolicitudNoEncontrada devuelve 404 con el codigo publicado y su detalle")
    void manejarErrorDeNegocio_conSolicitudInexistente_devuelve404ConCodigoYDetalles() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/negocio"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(RUTA_CODIGO).value("SOLICITUD_NO_ENCONTRADA"))
                .andExpect(jsonPath(RUTA_MENSAJE).value("No existe una solicitud con el identificador indicado"))
                .andExpect(jsonPath(RUTA_DETALLES)
                        .value(ControladorDeErroresDePrueba.DETALLE_SOLICITUD_NO_ENCONTRADA));
    }

    @Test
    @DisplayName("Un error de negocio se registra en WARN y sin traza de pila")
    void manejarErrorDeNegocio_conSolicitudInexistente_registraWarnSinTraza() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/negocio"))
                .andExpect(status().isNotFound());

        ILoggingEvent evento = obtenerUnicoEventoRegistrado();
        assertThat(evento.getLevel()).isEqualTo(Level.WARN);
        assertThat(evento.getThrowableProxy()).isNull();
    }

    @Test
    @DisplayName("BuroNoDisponible devuelve 500 generico sin codigo y sin filtrar su mensaje")
    void manejarErrorTecnico_conElBuroCaido_devuelve500SinFiltrarElMensaje() throws Exception {
        String cuerpo = mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/buro-caido"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_ERROR_INTERNO))
                .andExpect(jsonPath(RUTA_CODIGO).doesNotExist())
                .andExpect(jsonPath(RUTA_DETALLES).doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo)
                .doesNotContain(ControladorDeErroresDePrueba.MENSAJE_BURO_NO_DISPONIBLE)
                .doesNotContain(ControladorDeErroresDePrueba.MENSAJE_CAUSA_DEL_BURO)
                .doesNotContain(BuroNoDisponible.class.getSimpleName())
                .doesNotContain(ResourceAccessException.class.getSimpleName())
                .doesNotContain(MARCA_DE_TRAZA_DE_PILA);
    }

    @Test
    @DisplayName("ErrorDePersistencia devuelve 500 generico sin filtrar el fallo de la base de datos")
    void manejarErrorTecnico_conFalloDeLaBaseDeDatos_devuelve500SinFiltrarElMensaje() throws Exception {
        String cuerpo = mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/persistencia"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_ERROR_INTERNO))
                .andExpect(jsonPath(RUTA_DETALLES).doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo)
                .doesNotContain(ControladorDeErroresDePrueba.MENSAJE_PERSISTENCIA)
                .doesNotContain(ControladorDeErroresDePrueba.MENSAJE_CAUSA_DE_PERSISTENCIA)
                .doesNotContain(MARCA_DE_TRAZA_DE_PILA);
    }

    @Test
    @DisplayName("Una excepcion tecnica se registra en ERROR con la excepcion y su causa original")
    void manejarErrorTecnico_conElBuroCaido_registraErrorConLaTrazaYSuCausa() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/buro-caido"))
                .andExpect(status().isInternalServerError());

        ILoggingEvent evento = obtenerUnicoEventoRegistrado();
        assertThat(evento.getLevel()).isEqualTo(Level.ERROR);
        assertThat(evento.getThrowableProxy())
                .isNotNull()
                .extracting(IThrowableProxy::getClassName, IThrowableProxy::getMessage)
                .containsExactly(BuroNoDisponible.class.getName(),
                        ControladorDeErroresDePrueba.MENSAJE_BURO_NO_DISPONIBLE);
        assertThat(evento.getThrowableProxy().getCause())
                .isNotNull()
                .extracting(IThrowableProxy::getClassName, IThrowableProxy::getMessage)
                .containsExactly(ResourceAccessException.class.getName(),
                        ControladorDeErroresDePrueba.MENSAJE_CAUSA_DEL_BURO);
    }

    @Test
    @DisplayName("Una ruta inexistente sigue devolviendo 404: el manejador general no la convierte en 500")
    void manejarErrorInterno_conRutaInexistente_conservaEl404DeSpring() throws Exception {
        mockMvc.perform(get("/ruta-que-no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_PETICION_NO_ATENDIBLE));
    }

    @Test
    @DisplayName("Una excepcion no prevista devuelve 500 generico sin filtrar su mensaje")
    void manejarErrorInterno_conExcepcionNoPrevista_devuelve500SinFiltrarElMensaje() throws Exception {
        String cuerpo = mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_ERROR_INTERNO))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo)
                .doesNotContain(ControladorDeErroresDePrueba.MENSAJE_INESPERADO_INTERNO)
                .doesNotContain(MARCA_DE_TRAZA_DE_PILA);
    }

    /**
     * Devuelve el evento que el manejador registro durante la peticion. Exigir
     * que sea uno solo evita que una asercion de nivel pase por casualidad
     * porque alguna otra rama tambien escribio.
     */
    private ILoggingEvent obtenerUnicoEventoRegistrado() {
        assertThat(eventosDelManejador.list).hasSize(1);
        return eventosDelManejador.list.get(0);
    }
}
