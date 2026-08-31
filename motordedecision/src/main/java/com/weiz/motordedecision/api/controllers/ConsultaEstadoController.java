package com.weiz.motordedecision.api.controllers;

import com.weiz.motordedecision.api.models.response.EstadoDeSolicitudResponse;
import com.weiz.motordedecision.infraestructura.service.ConsultaEstadoService;
import com.weiz.motordedecision.util.enums.TipoDocumento;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone la consulta de estado:
 * {@code GET /api/solicitudes/{tipoDocumento}/{numeroDocumento}}.
 *
 * La variable de ruta se declara como {@link TipoDocumento} a proposito: un
 * tipo fuera del catalogo no llega ni al servicio, la conversion falla y el
 * {@code ControllerExceptionHandler} responde 400 con {@code location=path}
 * ({@code docs/error-handling.md} §3). No decide nada: recibe, delega y
 * responde (regla 1 de {@code docs/architecture.md} §3).
 */
@RestController
@RequestMapping("/api/solicitudes")
public class ConsultaEstadoController {

    private final ConsultaEstadoService consultaEstadoService;

    public ConsultaEstadoController(ConsultaEstadoService consultaEstadoService) {
        this.consultaEstadoService = consultaEstadoService;
    }

    /**
     * Lista las solicitudes del documento con su estado actual y su fecha.
     *
     * Responde 200 tambien cuando no hay ninguna: la lista vacia es la
     * respuesta, no un error (D-053).
     *
     * @param tipoDocumento tipo de documento de la ruta, uno de CC, CE o PA
     * @param numeroDocumento numero de documento de la ruta
     * @return las solicitudes de la mas reciente a la mas antigua
     */
    @GetMapping("/{tipoDocumento}/{numeroDocumento}")
    public List<EstadoDeSolicitudResponse> consultarEstado(@PathVariable TipoDocumento tipoDocumento,
                                                           @PathVariable String numeroDocumento) {

        return consultaEstadoService.consultarPorDocumento(tipoDocumento, numeroDocumento);
    }
}
