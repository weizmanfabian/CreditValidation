package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;

/**
 * Primera fila de la tabla: documento en lista bloqueada, RECHAZADO_FRAUDE.
 *
 * Va antes que la del buro caido a proposito. Un documento bloqueado se
 * rechaza con el buro en pie o de rodillas: la validacion de identidad no
 * consulta nada, asi que su rechazo ya esta en el rastro aunque el resto de la
 * cadena no llegara a ejecutarse.
 *
 * El nombre de la validacion cuyo rechazo significa fraude entra por el
 * constructor. La regla no menciona a {@code ValidacionIdentidad} ni la
 * importa: solo sabe que un rechazo con ese nombre no es un rechazo
 * corriente.
 */
public class ReglaRechazoPorFraude implements ReglaDecision {

    private final String nombreValidacionDeIdentidad;

    /**
     * @param nombreValidacionDeIdentidad nombre con el que la validacion de
     *                                    identidad registra su veredicto
     */
    public ReglaRechazoPorFraude(String nombreValidacionDeIdentidad) {
        this.nombreValidacionDeIdentidad = nombreValidacionDeIdentidad;
    }

    @Override
    public boolean cumpleCondicion(CasoDeDecision caso) {
        return caso.existeRechazoDe(nombreValidacionDeIdentidad);
    }

    @Override
    public EstadoSolicitud obtenerEstado() {
        return EstadoSolicitud.RECHAZADO_FRAUDE;
    }
}
