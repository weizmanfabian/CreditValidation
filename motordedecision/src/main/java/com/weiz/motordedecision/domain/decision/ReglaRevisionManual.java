package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;

/**
 * La fila que el enunciado no escribio: viable, pero sin alcanzar ninguna
 * aprobacion automatica. PENDIENTE_REVISION.
 *
 * Ocurre con un score entre 600 y 699 y un monto de mas de cinco veces los
 * ingresos: la cadena no la rechaza —el score llega al minimo y el monto cabe
 * en ocho veces los ingresos— pero no cumple ni la fila de APROBADO ni la de
 * PREAPROBADO. La tabla del enunciado deja ese hueco sin cubrir y alguien
 * tiene que decidirlo (D-043).
 *
 * Se cumple siempre: es la ultima de la lista y existe para que el motor
 * termine con un estado en todos los casos y no con una excepcion. Cualquier
 * regla escrita despues de esta seria codigo muerto.
 */
public class ReglaRevisionManual implements ReglaDecision {

    @Override
    public boolean cumpleCondicion(CasoDeDecision caso) {
        return true;
    }

    @Override
    public EstadoSolicitud obtenerEstado() {
        return EstadoSolicitud.PENDIENTE_REVISION;
    }
}
