package com.weiz.motordedecision.infraestructura.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el cliente del buro contra un servidor HTTP real
 * ({@link ServidorBuroSimulado}) en los tres caminos que exige la feature:
 * respuesta correcta, error 5xx y buro que tarda mas de la cuenta.
 *
 * Fija ademas las dos cosas que la prueba de mutacion destapo sin red
 * (progress/review_motor.md): el verbo, la ruta y el cuerpo con los que sale la
 * consulta, y el enmascarado del documento en el WARN del buro caido.
 *
 * El circuito se deja fuera de juego a proposito —hacen falta muchas mas
 * llamadas de las que hace esta clase para que juzgue— porque aqui se mide el
 * reintento y el fallback. La apertura del circuito se prueba aparte, en
 * {@link AperturaDelCircuitoDelBuroTest}, que necesita otra configuracion.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "buro.tiempo-espera-conexion=200ms",
        "buro.tiempo-espera-lectura=300ms",
        "resilience4j.retry.instances.buro.wait-duration=20ms",
        // Las dos juntas: en una ventana COUNT_BASED, Resilience4j recorta
        // `minimum-number-of-calls` al tamano de la ventana, asi que subir solo
        // el minimo deja el circuito juzgando a las diez llamadas
        // (docs/decisions.md D-072).
        "resilience4j.circuitbreaker.instances.buro.sliding-window-size=1000",
        "resilience4j.circuitbreaker.instances.buro.minimum-number-of-calls=1000"
})
@DisplayName("Cliente HTTP del buro")
class BuroClientHttpTest {

    private static final String TIPO_DOCUMENTO = "CC";
    private static final String NUMERO_DOCUMENTO = "1234567890";

    /** Los cuatro primeros digitos y el resto tapado: lo que permite `DIGITOS_VISIBLES_DEL_DOCUMENTO`. */
    private static final String DOCUMENTO_ENMASCARADO = "1234******";

    /** Debe coincidir con `resilience4j.retry.instances.buro.max-attempts`. */
    private static final int INTENTOS_CONFIGURADOS = 3;

    private static final int CODIGO_ERROR_DEL_SERVIDOR = 500;
    private static final String CUERPO_DE_ERROR = "{\"message\":\"Error interno\"}";

    private static final int SCORE_ESPERADO = 750;
    private static final String ESTADO_ESPERADO = "ACTIVO";
    private static final LocalDateTime FECHA_ESPERADA = LocalDateTime.of(2026, 8, 31, 10, 15, 30);
    private static final String INFORME_JSON = """
            {"score":750,"estado":"ACTIVO","reporteNegativo":false,"fechaConsulta":"2026-08-31 10:15:30"}""";

    /** Mas del tiempo de espera de lectura, para que la peticion no llegue a completarse. */
    private static final Duration RETARDO_MAYOR_QUE_LA_ESPERA = Duration.ofMillis(900);

    private static final ServidorBuroSimulado SERVIDOR = ServidorBuroSimulado.iniciar();

    @Autowired
    private BuroClient buroClient;

    private Logger loggerDelCliente;
    private ListAppender<ILoggingEvent> eventosDelCliente;

    @DynamicPropertySource
    static void registrarLaDireccionDelBuroSimulado(DynamicPropertyRegistry registro) {
        registro.add("buro.url", SERVIDOR::obtenerUrl);
    }

    @AfterAll
    static void detenerElServidorSimulado() {
        SERVIDOR.detener();
    }

    /**
     * Engancha un appender en memoria al logger del cliente. El log es la unica
     * salida observable del enmascarado del documento: sin esto, apagarlo no
     * rompe ningun test y el checkpoint C4 se queda sin red.
     */
    @BeforeEach
    void prepararElServidorYElAppenderDeLogs() {
        SERVIDOR.reiniciarContadorDePeticiones();

        eventosDelCliente = new ListAppender<>();
        eventosDelCliente.start();
        loggerDelCliente = (Logger) LoggerFactory.getLogger(BuroClientHttp.class);
        loggerDelCliente.addAppender(eventosDelCliente);
    }

    @AfterEach
    void desengancharElAppenderDeLogs() {
        loggerDelCliente.detachAppender(eventosDelCliente);
        eventosDelCliente.stop();
    }

    @Test
    @DisplayName("Con 200 del buro devuelve el informe mapeado a dominio, sin reintentar")
    void consultarInforme_cuandoElBuroResponde200_devuelveElInformeMapeado() {
        SERVIDOR.responderCon(200, INFORME_JSON);

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoConInforme(
                SCORE_ESPERADO, ESTADO_ESPERADO, false, FECHA_ESPERADA));
        assertThat(SERVIDOR.contarPeticionesRecibidas()).isOne();
    }

    @Test
    @DisplayName("La consulta sale como POST a /api/buro/consulta con el tipo y el numero de documento")
    void consultarInforme_cuandoConsultaElBuro_respetaElVerboLaRutaYElCuerpoDelContrato() {
        SERVIDOR.responderCon(200, INFORME_JSON);

        buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        ServidorBuroSimulado.PeticionRecibida peticion = SERVIDOR.obtenerUltimaPeticion();
        assertThat(peticion.metodo()).isEqualTo(ServidorBuroSimulado.METODO_DE_CONSULTA);
        assertThat(peticion.ruta()).isEqualTo(ServidorBuroSimulado.RUTA_DE_CONSULTA);
        assertThat(peticion.cuerpo())
                .contains("\"" + ServidorBuroSimulado.CAMPO_TIPO_DOCUMENTO + "\":\"" + TIPO_DOCUMENTO + "\"")
                .contains("\"" + ServidorBuroSimulado.CAMPO_NUMERO_DOCUMENTO + "\":\"" + NUMERO_DOCUMENTO + "\"");
    }

    @Test
    @DisplayName("Con 500 del buro reintenta hasta el maximo y cae en el fallback, sin lanzar")
    void consultarInforme_cuandoElBuroDevuelve500_reintentaYCaeEnElFallback() {
        SERVIDOR.responderCon(CODIGO_ERROR_DEL_SERVIDOR, CUERPO_DE_ERROR);

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoBuroNoDisponible());
        assertThat(SERVIDOR.contarPeticionesRecibidas()).isEqualTo(INTENTOS_CONFIGURADOS);
    }

    @Test
    @DisplayName("Con un buro que tarda mas que la espera de lectura, el resultado es buro no disponible")
    void consultarInforme_cuandoElBuroTardaMasQueLaEsperaDeLectura_caeEnElFallback() {
        SERVIDOR.responderConRetardo(RETARDO_MAYOR_QUE_LA_ESPERA, INFORME_JSON);

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoBuroNoDisponible());
        assertThat(SERVIDOR.contarPeticionesRecibidas()).isEqualTo(INTENTOS_CONFIGURADOS);
    }

    @Test
    @DisplayName("Un 200 con el cuerpo vacio se trata como buro caido, no devuelve un resultado nulo")
    void consultarInforme_cuandoElBuroResponde200SinCuerpo_caeEnElFallback() {
        SERVIDOR.responderCon(200, "");

        ResultadoConsultaBuro resultado = buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        assertThat(resultado).isEqualTo(ResultadoConsultaBuro.crearResultadoBuroNoDisponible());
    }

    @Test
    @DisplayName("El WARN del buro caido escribe el documento enmascarado, nunca el numero completo")
    void consultarInforme_cuandoElBuroDevuelve500_registraElDocumentoEnmascaradoEnUnWarn() {
        SERVIDOR.responderCon(CODIGO_ERROR_DEL_SERVIDOR, CUERPO_DE_ERROR);

        buroClient.consultarInforme(TIPO_DOCUMENTO, NUMERO_DOCUMENTO);

        ILoggingEvent evento = obtenerElUnicoEventoRegistrado();
        assertThat(evento.getLevel()).isEqualTo(Level.WARN);
        assertThat(evento.getFormattedMessage())
                .contains(DOCUMENTO_ENMASCARADO)
                .doesNotContain(NUMERO_DOCUMENTO);
    }

    private ILoggingEvent obtenerElUnicoEventoRegistrado() {
        List<ILoggingEvent> eventos = eventosDelCliente.list;
        assertThat(eventos).hasSize(1);
        return eventos.get(0);
    }
}
