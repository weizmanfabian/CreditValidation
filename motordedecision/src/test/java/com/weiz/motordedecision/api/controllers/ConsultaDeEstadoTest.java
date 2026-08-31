package com.weiz.motordedecision.api.controllers;

import com.weiz.motordedecision.domain.dataaccessors.SolicitudDataAccessor;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.infraestructura.client.BuroClient;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La consulta de estado de punta a punta: contexto real, repositorio real y
 * H2. Se siembran solicitudes con el accessor y se consulta por MockMvc.
 *
 * Aqui se fija lo que la rebanada web no puede: que el par tipo y numero
 * filtra de verdad —el mismo numero con otro tipo no aparece—, que el orden es
 * de la mas reciente a la mas antigua (D-054) y que la tasa que responde la
 * consulta es la sellada en la columna al radicar, no la vigente en
 * configuracion (D-051).
 *
 * El {@link BuroClient} se dobla solo para compartir el contexto cacheado de
 * {@code RadicacionDeSolicitudTest}; la consulta no lo toca, y eso tambien se
 * verifica.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Consulta de estado de punta a punta")
class ConsultaDeEstadoTest {

    private static final String TIPO_CC = "CC";
    private static final String TIPO_CE = "CE";
    private static final String DOCUMENTO_CON_SOLICITUDES = "1234567890";
    private static final String DOCUMENTO_SIN_SOLICITUDES = "999999999";

    private static final LocalDateTime FECHA_MAS_ANTIGUA = LocalDateTime.of(2026, 8, 30, 10, 0);
    private static final LocalDateTime FECHA_MAS_RECIENTE = LocalDateTime.of(2026, 8, 31, 9, 0);

    /**
     * La tasa vigente del estado APROBADO en application.yml es 1.2: si la
     * consulta respondiera la configuracion en vez de la columna, este valor
     * la delataria.
     */
    private static final BigDecimal TASA_SELLADA_DISTINTA_DE_LA_VIGENTE = new BigDecimal("9.99");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitudDataAccessor solicitudDataAccessor;

    @MockitoBean
    private BuroClient buroClient;

    @Test
    @DisplayName("Varias solicitudes del documento: las lista de la mas reciente a la mas antigua")
    void consultarEstado_conVariasSolicitudesDelDocumento_lasListaDeLaMasRecienteALaMasAntigua()
            throws Exception {

        sembrarSolicitud("SOL-20260830-901", TIPO_CC, DOCUMENTO_CON_SOLICITUDES,
                EstadoSolicitud.APROBADO, new BigDecimal("1.20"), FECHA_MAS_ANTIGUA);
        sembrarSolicitud("SOL-20260831-901", TIPO_CC, DOCUMENTO_CON_SOLICITUDES,
                EstadoSolicitud.RECHAZADO, null, FECHA_MAS_RECIENTE);
        sembrarSolicitud("SOL-20260831-902", TIPO_CE, DOCUMENTO_CON_SOLICITUDES,
                EstadoSolicitud.PREAPROBADO, new BigDecimal("1.80"), FECHA_MAS_RECIENTE);

        mockMvc.perform(get("/api/solicitudes/CC/" + DOCUMENTO_CON_SOLICITUDES))
                .andExpect(status().isOk())
                // El mismo numero con tipo CE no cuenta: el filtro es por el par
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idSolicitud").value("SOL-20260831-901"))
                .andExpect(jsonPath("$[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$[0].fechaCreacion").value("2026-08-31T09:00:00"))
                .andExpect(jsonPath("$[0].tasaEstimada").doesNotExist())
                .andExpect(jsonPath("$[1].idSolicitud").value("SOL-20260830-901"))
                .andExpect(jsonPath("$[1].estado").value("APROBADO"))
                .andExpect(jsonPath("$[1].fechaCreacion").value("2026-08-30T10:00:00"))
                .andExpect(jsonPath("$[1].tasaEstimada").value(1.20));

        verifyNoInteractions(buroClient);
    }

    @Test
    @DisplayName("Documento sin solicitudes: 200 con lista vacia, no 404")
    void consultarEstado_deUnDocumentoSinSolicitudes_devuelve200ConListaVacia() throws Exception {
        mockMvc.perform(get("/api/solicitudes/CC/" + DOCUMENTO_SIN_SOLICITUDES))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("La tasa que responde es la sellada en la columna, no la vigente en configuracion")
    void consultarEstado_conTasaSelladaDistintaDeLaVigente_respondeLaDeLaColumna() throws Exception {
        sembrarSolicitud("SOL-20260831-903", TIPO_CC, DOCUMENTO_CON_SOLICITUDES,
                EstadoSolicitud.APROBADO, TASA_SELLADA_DISTINTA_DE_LA_VIGENTE, FECHA_MAS_RECIENTE);

        mockMvc.perform(get("/api/solicitudes/CC/" + DOCUMENTO_CON_SOLICITUDES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tasaEstimada").value(9.99));
    }

    private void sembrarSolicitud(String idSolicitud,
                                  String tipoDocumento,
                                  String numeroDocumento,
                                  EstadoSolicitud estado,
                                  BigDecimal tasaEstimada,
                                  LocalDateTime fechaCreacion) {

        solicitudDataAccessor.guardarSolicitud(Solicitud.builder()
                .idSolicitud(idSolicitud)
                .tipoDocumento(tipoDocumento)
                .numeroDocumento(numeroDocumento)
                .nombres("Juan")
                .apellidos("Perez")
                .correoElectronico("juan.perez@example.com")
                .telefonoCelular("3001234567")
                .montoSolicitado(new BigDecimal("15000000"))
                .plazoMeses((short) 36)
                .ingresosMensuales(new BigDecimal("4000000"))
                .estado(estado.name())
                .tasaEstimada(tasaEstimada)
                .fechaCreacion(fechaCreacion)
                .build());
    }
}
