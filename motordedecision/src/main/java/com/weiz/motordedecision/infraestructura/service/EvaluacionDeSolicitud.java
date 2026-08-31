package com.weiz.motordedecision.infraestructura.service;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import com.weiz.motordedecision.domain.validacion.RegistroValidacion;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;

import java.util.List;

/**
 * Lo que queda despues de evaluar una solicitud y antes de guardarla: el
 * estado decidido, lo que dijo el buro y el rastro de la cadena.
 *
 * Es un valor de paso entre las dos mitades de la radicacion —evaluar y
 * registrar—, y por eso no viaja al cliente: la respuesta se arma en
 * {@code mapper} a partir de la solicitud ya persistida.
 *
 * @param estado estado final que decidieron las reglas
 * @param informeBuro resultado de la consulta al buro, con o sin informe
 * @param validaciones registros de la cadena, en orden de ejecucion
 */
public record EvaluacionDeSolicitud(

        EstadoSolicitud estado,

        ResultadoConsultaBuro informeBuro,

        List<RegistroValidacion> validaciones) {

    public EvaluacionDeSolicitud {
        validaciones = List.copyOf(validaciones);
    }
}
