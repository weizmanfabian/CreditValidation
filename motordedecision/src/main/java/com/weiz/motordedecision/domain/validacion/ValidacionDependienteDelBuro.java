package com.weiz.motordedecision.domain.validacion;

/**
 * Una validacion que no puede pronunciarse sin el informe del buro.
 *
 * El enunciado reserva un estado propio para el buro caido
 * ({@code PENDIENTE_REVISION}), asi que una validacion sin informe no debe
 * rechazar: debe callarse. Si leyera el informe ausente veria un score cero y
 * sin reportes negativos —los valores de relleno de
 * {@code ResultadoConsultaBuro}— y convertiria una falla del servicio externo
 * en un rechazo al solicitante.
 *
 * La condicion vive aqui, en una sola linea compartida, en vez de repetida en
 * cada implementacion que dependa del buro.
 */
public interface ValidacionDependienteDelBuro extends ValidacionCrediticia {

    @Override
    default boolean puedeEvaluarse(SolicitudEvaluada solicitud) {
        return solicitud.informeBuro().hayInformeDisponible();
    }
}
