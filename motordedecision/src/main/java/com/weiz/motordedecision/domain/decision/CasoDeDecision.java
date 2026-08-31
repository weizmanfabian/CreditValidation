package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.domain.validacion.RegistroValidacion;
import com.weiz.motordedecision.domain.validacion.SolicitudEvaluada;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;

import java.util.List;
import java.util.Optional;

/**
 * Todo lo que las reglas de decision necesitan saber: la solicitud y el rastro
 * que dejo la cadena de validaciones.
 *
 * Las reglas no vuelven a recorrer la lista de registros cada una por su
 * cuenta: preguntan a este caso, que es quien sabe leer el rastro. Asi una
 * regla nueva se escribe con predicados en lenguaje de negocio y no con
 * bucles.
 *
 * La invariante que hace fiable {@link #superaTodasLasValidaciones} esta en
 * {@code CadenaDeValidaciones}: la cadena solo se detiene antes de tiempo por
 * un rechazo o por falta de informe del buro (D-041). Sin ninguna de las dos
 * cosas, el rastro esta completo.
 *
 * @param solicitud datos de la solicitud evaluada, con el resultado del buro
 * @param validaciones registros de la cadena, en orden de ejecucion
 */
public record CasoDeDecision(SolicitudEvaluada solicitud, List<RegistroValidacion> validaciones) {

    public CasoDeDecision {
        validaciones = List.copyOf(validaciones);
    }

    /**
     * Indica si el buro respondio. Cuando no lo hizo, ninguna regla puede mirar
     * el score: no hay score que mirar.
     *
     * @return {@code true} si la consulta al buro trajo informe
     */
    public boolean hayInformeDelBuro() {
        return solicitud.informeBuro().hayInformeDisponible();
    }

    /**
     * Indica si alguna validacion rechazo la solicitud. Como la cadena se
     * detiene en la primera, a lo sumo hay una.
     *
     * @return {@code true} si el rastro contiene un rechazo
     */
    public boolean existeRechazo() {
        return buscarValidacionRechazada().isPresent();
    }

    /**
     * Indica si quien rechazo la solicitud fue una validacion concreta. Es lo
     * que separa un rechazo corriente de uno por documento bloqueado, que el
     * enunciado manda tratar aparte.
     *
     * @param nombreValidacion nombre de la validacion, tal como lo publica ella
     * @return {@code true} si el rechazo del rastro es de esa validacion
     */
    public boolean existeRechazoDe(String nombreValidacion) {
        return buscarValidacionRechazada()
                .filter(registro -> registro.nombre().equals(nombreValidacion))
                .isPresent();
    }

    /**
     * Indica si la solicitud recorrio la cadena entera sin que nadie la
     * rechazara, que es el punto de partida de las dos filas de aprobacion del
     * enunciado.
     *
     * @return {@code true} si hubo informe del buro y ninguna validacion rechazo
     */
    public boolean superaTodasLasValidaciones() {
        return hayInformeDelBuro() && !existeRechazo();
    }

    /**
     * Devuelve el score que reporto el buro. Solo tiene sentido cuando
     * {@link #hayInformeDelBuro} es cierto.
     *
     * @return score crediticio del titular
     */
    public int obtenerScore() {
        return solicitud.informeBuro().score();
    }

    private Optional<RegistroValidacion> buscarValidacionRechazada() {
        // La cadena corta en el primer rechazo, asi que basta con encontrarlo una vez
        return validaciones.stream()
                // Nos quedamos solo con los registros cuyo resultado es un rechazo
                .filter(registro -> registro.resultado() == ResultadoEvaluacion.RECHAZADO)
                // Y devolvemos el primero, que por construccion de la cadena es el unico
                .findFirst();
    }
}
