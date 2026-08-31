package com.weiz.buro.api.controllers;

import com.weiz.buro.api.models.request.ConsultaBuroRequest;
import com.weiz.buro.api.models.response.InformeCrediticioResponse;
import com.weiz.buro.domain.informe.SimuladorConsultaBuro;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la consulta del buro simulado: {@code POST /api/buro/consulta}.
 *
 * Recibe, valida el formato con {@code @Valid} —de un cuerpo invalido se ocupa
 * el {@code ControllerExceptionHandler}, que responde 400 con el
 * {@code ErrorResponse} del modulo— y delega en el dominio. No decide nada: ni
 * la paridad del documento ni el caso del servicio caido se resuelven aqui
 * (regla 1 de {@code docs/architecture.md} §3).
 */
@RestController
@RequestMapping("/api/buro")
public class ConsultaBuroController {

    private final SimuladorConsultaBuro simuladorConsultaBuro;

    public ConsultaBuroController(SimuladorConsultaBuro simuladorConsultaBuro) {
        this.simuladorConsultaBuro = simuladorConsultaBuro;
    }

    /**
     * Consulta el informe crediticio del documento recibido en el cuerpo.
     *
     * @param consulta tipo y numero de documento del titular
     * @return 200 con score, estado, reporte negativo y fecha de consulta
     */
    @PostMapping("/consulta")
    public InformeCrediticioResponse consultarInforme(@Valid @RequestBody ConsultaBuroRequest consulta) {
        return simuladorConsultaBuro.consultarInforme(consulta.numeroDocumento());
    }
}
