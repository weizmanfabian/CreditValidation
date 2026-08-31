package com.weiz.motordedecision.infraestructura.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

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
 * No es concurrente por diseno: cada test configura una respuesta y llama.
 */
final class ServidorBuroSimulado {

    private static final int PUERTO_LIBRE = 0;
    private static final int COLA_POR_OMISION = 0;
    private static final int HILOS_DE_ATENCION = 4;
    private static final String CABECERA_TIPO_CONTENIDO = "Content-Type";
    private static final String TIPO_CONTENIDO_JSON = "application/json";

    private final HttpServer servidor;
    private final AtomicInteger peticionesRecibidas = new AtomicInteger();

    private volatile int codigoDeRespuesta = 200;
    private volatile String cuerpoDeRespuesta = "{}";
    private volatile Duration retardoDeRespuesta = Duration.ZERO;

    private ServidorBuroSimulado(HttpServer servidor) {
        this.servidor = servidor;
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

    void detener() {
        servidor.stop(0);
    }

    private void atenderPeticion(HttpExchange intercambio) throws IOException {
        peticionesRecibidas.incrementAndGet();
        esperarElRetardoConfigurado();

        byte[] cuerpo = cuerpoDeRespuesta.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add(CABECERA_TIPO_CONTENIDO, TIPO_CONTENIDO_JSON);
        intercambio.sendResponseHeaders(codigoDeRespuesta, cuerpo.length);

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
