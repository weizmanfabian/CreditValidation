package com.weiz.buro.api.controllers;

import com.weiz.buro.config.ConfiguracionSimulacionBuro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de rebanada web del endpoint {@code POST /api/buro/consulta}.
 *
 * Se importa {@link ConfiguracionSimulacionBuro} en vez de simular el dominio:
 * el contrato del enunciado se verifica contra el generador real, que es
 * determinista, y de paso queda fijado que el registro de beans de
 * {@code config} funciona.
 *
 * El retardo del caso caido se baja a un milisegundo desde las propiedades. Es
 * la prueba de que el valor es configurable y evita, a la vez, un test que tarde
 * segundos.
 */
@WebMvcTest(ConsultaBuroController.class)
@Import(ConfiguracionSimulacionBuro.class)
@TestPropertySource(properties = {
        "buro.simulacion.documento-servicio-caido=0000000000",
        "buro.simulacion.retardo-servicio-caido=1ms"
})
class ConsultaBuroControllerTest {

    private static final String RUTA_CONSULTA = "/api/buro/consulta";

    private static final String RUTA_SCORE = "$.score";
    private static final String RUTA_ESTADO = "$.estado";
    private static final String RUTA_REPORTE_NEGATIVO = "$.reporteNegativo";
    private static final String RUTA_FECHA_CONSULTA = "$.fechaConsulta";
    private static final String RUTA_MENSAJE = "$.message";
    private static final String RUTA_PRIMER_CAMPO = "$.errors[0].field";
    private static final String RUTA_PRIMERA_LOCALIZACION = "$.errors[0].location";

    private static final String MENSAJE_DATOS_INVALIDOS = "Error en los datos proporcionados";
    private static final String PATRON_FECHA_ISO = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?";

    private static final String CUERPO_DOCUMENTO_PAR = """
            {"tipoDocumento": "CC", "numeroDocumento": "1234567890"}
            """;
    private static final String CUERPO_DOCUMENTO_IMPAR = """
            {"tipoDocumento": "CC", "numeroDocumento": "1234567891"}
            """;
    private static final String CUERPO_DOCUMENTO_SERVICIO_CAIDO = """
            {"tipoDocumento": "CC", "numeroDocumento": "0000000000"}
            """;
    private static final String CUERPO_CON_NUMERO_DOCUMENTO_INVALIDO = """
            {"tipoDocumento": "CC", "numeroDocumento": "abc"}
            """;
    private static final String CUERPO_SIN_TIPO_DOCUMENTO = """
            {"numeroDocumento": "1234567890"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Un documento terminado en par devuelve 200 con el contrato del enunciado")
    void consultarInforme_conDocumentoPar_devuelve200ConScoreAltoSinReporteNegativo() throws Exception {
        mockMvc.perform(post(RUTA_CONSULTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_DOCUMENTO_PAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath(RUTA_SCORE).value(635))
                .andExpect(jsonPath(RUTA_ESTADO).value("ACTIVO"))
                .andExpect(jsonPath(RUTA_REPORTE_NEGATIVO).value(false))
                .andExpect(jsonPath(RUTA_FECHA_CONSULTA).value(matchesPattern(PATRON_FECHA_ISO)));
    }

    @Test
    @DisplayName("Un documento terminado en impar devuelve 200 con score bajo y reporte negativo")
    void consultarInforme_conDocumentoImpar_devuelve200ConScoreBajoConReporteNegativo() throws Exception {
        mockMvc.perform(post(RUTA_CONSULTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_DOCUMENTO_IMPAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath(RUTA_SCORE).value(381))
                .andExpect(jsonPath(RUTA_ESTADO).value("EN_MORA"))
                .andExpect(jsonPath(RUTA_REPORTE_NEGATIVO).value(true))
                .andExpect(jsonPath(RUTA_FECHA_CONSULTA).value(matchesPattern(PATRON_FECHA_ISO)));
    }

    /**
     * Este caso recorre la ruta completa con la pausa real de
     * {@code PausaBloqueante} a un milisegundo. Que el retardo sea el
     * configurado lo fija {@code SimuladorConsultaBuroTest}, y que la pausa
     * bloquee de verdad, {@code PausaBloqueanteTest}: aqui solo se comprueba que
     * el documento reservado sigue devolviendo su informe por la regla de
     * paridad, sin estado especial.
     */
    @Test
    @DisplayName("El documento reservado para el servicio caido atraviesa la pausa y devuelve su informe")
    void consultarInforme_conElDocumentoDelServicioCaido_devuelve200ConElInformeDeSuParidad() throws Exception {
        mockMvc.perform(post(RUTA_CONSULTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_DOCUMENTO_SERVICIO_CAIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath(RUTA_SCORE).value(greaterThanOrEqualTo(600)))
                .andExpect(jsonPath(RUTA_SCORE).value(lessThanOrEqualTo(850)))
                .andExpect(jsonPath(RUTA_REPORTE_NEGATIVO).value(false));
    }

    @Test
    @DisplayName("Un numero de documento invalido devuelve 400 senalando el campo del cuerpo")
    void consultarInforme_conNumeroDocumentoInvalido_devuelve400ConElCampoDelCuerpo() throws Exception {
        mockMvc.perform(post(RUTA_CONSULTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_CON_NUMERO_DOCUMENTO_INVALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_DATOS_INVALIDOS))
                .andExpect(jsonPath(RUTA_PRIMER_CAMPO).value("numeroDocumento"))
                .andExpect(jsonPath(RUTA_PRIMERA_LOCALIZACION).value("body"));
    }

    @Test
    @DisplayName("Sin tipoDocumento se devuelve 400 senalando ese campo, no el otro")
    void consultarInforme_sinTipoDocumento_devuelve400ConElCampoDelCuerpo() throws Exception {
        mockMvc.perform(post(RUTA_CONSULTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_SIN_TIPO_DOCUMENTO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(RUTA_MENSAJE).value(MENSAJE_DATOS_INVALIDOS))
                .andExpect(jsonPath(RUTA_PRIMER_CAMPO).value("tipoDocumento"))
                .andExpect(jsonPath(RUTA_PRIMERA_LOCALIZACION).value("body"));
    }
}
