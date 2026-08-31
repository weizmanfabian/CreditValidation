package com.weiz.buro.domain.informe;

import java.time.Duration;

/**
 * Implementacion real de {@link PausaDeSimulacion}: bloquea el hilo que atiende
 * la peticion.
 *
 * Bloquear el hilo de la peticion es, en general, un defecto —la regla §7.1 del
 * skill {@code clean-code-expert} lo prohibe en los reintentos—, pero aqui es
 * justamente lo que se simula: un servicio externo que se quedo colgado y hace
 * saltar el timeout del cliente. Sin bloqueo no hay caso caido que ejercitar en
 * el motor (decision D-020).
 */
public final class PausaBloqueante implements PausaDeSimulacion {

    @Override
    public void pausar(Duration duracion) {
        if (duracion == null || duracion.isZero() || duracion.isNegative()) {
            return;
        }

        try {
            Thread.sleep(duracion.toMillis());
        } catch (InterruptedException interrupcion) {
            // Se restaura la marca de interrupcion para que quien gestione el
            // hilo sepa que se pidio detenerlo; la consulta continua y responde.
            Thread.currentThread().interrupt();
        }
    }
}
