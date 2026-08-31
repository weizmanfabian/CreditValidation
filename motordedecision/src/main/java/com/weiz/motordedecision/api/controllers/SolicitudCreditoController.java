package com.weiz.motordedecision.api.controllers;

import com.weiz.motordedecision.api.models.request.SolicitudCreditoRequest;
import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.infraestructura.service.SolicitudCreditoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la radicacion de solicitudes: {@code POST /api/solicitudes}.
 *
 * Recibe, valida el formato con {@code @Valid} —de un cuerpo invalido se ocupa
 * el {@code ControllerExceptionHandler}, que responde 400 con todos los campos
 * rechazados— y delega en el servicio. No decide nada: ni consulta el buro, ni
 * ejecuta validaciones, ni resuelve el estado (regla 1 de
 * {@code docs/architecture.md} §3).
 */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudCreditoController {

    private final SolicitudCreditoService solicitudCreditoService;

    public SolicitudCreditoController(SolicitudCreditoService solicitudCreditoService) {
        this.solicitudCreditoService = solicitudCreditoService;
    }

    /**
     * Radica la solicitud recibida en el cuerpo.
     *
     * Responde 200 tambien cuando la solicitud se rechaza o el buro no
     * responde: son decisiones de negocio, no errores
     * ({@code docs/error-handling.md} §4.4).
     *
     * @param peticion datos del formulario de solicitud
     * @return 200 con la respuesta compuesta y su identificador de negocio
     */
    @PostMapping
    public SolicitudCreditoResponse radicarSolicitud(@Valid @RequestBody SolicitudCreditoRequest peticion) {
        return solicitudCreditoService.radicarSolicitud(peticion);
    }
}
