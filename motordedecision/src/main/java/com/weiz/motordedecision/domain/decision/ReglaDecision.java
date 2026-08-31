package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;

/**
 * Una fila de la tabla de decision del enunciado: una condicion y el estado al
 * que lleva.
 *
 * Es el Strategy sobre el que se monta {@link MotorDeDecision}. Cada regla se
 * lee sola —dice cuando aplica y que decide— y ninguna sabe de las demas, asi
 * que anadir una fila a la tabla es escribir una clase mas y sumarla a la lista
 * del motor: ninguna de las existentes se toca (OCP). Es lo que evita la
 * escalera de {@code if} anidados que la tabla invita a escribir.
 *
 * Java puro, sin anotaciones de framework: los umbrales que cada regla aplica
 * entran por su constructor (docs/architecture.md §3, regla 2).
 */
public interface ReglaDecision {

    /**
     * Indica si esta regla es la que decide el caso.
     *
     * @param caso solicitud evaluada con el rastro de la cadena
     * @return {@code true} si la condicion de la fila se cumple
     */
    boolean cumpleCondicion(CasoDeDecision caso);

    /**
     * Devuelve el estado al que lleva esta regla cuando aplica.
     *
     * @return estado final de la solicitud
     */
    EstadoSolicitud obtenerEstado();
}
