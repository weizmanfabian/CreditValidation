package com.weiz.motordedecision.infraestructura.service.imp;

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
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decide en que estado queda una solicitud, sin guardar nada.
 *
 * Junta las tres piezas que ya existen y no reimplementa ninguna: el cliente
 * del buro (feature 9), la cadena de validaciones (feature 10) y las reglas de
 * decision (feature 11). Su unica responsabilidad es el orden en que se
 * consultan.
 *
 * El detalle esta en {@link #evaluarSolicitud}.
 */
@Component
public class EvaluadorDeSolicitud {

    private final BuroClient buroClient;
    private final CadenaDeValidaciones cadenaDeValidaciones;
    private final MotorDeDecision motorDeDecision;

    public EvaluadorDeSolicitud(BuroClient buroClient,
                                CadenaDeValidaciones cadenaDeValidaciones,
                                MotorDeDecision motorDeDecision) {

        this.buroClient = buroClient;
        this.cadenaDeValidaciones = cadenaDeValidaciones;
        this.motorDeDecision = motorDeDecision;
    }

    /**
     * Evalua la solicitud en tres pasos.
     *
     * Pasos:
     * - Consulta el buro. Si esta caido, el cliente no lanza nada: devuelve un
     *   resultado sin informe, y de ahi sale el {@code PENDIENTE_REVISION} con
     *   200 que pide el enunciado ({@code docs/error-handling.md} §4.4).
     * - Recorre la cadena de validaciones, que se detiene en la primera que
     *   rechaza o en la primera que no puede pronunciarse sin informe.
     * - Resuelve el estado con las reglas de decision, que deciden por la
     *   primera que aplica.
     *
     * @param peticion datos del formulario, ya validados de formato
     * @return el estado decidido con el informe y el rastro que lo sostienen
     */
    public EvaluacionDeSolicitud evaluarSolicitud(SolicitudCreditoRequest peticion) {
        ResultadoConsultaBuro informeBuro = buroClient.consultarInforme(
                peticion.tipoDocumento().name(), peticion.numeroDocumento());

        SolicitudEvaluada solicitud = new SolicitudEvaluada(peticion.numeroDocumento(),
                peticion.montoSolicitado(), peticion.ingresosMensuales(), informeBuro);

        List<RegistroValidacion> validaciones = cadenaDeValidaciones.evaluarSolicitud(solicitud);
        EstadoSolicitud estado = motorDeDecision.decidirEstado(new CasoDeDecision(solicitud, validaciones));

        return new EvaluacionDeSolicitud(estado, informeBuro, validaciones);
    }
}
