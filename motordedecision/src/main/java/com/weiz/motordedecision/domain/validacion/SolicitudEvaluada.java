package com.weiz.motordedecision.domain.validacion;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;

import java.math.BigDecimal;

/**
 * Todo lo que la cadena necesita saber para evaluar una solicitud.
 *
 * Es deliberadamente mas pequena que la entidad {@code Solicitud}: aqui solo
 * viajan los datos que alguna validacion mira. Nombres, correo y celular no
 * deciden nada, asi que no entran —y de paso la cadena deja de manipular datos
 * personales que no necesita.
 *
 * Lleva el informe del buro ya resuelto por
 * {@code infraestructura/client}: las validaciones leen un valor de dominio y
 * nunca saben que detras hubo HTTP (docs/architecture.md §3, regla 4).
 *
 * @param numeroDocumento numero de documento del solicitante
 * @param montoSolicitado monto de credito pedido
 * @param ingresosMensuales ingresos mensuales declarados
 * @param informeBuro resultado de la consulta al buro, con o sin informe
 */
public record SolicitudEvaluada(

        String numeroDocumento,

        BigDecimal montoSolicitado,

        BigDecimal ingresosMensuales,

        ResultadoConsultaBuro informeBuro) {
}
