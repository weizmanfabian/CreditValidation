package com.weiz.motordedecision.api.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Las cifras del credito: lo que se pidio y, cuando lo hay, a que tasa.
 *
 * La {@code tasaEstimada} es opcional a proposito. Un rechazo temprano no
 * ofrece credito, asi que no tiene tasa que ofrecer, y al serializarse con
 * {@code NON_NULL} el campo no viaja (D-045). El monto y el plazo si viajan
 * siempre: son lo que el solicitante pidio, exista oferta o no.
 *
 * @param montoSolicitado monto de credito pedido
 * @param plazoMeses plazo pedido, en meses
 * @param tasaEstimada tasa mensual ofrecida, o {@code null} si no hay oferta
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeccionDetalleFinanciero(BigDecimal montoSolicitado, Short plazoMeses, BigDecimal tasaEstimada) {
}
