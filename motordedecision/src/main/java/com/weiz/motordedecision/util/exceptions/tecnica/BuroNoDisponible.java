package com.weiz.motordedecision.util.exceptions.tecnica;

import com.weiz.motordedecision.util.exceptions.ExcepcionTecnica;
import org.springframework.web.client.RestClientException;

/**
 * El servicio de buro no respondio: se cayo, agoto el tiempo de espera o
 * devolvio un estado que el cliente no puede interpretar.
 *
 * Envuelve la {@link RestClientException} del cliente HTTP para que ninguna
 * excepcion de Spring escape de {@code infraestructura/client} hacia arriba
 * ({@code docs/conventions.md} §6). El constructor exige esa causa por tipo: no
 * hay forma de construirla perdiendo el origen del fallo.
 *
 * Que exista un manejador para ella es una red de seguridad, no la ruta
 * esperada. En el flujo de radicacion el buro caido lo absorbe el fallback de
 * Resilience4j y termina en una solicitud {@code PENDIENTE_REVISION} con 200,
 * porque es un escenario previsto del negocio
 * ({@code docs/error-handling.md} §4.4).
 */
public class BuroNoDisponible extends ExcepcionTecnica {

    /**
     * @param mensaje que se intentaba hacer contra el buro, para el log
     * @param causa fallo original del cliente HTTP
     */
    public BuroNoDisponible(String mensaje, RestClientException causa) {
        super(mensaje, causa);
    }
}
