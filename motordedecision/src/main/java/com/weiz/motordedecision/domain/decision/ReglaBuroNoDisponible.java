package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;

/**
 * Segunda fila de la tabla: servicio de buro no disponible,
 * PENDIENTE_REVISION.
 *
 * El buro caido llega hasta aqui como un valor y no como una excepcion
 * (D-037), y la cadena se detuvo sin evaluar lo que dependia del informe
 * (D-041). Esta regla es la que convierte ese silencio en el estado que el
 * enunciado le reserva, en vez de en un rechazo al solicitante.
 *
 * No lleva configuracion: el buro respondio o no respondio, no hay umbral que
 * ajustar.
 */
public class ReglaBuroNoDisponible implements ReglaDecision {

    @Override
    public boolean cumpleCondicion(CasoDeDecision caso) {
        return !caso.hayInformeDelBuro();
    }

    @Override
    public EstadoSolicitud obtenerEstado() {
        return EstadoSolicitud.PENDIENTE_REVISION;
    }
}
