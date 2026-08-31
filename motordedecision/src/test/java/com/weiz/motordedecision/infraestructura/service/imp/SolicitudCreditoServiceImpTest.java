package com.weiz.motordedecision.infraestructura.service.imp;

import com.weiz.motordedecision.api.models.request.PeticionesDePrueba;
import com.weiz.motordedecision.api.models.request.SolicitudCreditoRequest;
import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import com.weiz.motordedecision.domain.dataaccessors.SolicitudDataAccessor;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.infraestructura.service.EvaluacionDeSolicitud;
import com.weiz.motordedecision.mapper.EnsambladorDeSolicitud;
import com.weiz.motordedecision.mapper.MapeadorDeSolicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.exceptions.tecnica.ErrorDePersistencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La orquestacion de la radicacion: evaluar, ensamblar, guardar y responder,
 * en ese orden y sin saltarse ninguno.
 *
 * El caso que mas importa fijar es el del guardado que falla despues de haber
 * decidido: la excepcion tecnica sube tal cual —el manejador respondera 500— y
 * la respuesta NO se arma, porque responder una decision que no quedo
 * registrada dejaria al cliente creyendo en una solicitud que no existe
 * (D-049).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de radicacion de solicitudes")
class SolicitudCreditoServiceImpTest {

    private static final LocalDateTime FECHA_CONSULTA = LocalDateTime.of(2026, 8, 31, 10, 30);
    private static final EvaluacionDeSolicitud EVALUACION = new EvaluacionDeSolicitud(
            EstadoSolicitud.APROBADO,
            ResultadoConsultaBuro.crearResultadoConInforme(750, "ACTIVO", false, FECHA_CONSULTA),
            List.of());

    @Mock
    private EvaluadorDeSolicitud evaluadorDeSolicitud;

    @Mock
    private EnsambladorDeSolicitud ensambladorDeSolicitud;

    @Mock
    private SolicitudDataAccessor solicitudDataAccessor;

    @Mock
    private MapeadorDeSolicitud mapeadorDeSolicitud;

    @InjectMocks
    private SolicitudCreditoServiceImp servicio;

    @Test
    @DisplayName("Radicar evalua, guarda la solicitud ensamblada y responde con la persistida")
    void radicarSolicitud_conElFlujoCompleto_guardaLaEnsambladaYRespondeConLaPersistida() {
        SolicitudCreditoRequest peticion = PeticionesDePrueba.crearPeticionValida();
        Solicitud ensamblada = new Solicitud();
        Solicitud persistida = crearSolicitudPersistida();
        SolicitudCreditoResponse respuestaEsperada = crearRespuesta();
        when(evaluadorDeSolicitud.evaluarSolicitud(peticion)).thenReturn(EVALUACION);
        when(ensambladorDeSolicitud.ensamblarSolicitud(peticion, EVALUACION)).thenReturn(ensamblada);
        when(solicitudDataAccessor.guardarSolicitud(ensamblada)).thenReturn(persistida);
        when(mapeadorDeSolicitud.mapearARespuesta(persistida)).thenReturn(respuestaEsperada);

        SolicitudCreditoResponse respuesta = servicio.radicarSolicitud(peticion);

        assertThat(respuesta).isSameAs(respuestaEsperada);
        verify(solicitudDataAccessor).guardarSolicitud(ensamblada);
    }

    @Test
    @DisplayName("Si el guardado falla tras decidir, la excepcion sube y no se responde nada (D-049)")
    void radicarSolicitud_conElGuardadoFallando_propagaLaExcepcionSinArmarRespuesta() {
        SolicitudCreditoRequest peticion = PeticionesDePrueba.crearPeticionValida();
        Solicitud ensamblada = new Solicitud();
        ErrorDePersistencia fallo = new ErrorDePersistencia("No se pudo guardar la solicitud",
                new DataAccessResourceFailureException("sin conexion"));
        when(evaluadorDeSolicitud.evaluarSolicitud(peticion)).thenReturn(EVALUACION);
        when(ensambladorDeSolicitud.ensamblarSolicitud(peticion, EVALUACION)).thenReturn(ensamblada);
        when(solicitudDataAccessor.guardarSolicitud(ensamblada)).thenThrow(fallo);

        assertThatThrownBy(() -> servicio.radicarSolicitud(peticion)).isSameAs(fallo);

        verify(mapeadorDeSolicitud, never()).mapearARespuesta(any());
    }

    private static Solicitud crearSolicitudPersistida() {
        return Solicitud.builder()
                .idSolicitud("SOL-20260831-001")
                .estado(EstadoSolicitud.APROBADO.name())
                .build();
    }

    private static SolicitudCreditoResponse crearRespuesta() {
        return SolicitudCreditoResponse
                .crearConstructorPara("SOL-20260831-001", FECHA_CONSULTA, EstadoSolicitud.APROBADO)
                .construir();
    }
}
