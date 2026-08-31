package com.weiz.buro.util.exceptions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.weiz.buro.util.exceptions.tecnica.ExcepcionTecnicaDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    private static final String RUTA_MENSAJE = "$.message";
    private static final String RUTA_CODIGO = "$.codigo";
    private static final String RUTA_DETALLES = "$.detalles";
    private static final String RUTA_PRIMER_CAMPO = "$.errors[0].field";
    private static final String RUTA_PRIMERA_LOCALIZACION = "$.errors[0].location";

    private static final String CUERPO_CON_DOS_CAMPOS_INVALIDOS = """
            {"tipoDocumento": null, "numeroDocumento": "abc"}
            """;
    private static final String CUERPO_CON_JSON_MALFORMADO = """
            {"tipoDocumento": "CC", "numeroDocumento": "1234567890",}
            """;

    @Autowired
    private MockMvc mockMvc;

    private Logger loggerDelManejador;
    private ListAppender<ILoggingEvent> eventosDelManejador;

    /**
     * Engancha un appender en memoria al logger del manejador. El nivel del log
     * es la unica salida observable de la mitad del criterio 4 que no viaja en la
     * respuesta HTTP: sin esto, degradar el WARN a ERROR o perder la traza no
     * rompe ningun test.
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
                        .value(containsInAnyOrder("tipoDocumento", "numeroDocumento")))
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
                .andExpect(jsonPath("$.errors[0].message").value("correo es requerido"))
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
    @DisplayName("Una ExcepcionNegocio devuelve 4xx con el codigo publicado y su detalle")
    void manejarErrorDeNegocio_conReglaIncumplida_devuelve400ConCodigoYDetalles() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/negocio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_CODIGO).value("TIPO_DOCUMENTO_NO_SOPORTADO"))
                .andExpect(jsonPath(RUTA_MENSAJE).value("El tipo de documento no es CC, CE ni PA"))
                .andExpect(jsonPath(RUTA_DETALLES).value(ControladorDeErroresDePrueba.MENSAJE_NEGOCIO));
    }

    @Test
    @DisplayName("Una ExcepcionNegocio se registra en WARN y sin traza de pila")
    void manejarErrorDeNegocio_conReglaIncumplida_registraWarnSinTraza() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/negocio"))
                .andExpect(status().isBadRequest());

        ILoggingEvent evento = obtenerUnicoEventoRegistrado();
        assertThat(evento.getLevel()).isEqualTo(Level.WARN);
        assertThat(evento.getThrowableProxy()).isNull();
    }

    @Test
    @DisplayName("Una ExcepcionTecnica devuelve 500 generico sin codigo y sin filtrar su mensaje")
    void manejarErrorTecnico_conFalloDeDependencia_devuelve500SinFiltrarElMensaje() throws Exception {
        String cuerpo = mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/tecnica"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_ERROR_INTERNO))
                .andExpect(jsonPath(RUTA_CODIGO).doesNotExist())
                .andExpect(jsonPath(RUTA_DETALLES).doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain(ControladorDeErroresDePrueba.MENSAJE_TECNICO_INTERNO);
    }

    @Test
    @DisplayName("Una ExcepcionTecnica se registra en ERROR con la excepcion y su causa original")
    void manejarErrorTecnico_conFalloDeDependencia_registraErrorConLaTrazaYSuCausa() throws Exception {
        mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/tecnica"))
                .andExpect(status().isInternalServerError());

        ILoggingEvent evento = obtenerUnicoEventoRegistrado();
        assertThat(evento.getLevel()).isEqualTo(Level.ERROR);
        assertThat(evento.getThrowableProxy())
                .isNotNull()
                .extracting(IThrowableProxy::getClassName, IThrowableProxy::getMessage)
                .containsExactly(ExcepcionTecnicaDePrueba.class.getName(),
                        ControladorDeErroresDePrueba.MENSAJE_TECNICO_INTERNO);
        assertThat(evento.getThrowableProxy().getCause())
                .isNotNull()
                .extracting(IThrowableProxy::getClassName, IThrowableProxy::getMessage)
                .containsExactly(IllegalStateException.class.getName(),
                        ControladorDeErroresDePrueba.MENSAJE_CAUSA_ORIGINAL);
    }

    @Test
    @DisplayName("Una ruta inexistente sigue devolviendo 404: el manejador general no la convierte en 500")
    void manejarErrorInterno_conRutaInexistente_conservaEl404DeSpring() throws Exception {
        mockMvc.perform(get("/ruta-que-no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(RUTA_MENSAJE).value("La peticion no se puede atender en este recurso"));
    }

    @Test
    @DisplayName("Una excepcion no prevista devuelve 500 generico sin filtrar su mensaje")
    void manejarErrorInterno_conExcepcionNoPrevista_devuelve500SinFiltrarElMensaje() throws Exception {
        String cuerpo = mockMvc.perform(get(ControladorDeErroresDePrueba.RUTA_BASE + "/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_ERROR_INTERNO))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain(ControladorDeErroresDePrueba.MENSAJE_INESPERADO_INTERNO);
    }

    /**
     * Devuelve el evento que el manejador registro durante la peticion. Exigir
     * que sea uno solo evita que una asercion de nivel pase por casualidad
     * porque alguna otra rama tambien escribio.
     */
    private ILoggingEvent obtenerUnicoEventoRegistrado() {
        assertThat(eventosDelManejador.list).hasSize(1);
        return eventosDelManejador.list.getFirst();
    }
}
