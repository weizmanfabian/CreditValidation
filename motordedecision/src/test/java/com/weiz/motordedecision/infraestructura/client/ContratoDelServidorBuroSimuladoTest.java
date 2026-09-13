package com.weiz.motordedecision.infraestructura.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Prueba la guarda del doble, no el codigo de produccion.
 *
 * {@link ServidorBuroSimulado} es lo unico que sujeta el contrato HTTP con el
 * modulo buro en toda la suite del motor: si su guarda se rompe, los tests del
 * cliente siguen en verde aunque el motor llame a otra ruta. Por eso la guarda
 * se prueba aparte y de forma directa, con un cliente HTTP del JDK que habla
 * con el socket real (docs/decisions.md D-069).
 *
 * No levanta contexto de Spring: es un test unitario sobre un socket.
 */
@DisplayName("Contrato del buro simulado")
class ContratoDelServidorBuroSimuladoTest {

    private static final String CUERPO_DE_CONSULTA_COMPLETO = """
            {"tipoDocumento":"CC","numeroDocumento":"1234567890"}""";
    private static final String INFORME_JSON = """
            {"score":750,"estado":"ACTIVO","reporteNegativo":false,"fechaConsulta":"2026-08-31 10:15:30"}""";

    private static final int CODIGO_CORRECTO = 200;

    private static ServidorBuroSimulado servidor;
    private static HttpClient cliente;

    @BeforeAll
    static void iniciarElServidorSimulado() {
        servidor = ServidorBuroSimulado.iniciar();
        cliente = HttpClient.newHttpClient();
    }

    @AfterAll
    static void detenerElServidorSimulado() {
        servidor.detener();
    }

    @BeforeEach
    void configurarLaRespuestaCorrecta() {
        servidor.responderCon(CODIGO_CORRECTO, INFORME_JSON);
    }

    @Test
    @DisplayName("El POST a la ruta de consulta con los dos campos recibe la respuesta configurada")
    void atenderPeticion_conElContratoDelBuro_devuelveLaRespuestaConfigurada() throws Exception {
        HttpResponse<String> respuesta = enviar(
                ServidorBuroSimulado.METODO_DE_CONSULTA,
                ServidorBuroSimulado.RUTA_DE_CONSULTA,
                CUERPO_DE_CONSULTA_COMPLETO);

        assertThat(respuesta.statusCode()).isEqualTo(CODIGO_CORRECTO);
        assertThat(respuesta.body()).isEqualTo(INFORME_JSON);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("crearPeticionesFueraDelContrato")
    @DisplayName("Cualquier peticion fuera del contrato recibe 404, no la respuesta configurada")
    void atenderPeticion_fueraDelContratoDelBuro_devuelve404(String escenario,
                                                             String metodo,
                                                             String ruta,
                                                             String cuerpo) throws Exception {

        HttpResponse<String> respuesta = enviar(metodo, ruta, cuerpo);

        assertThat(respuesta.statusCode())
                .as(escenario)
                .isEqualTo(ServidorBuroSimulado.CODIGO_RECURSO_NO_ENCONTRADO);
    }

    static Stream<Arguments> crearPeticionesFueraDelContrato() {
        String ruta = ServidorBuroSimulado.RUTA_DE_CONSULTA;
        String metodo = ServidorBuroSimulado.METODO_DE_CONSULTA;

        return Stream.of(
                arguments("GET sobre la ruta de consulta", "GET", ruta, CUERPO_DE_CONSULTA_COMPLETO),
                arguments("PUT sobre la ruta de consulta", "PUT", ruta, CUERPO_DE_CONSULTA_COMPLETO),
                arguments("POST a la raiz", metodo, "/", CUERPO_DE_CONSULTA_COMPLETO),
                arguments("POST a una ruta parecida", metodo, ruta + "s", CUERPO_DE_CONSULTA_COMPLETO),
                arguments("cuerpo sin numeroDocumento", metodo, ruta, "{\"tipoDocumento\":\"CC\"}"),
                arguments("cuerpo sin tipoDocumento", metodo, ruta, "{\"numeroDocumento\":\"1234567890\"}"),
                arguments("cuerpo con el documento en blanco", metodo, ruta,
                        "{\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"  \"}"),
                arguments("cuerpo vacio", metodo, ruta, ""),
                arguments("cuerpo que no es JSON", metodo, ruta, "tipoDocumento=CC"));
    }

    private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo)
            throws IOException, InterruptedException {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(servidor.obtenerUrl() + ruta))
                .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();

        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }
}
