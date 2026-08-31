package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.EstadoDeSolicitudResponse;
import com.weiz.motordedecision.domain.entities.Solicitud;
import org.springframework.stereotype.Component;

/**
 * Convierte una solicitud persistida en el elemento de la lista de la
 * consulta de estado.
 *
 * Es deliberadamente mas simple que {@link MapeadorDeSolicitud}: aqui ninguna
 * seccion depende del estado, asi que no hay aportantes ni traduccion al enum.
 * El estado viaja tal como esta en la columna, cuyo catalogo ya lo cierra la
 * restriccion {@code ck_solicitud_estado} del esquema, y la tasa es la que se
 * sello al radicar (D-051), no la de la configuracion vigente.
 */
@Component
public class MapeadorDeConsultaDeEstado {

    /**
     * @param solicitud solicitud persistida, con su estado y su tasa sellada
     * @return el elemento de la lista con el sobre y las cifras del credito
     */
    public EstadoDeSolicitudResponse mapearAEstado(Solicitud solicitud) {
        return new EstadoDeSolicitudResponse(
                solicitud.getIdSolicitud(),
                solicitud.getFechaCreacion(),
                solicitud.getEstado(),
                solicitud.getMontoSolicitado(),
                solicitud.getPlazoMeses(),
                solicitud.getTasaEstimada());
    }
}
