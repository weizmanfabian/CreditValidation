package com.weiz.motordedecision.api.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un elemento de la lista que responde la consulta de estado: una solicitud
 * del documento consultado, con su estado actual y su fecha (enunciado §5).
 *
 * Lleva el mismo sobre que la respuesta de radicacion —identificador, fecha y
 * estado— mas las tres cifras que permiten distinguir dos solicitudes del
 * mismo documento: monto, plazo y la tasa que se ofrecio al radicar. La tasa
 * sale de la columna, no de la configuracion vigente (D-051), y cuando no hubo
 * oferta no viaja como {@code null} sino que no viaja, igual que en
 * {@code SeccionDetalleFinanciero}. El rastro de validaciones no se incluye:
 * la consulta lista estados, no reevalua (D-052).
 *
 * @param idSolicitud identificador de negocio {@code SOL-yyyyMMdd-NNN}
 * @param fechaCreacion instante en que se radico la solicitud
 * @param estado estado actual, uno de los cinco del catalogo
 * @param montoSolicitado monto de credito pedido
 * @param plazoMeses plazo pedido, en meses
 * @param tasaEstimada tasa mensual ofrecida al radicar, o {@code null} si no hubo oferta
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"idSolicitud", "fechaCreacion", "estado", "montoSolicitado", "plazoMeses", "tasaEstimada"})
public record EstadoDeSolicitudResponse(

        String idSolicitud,

        LocalDateTime fechaCreacion,

        String estado,

        BigDecimal montoSolicitado,

        Short plazoMeses,

        BigDecimal tasaEstimada) {
}
