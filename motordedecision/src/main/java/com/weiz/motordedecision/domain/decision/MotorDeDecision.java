package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;

import java.util.List;

/**
 * Resuelve el estado final recorriendo las reglas en orden: decide la primera
 * que aplica.
 *
 * El orden no lo decide Spring ni el orden de escaneo de los componentes: lo
 * decide la lista que recibe por constructor, que se arma en
 * {@code ConfiguracionReglasDeDecision} siguiendo el mismo patron que la
 * cadena de validaciones (D-040). Y aqui el orden es contrato, no estilo: mas
 * de una fila de la tabla puede cumplirse a la vez —un documento bloqueado con
 * el buro caido cumple dos— y la primera de la lista es la que manda (D-044).
 *
 * Es Java puro y no conoce ninguna regla concreta, asi que anadir una fila a la
 * tabla no obliga a tocar esta clase ni las existentes.
 *
 * El detalle esta en {@link #decidirEstado}.
 */
public class MotorDeDecision {

    private static final String SIN_REGLA_APLICABLE =
            "Ninguna regla de decision aplica al caso: la ultima de la lista debe cumplirse siempre";

    private final List<ReglaDecision> reglas;

    /**
     * @param reglas las reglas, en el orden exacto en que se evaluan; la ultima
     *               debe cumplirse siempre
     */
    public MotorDeDecision(List<ReglaDecision> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    /**
     * Decide en que estado queda la solicitud.
     *
     * @param caso solicitud evaluada con el rastro de la cadena
     * @return el estado de la primera regla que aplica
     * @throws IllegalStateException si ninguna regla aplica, que solo puede
     *                               pasar si la lista se armo sin su regla
     *                               final: es un defecto de configuracion, no
     *                               un caso de negocio
     */
    public EstadoSolicitud decidirEstado(CasoDeDecision caso) {
        // Recorremos las reglas en el orden recibido, que es el de la tabla del enunciado
        return reglas.stream()
                // Nos quedamos con las que aplican al caso
                .filter(regla -> regla.cumpleCondicion(caso))
                // La primera que aplica es la que decide: las siguientes ya no se consultan
                .findFirst()
                // De la regla nos interesa solo su estado
                .map(ReglaDecision::obtenerEstado)
                // Sin regla final que se cumpla siempre, la lista esta mal armada
                .orElseThrow(() -> new IllegalStateException(SIN_REGLA_APLICABLE));
    }
}
