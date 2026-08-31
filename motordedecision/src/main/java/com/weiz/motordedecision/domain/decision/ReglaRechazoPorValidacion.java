package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;

/**
 * Tercera fila de la tabla: falla en la validacion de score o de reporte
 * negativo, RECHAZADO.
 *
 * No nombra a ninguna validacion concreta. El enunciado enumera las dos que
 * pueden rechazar una vez descartado el fraude, pero la condicion que importa
 * es "alguna validacion rechazo": si manana se anade una quinta validacion, su
 * rechazo cae aqui sin tocar esta clase.
 *
 * Depende de ir detras de {@link ReglaRechazoPorFraude}, que es quien aparta
 * antes el unico rechazo con estado propio.
 */
public class ReglaRechazoPorValidacion implements ReglaDecision {

    @Override
    public boolean cumpleCondicion(CasoDeDecision caso) {
        return caso.existeRechazo();
    }

    @Override
    public EstadoSolicitud obtenerEstado() {
        return EstadoSolicitud.RECHAZADO;
    }
}
