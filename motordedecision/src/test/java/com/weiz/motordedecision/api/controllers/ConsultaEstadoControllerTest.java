package com.weiz.motordedecision.api.controllers;

import com.weiz.motordedecision.api.models.response.EstadoDeSolicitudResponse;
import com.weiz.motordedecision.infraestructura.service.ConsultaEstadoService;
import com.weiz.motordedecision.util.enums.TipoDocumento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rebanada web de la consulta de estado: la ruta, la conversion del tipo de
 * documento y la delegacion en el servicio. El flujo con la persistencia real
 * se prueba aparte, en {@code ConsultaDeEstadoTest}.
 *
 * Los tres criterios de la feature se fijan aqui: la lista con estado y fecha,
 * el documento sin solicitudes que responde 200 con lista vacia —nunca 404—, y
 * el tipo de documento invalido que responde 400 con {@code location=path} sin
 * tocar el servicio.
 */
@WebMvcTest(ConsultaEstadoController.class)
@DisplayName("Controlador de consulta de estado")
class ConsultaEstadoControllerTest {

    private static final String RUTA_CON_SOLICITUDES = "/api/solicitudes/CC/1234567890";
    private static final String RUTA_SIN_SOLICITUDES = "/api/solicitudes/CE/999999999";
    private static final String RUTA_CON_TIPO_INVALIDO = "/api/solicitudes/XX/1234567890";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultaEstadoService consultaEstadoService;

    @Test
    @DisplayName("GET con solicitudes responde 200 con la lista en el orden del servicio")
    void consultarEstado_conSolicitudesDelDocumento_devuelve200ConLaLista() throws Exception {
        when(consultaEstadoService.consultarPorDocumento(TipoDocumento.CC, "1234567890"))
                .thenReturn(List.of(crearElementoConTasa(), crearElementoSinTasa()));

        mockMvc.perform(get(RUTA_CON_SOLICITUDES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idSolicitud").value("SOL-20260831-002"))
                .andExpect(jsonPath("$[0].fechaCreacion").value("2026-08-31T09:00:00"))
                .andExpect(jsonPath("$[0].estado").value("APROBADO"))
                .andExpect(jsonPath("$[0].montoSolicitado").value(15000000))
                .andExpect(jsonPath("$[0].plazoMeses").value(36))
                .andExpect(jsonPath("$[0].tasaEstimada").value(1.20))
                .andExpect(jsonPath("$[1].idSolicitud").value("SOL-20260830-001"))
                .andExpect(jsonPath("$[1].estado").value("RECHAZADO"))
                // Sin oferta no hay tasa, y lo que no aplica no viaja como null
                .andExpect(jsonPath("$[1].tasaEstimada").doesNotExist());

        verify(consultaEstadoService).consultarPorDocumento(TipoDocumento.CC, "1234567890");
    }

    @Test
    @DisplayName("GET de un documento sin solicitudes responde 200 con lista vacia, no 404")
    void consultarEstado_sinSolicitudes_devuelve200ConListaVacia() throws Exception {
        when(consultaEstadoService.consultarPorDocumento(TipoDocumento.CE, "999999999"))
                .thenReturn(List.of());

        mockMvc.perform(get(RUTA_SIN_SOLICITUDES))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("GET con tipo de documento invalido responde 400 con location=path sin tocar el servicio")
    void consultarEstado_conTipoDeDocumentoInvalido_devuelve400ConLocationPath() throws Exception {
        mockMvc.perform(get(RUTA_CON_TIPO_INVALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error en los datos proporcionados"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("tipoDocumento"))
                .andExpect(jsonPath("$.errors[0].location").value("path"));

        verify(consultaEstadoService, never()).consultarPorDocumento(any(), anyString());
    }

    private static EstadoDeSolicitudResponse crearElementoConTasa() {
        return new EstadoDeSolicitudResponse("SOL-20260831-002", LocalDateTime.of(2026, 8, 31, 9, 0),
                "APROBADO", new BigDecimal("15000000"), (short) 36, new BigDecimal("1.20"));
    }

    private static EstadoDeSolicitudResponse crearElementoSinTasa() {
        return new EstadoDeSolicitudResponse("SOL-20260830-001", LocalDateTime.of(2026, 8, 30, 10, 30),
                "RECHAZADO", new BigDecimal("20000000"), (short) 24, null);
    }
}
