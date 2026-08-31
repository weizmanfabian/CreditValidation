/**
 * Cliente HTTP del buro de credito.
 *
 * Unico punto del motor que sabe que el buro se alcanza por HTTP: aqui viven el
 * timeout, el reintento con backoff exponencial y jitter, el circuit breaker y
 * el fallback. Los servicios reciben un resultado de dominio, jamas una
 * excepcion de transporte (docs/architecture.md §3).
 */
package com.weiz.motordedecision.infraestructura.client;
