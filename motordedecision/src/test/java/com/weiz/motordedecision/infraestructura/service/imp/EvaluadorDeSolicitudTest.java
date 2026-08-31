package com.weiz.motordedecision.infraestructura.service.imp;

import com.weiz.motordedecision.api.models.request.PeticionesDePrueba;
import com.weiz.motordedecision.api.models.request.SolicitudCreditoRequest;
import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import com.weiz.motordedecision.domain.decision.CasoDeDecision;
import com.weiz.motordedecision.domain.decision.MotorDeDecision;
import com.weiz.motordedecision.domain.validacion.CadenaDeValidaciones;
import com.weiz.motordedecision.domain.validacion.RegistroValidacion;
import com.weiz.motordedecision.domain.validacion.SolicitudEvaluada;
import com.weiz.motordedecision.infraestructura.client.BuroClient;
import com.weiz.motordedecision.infraestructura.service.EvaluacionDeSolicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El evaluador junta tres piezas que ya existen —buro, cadena y reglas— y lo
 * unico suyo es el orden y el traslado de datos entre ellas. Eso es lo que se
 * fija aqui con dobles: que la cadena recibe la solicitud CON el informe que
 * respondio el buro, que las reglas deciden sobre el rastro que dejo la cadena
 * y que la evaluacion resultante conserva las tres cosas.
 *
 * Que el buro caido termina en {@code PENDIENTE_REVISION} no se decide aqui:
 * lo deciden las reglas, y de punta a punta lo prueba
 * {@code RadicacionDeSolicitudTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Evaluador de solicitudes")
class EvaluadorDeSolicitudTest {

    private static final LocalDateTime FECHA_CONSULTA = LocalDateTime.of(2026, 8, 31, 10, 30);
    private static final ResultadoConsultaBuro INFORME = ResultadoConsultaBuro.crearResultadoConInforme(
            750, "ACTIVO", false, FECHA_CONSULTA);
    private static final List<RegistroValidacion> RASTRO = List.of(
            new RegistroValidacion((short) 1, "Identidad", ResultadoEvaluacion.APROBADO, "Documento no bloqueado"));

    @Mock
    private BuroClient buroClient;

    @Mock
    private CadenaDeValidaciones cadenaDeValidaciones;

    @Mock
    private MotorDeDecision motorDeDecision;

    @InjectMocks
    private EvaluadorDeSolicitud evaluadorDeSolicitud;

    @Test
    @DisplayName("La cadena evalua la solicitud con el informe que respondio el buro")
    void evaluarSolicitud_conElInformeDelBuro_loEntregaALaCadenaConLosDatosDeLaPeticion() {
        SolicitudCreditoRequest peticion = PeticionesDePrueba.crearPeticionValida();
        when(buroClient.consultarInforme("CC", peticion.numeroDocumento())).thenReturn(INFORME);
        when(cadenaDeValidaciones.evaluarSolicitud(any())).thenReturn(RASTRO);
        when(motorDeDecision.decidirEstado(any())).thenReturn(EstadoSolicitud.APROBADO);

        evaluadorDeSolicitud.evaluarSolicitud(peticion);

        ArgumentCaptor<SolicitudEvaluada> solicitud = ArgumentCaptor.forClass(SolicitudEvaluada.class);
        verify(cadenaDeValidaciones).evaluarSolicitud(solicitud.capture());
        assertThat(solicitud.getValue())
                .returns(peticion.numeroDocumento(), SolicitudEvaluada::numeroDocumento)
                .returns(peticion.montoSolicitado(), SolicitudEvaluada::montoSolicitado)
                .returns(peticion.ingresosMensuales(), SolicitudEvaluada::ingresosMensuales)
                .returns(INFORME, SolicitudEvaluada::informeBuro);
    }

    @Test
    @DisplayName("Las reglas deciden sobre el rastro que dejo la cadena, no sobre otro")
    void evaluarSolicitud_conElRastroDeLaCadena_seLoEntregaALasReglasDeDecision() {
        SolicitudCreditoRequest peticion = PeticionesDePrueba.crearPeticionValida();
        when(buroClient.consultarInforme("CC", peticion.numeroDocumento())).thenReturn(INFORME);
        when(cadenaDeValidaciones.evaluarSolicitud(any())).thenReturn(RASTRO);
        when(motorDeDecision.decidirEstado(any())).thenReturn(EstadoSolicitud.APROBADO);

        evaluadorDeSolicitud.evaluarSolicitud(peticion);

        ArgumentCaptor<CasoDeDecision> caso = ArgumentCaptor.forClass(CasoDeDecision.class);
        verify(motorDeDecision).decidirEstado(caso.capture());
        assertThat(caso.getValue().validaciones()).isEqualTo(RASTRO);
    }

    @Test
    @DisplayName("La evaluacion conserva el estado decidido, el informe y el rastro")
    void evaluarSolicitud_conLasTresPiezasResueltas_conservaLasTresEnLaEvaluacion() {
        SolicitudCreditoRequest peticion = PeticionesDePrueba.crearPeticionValida();
        when(buroClient.consultarInforme("CC", peticion.numeroDocumento())).thenReturn(INFORME);
        when(cadenaDeValidaciones.evaluarSolicitud(any())).thenReturn(RASTRO);
        when(motorDeDecision.decidirEstado(any())).thenReturn(EstadoSolicitud.PREAPROBADO);

        EvaluacionDeSolicitud evaluacion = evaluadorDeSolicitud.evaluarSolicitud(peticion);

        assertThat(evaluacion)
                .returns(EstadoSolicitud.PREAPROBADO, EvaluacionDeSolicitud::estado)
                .returns(INFORME, EvaluacionDeSolicitud::informeBuro)
                .returns(RASTRO, EvaluacionDeSolicitud::validaciones);
    }
}
