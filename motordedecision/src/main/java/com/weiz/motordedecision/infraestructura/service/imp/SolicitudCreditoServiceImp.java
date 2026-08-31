package com.weiz.motordedecision.infraestructura.service.imp;

import com.weiz.motordedecision.api.models.request.SolicitudCreditoRequest;
import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.domain.dataaccessors.SolicitudDataAccessor;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.infraestructura.service.EvaluacionDeSolicitud;
import com.weiz.motordedecision.infraestructura.service.SolicitudCreditoService;
import com.weiz.motordedecision.mapper.EnsambladorDeSolicitud;
import com.weiz.motordedecision.mapper.MapeadorDeSolicitud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orquesta la radicacion de una solicitud de credito.
 *
 * Es el punto donde se juntan las piezas que ya existian: la evaluacion
 * ({@link EvaluadorDeSolicitud}), el ensamblado de la entidad
 * ({@link EnsambladorDeSolicitud}), la persistencia
 * ({@link SolicitudDataAccessor}) y el armado de la respuesta
 * ({@link MapeadorDeSolicitud}). No reimplementa ninguna: su responsabilidad es
 * el orden.
 *
 * No lleva {@code @Transactional} a proposito. La transaccion la abre y la
 * cierra {@code SolicitudDataAccessor.guardarSolicitud}, que es lo unico que
 * escribe; abrirla aqui la mantendria viva durante la consulta HTTP al buro
 * —segundos, con sus reintentos— reteniendo una conexion de la piscina sin
 * necesidad.
 *
 * El detalle del flujo esta en {@link #radicarSolicitud}.
 */
@Service
public class SolicitudCreditoServiceImp implements SolicitudCreditoService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudCreditoServiceImp.class);

    private final EvaluadorDeSolicitud evaluadorDeSolicitud;
    private final EnsambladorDeSolicitud ensambladorDeSolicitud;
    private final SolicitudDataAccessor solicitudDataAccessor;
    private final MapeadorDeSolicitud mapeadorDeSolicitud;

    public SolicitudCreditoServiceImp(EvaluadorDeSolicitud evaluadorDeSolicitud,
                                      EnsambladorDeSolicitud ensambladorDeSolicitud,
                                      SolicitudDataAccessor solicitudDataAccessor,
                                      MapeadorDeSolicitud mapeadorDeSolicitud) {

        this.evaluadorDeSolicitud = evaluadorDeSolicitud;
        this.ensambladorDeSolicitud = ensambladorDeSolicitud;
        this.solicitudDataAccessor = solicitudDataAccessor;
        this.mapeadorDeSolicitud = mapeadorDeSolicitud;
    }

    /**
     * Radica la solicitud en cuatro pasos: evaluar, ensamblar, guardar y
     * responder.
     *
     * Se guarda siempre, decida lo que decida el motor: el enunciado pide poder
     * consultar despues el estado de cualquier solicitud, tambien el de las
     * rechazadas. Si el guardado falla, la peticion falla con 500 y no se
     * responde una decision que no quedo registrada (D-049).
     *
     * @param peticion datos del formulario, ya validados de formato
     * @return la respuesta compuesta de la solicitud ya persistida
     */
    @Override
    public SolicitudCreditoResponse radicarSolicitud(SolicitudCreditoRequest peticion) {
        EvaluacionDeSolicitud evaluacion = evaluadorDeSolicitud.evaluarSolicitud(peticion);
        Solicitud solicitud = solicitudDataAccessor.guardarSolicitud(
                ensambladorDeSolicitud.ensamblarSolicitud(peticion, evaluacion));

        log.info("Solicitud {} radicada en estado {}", solicitud.getIdSolicitud(), evaluacion.estado());

        return mapeadorDeSolicitud.mapearARespuesta(solicitud);
    }
}
