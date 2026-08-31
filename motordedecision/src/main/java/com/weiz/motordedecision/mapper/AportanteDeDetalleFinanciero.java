package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.SeccionDetalleFinanciero;
import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aporta la seccion {@code detalle}: el monto y el plazo que se pidieron, mas
 * la tasa cuando el estado da derecho a una.
 *
 * La tasa no se calcula aqui ni se escribe en el codigo: entra por constructor
 * desde {@code respuesta.tasa-estimada-por-estado} (D-045). Un estado que no
 * este en ese mapa no tiene oferta, y la seccion viaja sin tasa —que es
 * exactamente lo que el enunciado pide para el rechazo temprano—.
 */
public class AportanteDeDetalleFinanciero implements AportanteDeSeccion {

    private final Map<EstadoSolicitud, BigDecimal> tasasPorEstado;

    /**
     * @param tasasPorEstado tasa ofrecida en cada estado; los estados ausentes
     *                       no llevan tasa
     */
    public AportanteDeDetalleFinanciero(Map<EstadoSolicitud, BigDecimal> tasasPorEstado) {
        this.tasasPorEstado = Map.copyOf(tasasPorEstado);
    }

    @Override
    public void aportarA(SolicitudCreditoResponse.Builder constructor, Solicitud solicitud, EstadoSolicitud estado) {
        constructor.agregarDetalle(new SeccionDetalleFinanciero(
                solicitud.getMontoSolicitado(),
                solicitud.getPlazoMeses(),
                tasasPorEstado.get(estado)));
    }
}
