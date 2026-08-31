package com.weiz.motordedecision.infraestructura.service;

import com.weiz.motordedecision.api.models.request.SolicitudCreditoRequest;
import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;

/**
 * Radica una solicitud de credito: la evalua, la registra y responde.
 *
 * El controlador depende de esta interfaz y no de su implementacion, para que
 * la orquestacion pueda cambiar sin tocar la capa que expone el API
 * (docs/architecture.md §4).
 */
public interface SolicitudCreditoService {

    /**
     * Radica la solicitud recibida.
     *
     * Que la solicitud se rechace no es un fallo: un rechazo, un fraude o un
     * buro caido son respuestas validas con 200, no excepciones
     * ({@code docs/error-handling.md} §4.4).
     *
     * @param peticion datos del formulario, ya validados de formato
     * @return la respuesta compuesta, con las secciones que apliquen al estado
     */
    SolicitudCreditoResponse radicarSolicitud(SolicitudCreditoRequest peticion);
}
