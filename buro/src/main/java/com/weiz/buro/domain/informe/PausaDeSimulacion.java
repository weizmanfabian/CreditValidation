package com.weiz.buro.domain.informe;

import java.time.Duration;

/**
 * Pausa que el buro simulado interpone antes de responder cuando le consultan el
 * documento que representa el servicio caido.
 *
 * Existe como abstraccion, y no como una llamada directa a
 * {@code Thread.sleep}, por una razon de verificacion: asi el test del
 * simulador comprueba que se pide exactamente la duracion configurada sin
 * esperarla de verdad, y ningun test queda atado al reloj de pared
 * ({@code docs/verification.md} §3).
 */
@FunctionalInterface
public interface PausaDeSimulacion {

    /**
     * Detiene el hilo que atiende la consulta durante la duracion indicada.
     *
     * @param duracion cuanto se retrasa la respuesta
     */
    void pausar(Duration duracion);
}
