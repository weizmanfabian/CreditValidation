package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.SeccionSolicitante;
import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;

/**
 * Aporta la seccion {@code solicitante}, que viaja en todos los estados: quien
 * pidio el credito no depende de como termine la evaluacion.
 *
 * Une los dos pares de columnas que el enunciado publica como un solo campo:
 * nombres con apellidos, y tipo con numero de documento.
 */
public class AportanteDeSolicitante implements AportanteDeSeccion {

    private static final String SEPARADOR = " ";

    @Override
    public void aportarA(SolicitudCreditoResponse.Builder constructor, Solicitud solicitud, EstadoSolicitud estado) {
        constructor.agregarSolicitante(new SeccionSolicitante(
                solicitud.getNombres() + SEPARADOR + solicitud.getApellidos(),
                solicitud.getTipoDocumento() + SEPARADOR + solicitud.getNumeroDocumento()));
    }
}
