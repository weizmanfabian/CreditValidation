package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;

import java.util.List;

/**
 * Convierte una solicitud persistida en la respuesta que ve el cliente.
 *
 * No sabe que secciones existen: pone el sobre —identificador, fecha y
 * estado— y deja que cada {@link AportanteDeSeccion} de la lista agregue la
 * suya si aplica. Por eso una seccion nueva no obliga a tocar esta clase ni las
 * secciones existentes: se escribe su aportante y se anade a la lista de
 * {@code ConfiguracionDeRespuesta}.
 *
 * El detalle esta en {@link #mapearARespuesta}.
 */
public class MapeadorDeSolicitud {

    private final List<AportanteDeSeccion> aportantes;

    /**
     * @param aportantes las secciones que se ofrecen a aportar; el orden no
     *                   altera el resultado, porque cada una escribe la suya
     */
    public MapeadorDeSolicitud(List<AportanteDeSeccion> aportantes) {
        this.aportantes = List.copyOf(aportantes);
    }

    /**
     * Arma la respuesta de una solicitud ya evaluada y persistida.
     *
     * @param solicitud solicitud con su estado, su score y su rastro
     * @return la respuesta con las secciones que aplican al caso
     */
    public SolicitudCreditoResponse mapearARespuesta(Solicitud solicitud) {
        EstadoSolicitud estado = EstadoSolicitud.valueOf(solicitud.getEstado());
        SolicitudCreditoResponse.Builder constructor = SolicitudCreditoResponse.crearConstructorPara(
                solicitud.getIdSolicitud(), solicitud.getFechaCreacion(), estado);

        // Cada aportante decide por su cuenta si su seccion aplica a este estado
        aportantes.forEach(aportante -> aportante.aportarA(constructor, solicitud, estado));

        return constructor.construir();
    }
}
