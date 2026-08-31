package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;

import java.math.BigDecimal;

/**
 * Las dos filas de aprobacion de la tabla, que solo se diferencian en sus
 * numeros: score minimo y multiplo de los ingresos.
 *
 * Es una sola clase instanciada dos veces —700 y ocho veces los ingresos para
 * APROBADO, 600 y cinco veces para PREAPROBADO— porque el criterio es
 * identico y duplicarlo en dos clases seria la misma regla escrita dos veces.
 * Los cuatro numeros viven en {@code reglas.decision} y entran por el
 * constructor: aqui no hay literales.
 *
 * El score que compara es el del buro, no el de la cadena: la cadena corta por
 * debajo de 600, que es donde la solicitud deja de ser viable; estos dos
 * umbrales son los que graduan lo que si es viable (D-042).
 *
 * El dinero se compara con {@link BigDecimal}, nunca con {@code double}
 * (docs/conventions.md §4).
 */
public class ReglaAprobacionPorUmbrales implements ReglaDecision {

    private final EstadoSolicitud estado;
    private final int scoreMinimo;
    private final int multiploMaximoDeIngresos;

    /**
     * @param estado estado al que lleva esta fila de la tabla
     * @param scoreMinimo score a partir del cual la fila aplica
     * @param multiploMaximoDeIngresos veces los ingresos mensuales que puede
     *                                 valer el monto solicitado
     */
    public ReglaAprobacionPorUmbrales(EstadoSolicitud estado, int scoreMinimo, int multiploMaximoDeIngresos) {
        this.estado = estado;
        this.scoreMinimo = scoreMinimo;
        this.multiploMaximoDeIngresos = multiploMaximoDeIngresos;
    }

    @Override
    public boolean cumpleCondicion(CasoDeDecision caso) {
        return caso.superaTodasLasValidaciones()
                && caso.obtenerScore() >= scoreMinimo
                && cabeEnLosIngresos(caso);
    }

    @Override
    public EstadoSolicitud obtenerEstado() {
        return estado;
    }

    private boolean cabeEnLosIngresos(CasoDeDecision caso) {
        BigDecimal montoMaximo = caso.solicitud().ingresosMensuales()
                .multiply(BigDecimal.valueOf(multiploMaximoDeIngresos));

        return caso.solicitud().montoSolicitado().compareTo(montoMaximo) <= 0;
    }
}
