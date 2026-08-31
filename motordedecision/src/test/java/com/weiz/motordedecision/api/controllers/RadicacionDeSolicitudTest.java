package com.weiz.motordedecision.api.controllers;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import com.weiz.motordedecision.domain.dataaccessors.SolicitudDataAccessor;
import com.weiz.motordedecision.domain.entities.ResultadoValidacion;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.infraestructura.client.BuroClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El flujo de radicacion completo por MockMvc: contexto real, cadena real,
 * reglas reales y persistencia real sobre H2. Lo unico simulado es el
 * {@link BuroClient}, que es la frontera HTTP con el otro servicio; su cliente
 * de verdad ya se probo contra un servidor real en {@code BuroClientHttpTest}.
 *
 * Aqui se fijan tres criterios de la feature: el estado que responde cada
 * escenario del enunciado, que el buro caido termina en 200 con
 * {@code PENDIENTE_REVISION} y nunca en un 5xx, y que la solicitud y su rastro
 * de validaciones quedan persistidos.
 *
 * Cada test corre dentro de una transaccion que se revierte al terminar: la
 * base queda limpia y el consecutivo del dia arranca de nuevo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Radicacion de solicitudes de punta a punta")
class RadicacionDeSolicitudTest {

    private static final String RUTA_SOLICITUDES = "/api/solicitudes";
    private static final String PATRON_ID_SOLICITUD = "SOL-\\d{8}-\\d{3}";

    private static final String TIPO_DOCUMENTO = "CC";
    private static final String DOCUMENTO_APROBADO = "1234567890";
    private static final String DOCUMENTO_BLOQUEADO = "1010101010";
    private static final int SCORE_ALTO = 750;
    private static final int SCORE_BAJO = 381;
    private static final LocalDateTime FECHA_CONSULTA = LocalDateTime.of(2026, 8, 31, 10, 30);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitudDataAccessor solicitudDataAccessor;

    @MockitoBean
    private BuroClient buroClient;

    @Test
    @DisplayName("Score alto y monto dentro del multiplo: 200 APROBADO con tasa y siguiente paso")
    void radicarSolicitud_conScoreAltoYMontoDentroDelMultiplo_respondeAprobado() throws Exception {
        simularInforme(DOCUMENTO_APROBADO, SCORE_ALTO, "ACTIVO", false);

        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearCuerpo(DOCUMENTO_APROBADO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSolicitud", matchesPattern(PATRON_ID_SOLICITUD)))
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.solicitante.nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.detalle.tasaEstimada").value(1.2))
                .andExpect(jsonPath("$.evaluacion.scoreBureau").value(SCORE_ALTO))
                .andExpect(jsonPath("$.evaluacion.validaciones", hasSize(4)))
                .andExpect(jsonPath("$.siguientePaso")
                        .value("Se enviará contrato al correo registrado en 24 horas"));
    }

    @Test
    @DisplayName("Aprobado: la solicitud queda persistida con sus datos, su score y sus cuatro validaciones")
    void radicarSolicitud_conSolicitudAprobada_persisteLaSolicitudYSuRastro() throws Exception {
        simularInforme(DOCUMENTO_APROBADO, SCORE_ALTO, "ACTIVO", false);

        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearCuerpo(DOCUMENTO_APROBADO)))
                .andExpect(status().isOk());

        Solicitud persistida = buscarLaUnicaSolicitudDe(DOCUMENTO_APROBADO);
        assertThat(persistida)
                .returns("APROBADO", Solicitud::getEstado)
                .returns(SCORE_ALTO, Solicitud::getScoreBuro)
                .returns("Juan", Solicitud::getNombres)
                .returns("Pérez", Solicitud::getApellidos)
                .returns((short) 36, Solicitud::getPlazoMeses);
        assertThat(persistida.getMontoSolicitado()).isEqualByComparingTo("15000000");
        assertThat(persistida.getTasaEstimada()).isEqualByComparingTo("1.2");
        assertThat(persistida.getResultados())
                .extracting(ResultadoValidacion::getOrden, ResultadoValidacion::getNombre,
                        ResultadoValidacion::getResultado)
                .containsExactly(
                        tuple((short) 1, "Identidad", "APROBADO"),
                        tuple((short) 2, "Score", "APROBADO"),
                        tuple((short) 3, "Capacidad de pago", "APROBADO"),
                        tuple((short) 4, "Reporte negativo", "APROBADO"));
    }

    @Test
    @DisplayName("Score por debajo del corte: 200 RECHAZADO, sin tasa y con el rastro hasta el corte")
    void radicarSolicitud_conScorePorDebajoDelCorte_respondeRechazadoYPersisteElCorte() throws Exception {
        simularInforme(DOCUMENTO_APROBADO, SCORE_BAJO, "EN_MORA", true);

        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearCuerpo(DOCUMENTO_APROBADO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.detalle.tasaEstimada").doesNotExist())
                .andExpect(jsonPath("$.evaluacion.validaciones", hasSize(2)))
                .andExpect(jsonPath("$.evaluacion.validaciones[1].nombre").value("Score"))
                .andExpect(jsonPath("$.evaluacion.validaciones[1].resultado").value("RECHAZADO"))
                .andExpect(jsonPath("$.evaluacion.validaciones[1].detalle").value("Score 381 < 600"));

        Solicitud persistida = buscarLaUnicaSolicitudDe(DOCUMENTO_APROBADO);
        assertThat(persistida.getEstado()).isEqualTo("RECHAZADO");
        assertThat(persistida.getResultados()).hasSize(2);
    }

    @Test
    @DisplayName("Buro caido: 200 con PENDIENTE_REVISION y sin score, nunca un 5xx")
    void radicarSolicitud_conElBuroCaido_responde200ConPendienteRevision() throws Exception {
        when(buroClient.consultarInforme(eq(TIPO_DOCUMENTO), anyString()))
                .thenReturn(ResultadoConsultaBuro.crearResultadoBuroNoDisponible());

        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearCuerpo(DOCUMENTO_APROBADO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.evaluacion.scoreBureau").doesNotExist())
                .andExpect(jsonPath("$.siguientePaso")
                        .value("Un analista revisará la solicitud y le informaremos el resultado"));

        // Sin informe la cadena se detiene tras Identidad y no registra Score (D-041)
        Solicitud persistida = buscarLaUnicaSolicitudDe(DOCUMENTO_APROBADO);
        assertThat(persistida)
                .returns("PENDIENTE_REVISION", Solicitud::getEstado)
                .returns(null, Solicitud::getScoreBuro);
        assertThat(persistida.getResultados())
                .extracting(ResultadoValidacion::getNombre)
                .containsExactly("Identidad");
    }

    @Test
    @DisplayName("Documento bloqueado: 200 RECHAZADO_FRAUDE sin tasa ni siguiente paso")
    void radicarSolicitud_conDocumentoBloqueado_respondeRechazadoFraude() throws Exception {
        simularInforme(DOCUMENTO_BLOQUEADO, SCORE_ALTO, "ACTIVO", false);

        mockMvc.perform(post(RUTA_SOLICITUDES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearCuerpo(DOCUMENTO_BLOQUEADO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO_FRAUDE"))
                .andExpect(jsonPath("$.detalle.tasaEstimada").doesNotExist())
                .andExpect(jsonPath("$.siguientePaso").doesNotExist())
                .andExpect(jsonPath("$.evaluacion.validaciones", hasSize(1)))
                .andExpect(jsonPath("$.evaluacion.validaciones[0].detalle")
                        .value("Documento reportado en la lista de bloqueados"));

        assertThat(buscarLaUnicaSolicitudDe(DOCUMENTO_BLOQUEADO).getEstado()).isEqualTo("RECHAZADO_FRAUDE");
    }

    private void simularInforme(String numeroDocumento, int score, String estadoTitular, boolean hayReporteNegativo) {
        when(buroClient.consultarInforme(TIPO_DOCUMENTO, numeroDocumento))
                .thenReturn(ResultadoConsultaBuro.crearResultadoConInforme(
                        score, estadoTitular, hayReporteNegativo, FECHA_CONSULTA));
    }

    private Solicitud buscarLaUnicaSolicitudDe(String numeroDocumento) {
        List<Solicitud> solicitudes = solicitudDataAccessor.buscarPorDocumento(TIPO_DOCUMENTO, numeroDocumento);

        assertThat(solicitudes).hasSize(1);
        return solicitudes.get(0);
    }

    private static String crearCuerpo(String numeroDocumento) {
        return """
                {
                  "tipoDocumento": "CC",
                  "numeroDocumento": "%s",
                  "nombres": "Juan",
                  "apellidos": "Pérez",
                  "correo": "juan.perez@example.com",
                  "celular": "3001234567",
                  "montoSolicitado": 15000000,
                  "plazoMeses": 36,
                  "ingresosMensuales": 4000000
                }
                """.formatted(numeroDocumento);
    }
}
