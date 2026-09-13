package com.weiz.motordedecision.infraestructura.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor HTTP de verdad, en un puerto libre de la maquina, que hace de buro
 * en las pruebas del cliente.
 *
 * Es un socket real y no un {@code MockRestServiceServer} porque hay un caso
 * que solo se puede provocar asi: el buro que acepta la conexion y no contesta
 * a tiempo. Un doble que sustituye la fabrica de peticiones nunca ejercita el
 * tiempo de espera de lectura, que es justo lo que la feature tiene que
 * demostrar. Usa {@code com.sun.net.httpserver}, que viene en el JDK: no anade
 * ninguna dependencia al modulo.
 *
 * Ademas de responder, guarda el contrato HTTP hacia el modulo buro: solo
 * atiende el POST a {@code /api/buro/consulta} con {@code tipoDocumento} y
 * {@code numeroDocumento} en el cuerpo, y responde 404 a cualquier otra cosa
 * (docs/decisions.md D-069). Sin esa guarda, cambiar la ruta en
 * {@link BuroClientHttp} dejaba la suite entera en verde.
 *
 * No es concurrente por diseno: cada test configura una respuesta y llama.
 */
final class ServidorBuroSimulado {

    /** Verbo, ruta y campos que publica ConsultaBuroController en el modulo buro. */
    static final String METODO_DE_CONSULTA = "POST";
    static final String RUTA_DE_CONSULTA = "/api/buro/consulta";
    static final String CAMPO_TIPO_DOCUMENTO = "tipoDocumento";
    static final String CAMPO_NUMERO_DOCUMENTO = "numeroDocumento";

    static final int CODIGO_RECURSO_NO_ENCONTRADO = 404;

    private static final int PUERTO_LIBRE = 0;
    private static final int COLA_POR_OMISION = 0;
    private static final int HILOS_DE_ATENCION = 4;
    private static final String CABECERA_TIPO_CONTENIDO = "Content-Type";
    private static final String TIPO_CONTENIDO_JSON = "application/json";
    private static final String CUERPO_RECURSO_NO_ENCONTRADO =
            "{\"message\":\"La peticion no corresponde al contrato del buro\"}";

    private static final ObjectMapper LECTOR_DE_JSON = new ObjectMapper();

    private final HttpServer servidor;
    private final AtomicInteger peticionesRecibidas = new AtomicInteger();

    private volatile int codigoDeRespuesta = 200;
    private volatile String cuerpoDeRespuesta = "{}";
    private volatile Duration retardoDeRespuesta = Duration.ZERO;
    private volatile PeticionRecibida ultimaPeticion;

    private ServidorBuroSimulado(HttpServer servidor) {
        this.servidor = servidor;
    }

    /** Lo que llego por el cable, para que el test afirme sobre ello. */
    record PeticionRecibida(String metodo, String ruta, String cuerpo) {
    }

    static ServidorBuroSimulado iniciar() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress(PUERTO_LIBRE), COLA_POR_OMISION);
            ServidorBuroSimulado simulado = new ServidorBuroSimulado(servidor);

            servidor.createContext("/", simulado::atenderPeticion);
            servidor.setExecutor(Executors.newFixedThreadPool(HILOS_DE_ATENCION));
            servidor.start();
            return simulado;

        } catch (IOException excepcion) {
            throw new UncheckedIOException("No se pudo levantar el buro simulado", excepcion);
        }
    }

    String obtenerUrl() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    /** Fija la respuesta inmediata que devolvera la proxima peticion. */
    void responderCon(int codigoDeRespuesta, String cuerpoDeRespuesta) {
        this.codigoDeRespuesta = codigoDeRespuesta;
        this.cuerpoDeRespuesta = cuerpoDeRespuesta;
        this.retardoDeRespuesta = Duration.ZERO;
    }

    /**
     * Reproduce el caso del enunciado: el buro no falla, solo tarda. El retardo
     * se consigue durmiendo el hilo que atiende la peticion, que es lo que hace
     * un servicio lento de verdad; la regla que prohibe dormir hilos rige en el
     * codigo de produccion, no en el servicio que se esta simulando.
     */
    void responderConRetardo(Duration retardoDeRespuesta, String cuerpoDeRespuesta) {
        this.codigoDeRespuesta = 200;
        this.cuerpoDeRespuesta = cuerpoDeRespuesta;
        this.retardoDeRespuesta = retardoDeRespuesta;
    }

    int contarPeticionesRecibidas() {
        return peticionesRecibidas.get();
    }

    void reiniciarContadorDePeticiones() {
        peticionesRecibidas.set(0);
    }

    /** Devuelve la ultima peticion atendida, cumpla o no el contrato; nula si no hubo ninguna. */
    PeticionRecibida obtenerUltimaPeticion() {
        return ultimaPeticion;
    }

    void detener() {
        servidor.stop(0);
    }

    private void atenderPeticion(HttpExchange intercambio) throws IOException {
        peticionesRecibidas.incrementAndGet();

        PeticionRecibida peticion = leerLaPeticion(intercambio);
        ultimaPeticion = peticion;

        if (!cumpleElContratoDeConsulta(peticion)) {
            responder(intercambio, CODIGO_RECURSO_NO_ENCONTRADO, CUERPO_RECURSO_NO_ENCONTRADO);
            return;
        }

        esperarElRetardoConfigurado();
        responder(intercambio, codigoDeRespuesta, cuerpoDeRespuesta);
    }

    private static PeticionRecibida leerLaPeticion(HttpExchange intercambio) throws IOException {
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return new PeticionRecibida(
                intercambio.getRequestMethod(), intercambio.getRequestURI().getPath(), cuerpo);
    }

    /**
     * El contrato que el modulo buro publica en ConsultaBuroController: verbo,
     * ruta y los dos campos del cuerpo. Lo que no lo cumpla no es una consulta
     * al buro, y un buro de verdad tampoco la atenderia.
     */
    private static boolean cumpleElContratoDeConsulta(PeticionRecibida peticion) {
        return METODO_DE_CONSULTA.equals(peticion.metodo())
                && RUTA_DE_CONSULTA.equals(peticion.ruta())
                && tieneLosCamposDelDocumento(peticion.cuerpo());
    }

    private static boolean tieneLosCamposDelDocumento(String cuerpo) {
        try {
            JsonNode raiz = LECTOR_DE_JSON.readTree(cuerpo);
            return tieneTextoNoVacio(raiz, CAMPO_TIPO_DOCUMENTO)
                    && tieneTextoNoVacio(raiz, CAMPO_NUMERO_DOCUMENTO);

        } catch (JacksonException cuerpoIlegible) {
            return false;
        }
    }

    private static boolean tieneTextoNoVacio(JsonNode raiz, String campo) {
        JsonNode valor = raiz.get(campo);
        return valor != null && valor.isString() && !valor.asString().isBlank();
    }

    private void responder(HttpExchange intercambio, int codigo, String cuerpoEnTexto) throws IOException {
        byte[] cuerpo = cuerpoEnTexto.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add(CABECERA_TIPO_CONTENIDO, TIPO_CONTENIDO_JSON);
        intercambio.sendResponseHeaders(codigo, cuerpo.length);

        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }

    private void esperarElRetardoConfigurado() {
        if (retardoDeRespuesta.isZero()) {
            return;
        }

        try {
            Thread.sleep(retardoDeRespuesta.toMillis());
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
        }
    }
}
