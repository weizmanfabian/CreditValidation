package com.weiz.motordedecision.api.controllers;

import com.weiz.motordedecision.api.models.request.SolicitudCreditoRequest;
import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.infraestructura.service.SolicitudCreditoService;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.enums.TipoDocumento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rebanada web del controlador de radicacion: rutas, validacion del cuerpo y
 * delegacion en el servicio. El flujo completo con el buro simulado y la
 * persistencia se prueba aparte, en {@code RadicacionDeSolicitudTest}.
 *
 * El criterio que se fija aqui es el del contrato de errores: una entrada
 * invalida responde 400 con TODOS los campos rechazados en {@code errors[]},
 * no solo el primero, y sin tocar el servicio
 * ({@code docs/error-handling.md} §7).
 */
@WebMvcTest(SolicitudCreditoController.class)
@DisplayName("Controlador de radicacion de solicitudes")
class SolicitudCreditoControllerTest {

    private static final String RUTA_SOLICITUDES = "/api/solicitudes";
    private static final String ID_SOLICITUD = "SOL-20260831-001";

    private static final String CUERPO_VALIDO_DEL_ENUNCIADO = """
            {
              "tipoDocumento": "CC",
              "numeroDocumento": "1234567890",
              "nombres": "Juan",
              "apellidos": "Pérez",
              "correo": "juan.perez@example.com",
              "celular": "3001234567",
              "montoSolicitado": 15000000,
              "plazoMeses": 36,
              "ingresosMensuales": 4000000
            }
            """;

    /**
     * Nueve campos y los nueve mal: cuatro ausentes y cinco con el valor fuera
     * de lo permitido. Es el peor caso de la tabla de
     * {@code docs/error-handling.md} §7.
     */
    private static final String CUERPO_CON_LOS_NUEVE_CAMPOS_INVALIDOS = """
            {
              "numeroDocumento": "12",
              "correo": "correo-sin-arroba",
              "celular": "1234567890",
              "montoSolicitado": 500000,
              "plazoMeses": 18,
              "ingresosMensuales": 0
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudCreditoService solicitudCreditoService;

    @Test
    @DisplayName("POST valido responde 200 y delega la peticion completa en el servicio")
    void radicarSolicitud_conCuerpoValido_delegaEnElServicioYDevuelveSuRespuesta() throws Exception {
        when(solicitudCreditoService.radicarSolicitud(any())).thenReturn(crearRespuestaMinima());

        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO_DEL_ENUNCIADO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSolicitud").value(ID_SOLICITUD))
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        ArgumentCaptor<SolicitudCreditoRequest> peticion = ArgumentCaptor.forClass(SolicitudCreditoRequest.class);
        verify(solicitudCreditoService).radicarSolicitud(peticion.capture());
        assertThat(peticion.getValue())
                .returns(TipoDocumento.CC, SolicitudCreditoRequest::tipoDocumento)
                .returns("1234567890", SolicitudCreditoRequest::numeroDocumento)
                .returns(new BigDecimal("15000000"), SolicitudCreditoRequest::montoSolicitado)
                .returns(36, SolicitudCreditoRequest::plazoMeses);
    }

    @Test
    @DisplayName("POST con los nueve campos invalidos responde 400 con los nueve en errors[]")
    void radicarSolicitud_conTodosLosCamposInvalidos_devuelve400ConTodosLosCampos() throws Exception {
        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_CON_LOS_NUEVE_CAMPOS_INVALIDOS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error en los datos proporcionados"))
                .andExpect(jsonPath("$.errors", hasSize(9)))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                        "tipoDocumento", "numeroDocumento", "nombres", "apellidos", "correo",
                        "celular", "montoSolicitado", "plazoMeses", "ingresosMensuales")))
                .andExpect(jsonPath("$.errors[*].location", everyItem(is("body"))));

        verify(solicitudCreditoService, never()).radicarSolicitud(any());
    }

    @Test
    @DisplayName("POST con monto por debajo del minimo responde 400 indicando el campo y el minimo")
    void radicarSolicitud_conMontoMenorAlMinimo_devuelve400ConElCampoYSuMensaje() throws Exception {
        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO_DEL_ENUNCIADO.replace("15000000", "500000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("montoSolicitado"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Monto solicitado debe ser mayor o igual a 1000000"))
                .andExpect(jsonPath("$.errors[0].location").value("body"));

        verify(solicitudCreditoService, never()).radicarSolicitud(any());
    }

    @Test
    @DisplayName("POST con plazo fuera del conjunto responde 400 enumerando los plazos validos")
    void radicarSolicitud_conPlazoFueraDelConjunto_devuelve400ConLosPlazosValidos() throws Exception {
        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO_DEL_ENUNCIADO.replace("36", "18")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("plazoMeses"))
                .andExpect(jsonPath("$.errors[0].message").value("Plazo en meses debe ser 12, 24, 36 o 48"));

        verify(solicitudCreditoService, never()).radicarSolicitud(any());
    }

    @Test
    @DisplayName("POST con un JSON roto responde 400 sin tocar el servicio")
    void radicarSolicitud_conCuerpoIlegible_devuelve400SinTocarElServicio() throws Exception {
        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"tipoDocumento\": \"CC\", }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la peticion no es un JSON valido"));

        verify(solicitudCreditoService, never()).radicarSolicitud(any());
    }

    private static SolicitudCreditoResponse crearRespuestaMinima() {
        return SolicitudCreditoResponse
                .crearConstructorPara(ID_SOLICITUD, LocalDateTime.of(2026, 8, 31, 10, 30), EstadoSolicitud.APROBADO)
                .construir();
    }
}
