package com.weiz.motordedecision.domain.validacion;

import java.math.BigDecimal;

/**
 * Tercera validacion de la cadena: que el monto pedido quepa en los ingresos.
 *
 * El tope se expresa como un multiplo de los ingresos mensuales y entra por el
 * constructor desde {@code reglas.validacion.multiplo-maximo-de-ingresos}. Es
 * el multiplo mas permisivo del enunciado: por encima de el ninguna decision
 * posterior podria aprobar la solicitud, asi que la cadena la corta aqui.
 *
 * El dinero se compara con {@link BigDecimal}, nunca con {@code double}
 * (docs/conventions.md §4).
 */
public class ValidacionCapacidadDePago implements ValidacionCrediticia {

    private static final String NOMBRE = "Capacidad de pago";
    private static final String DETALLE_APROBADO = "Monto dentro del rango permitido";
    private static final String PLANTILLA_RECHAZADO = "Monto solicitado supera %d veces los ingresos mensuales";

    private final int multiploMaximoDeIngresos;

    /**
     * @param multiploMaximoDeIngresos cuantas veces los ingresos mensuales puede
     *                                 valer el monto solicitado
     */
    public ValidacionCapacidadDePago(int multiploMaximoDeIngresos) {
        this.multiploMaximoDeIngresos = multiploMaximoDeIngresos;
    }

    @Override
    public String obtenerNombre() {
        return NOMBRE;
    }

    @Override
    public Veredicto validar(SolicitudEvaluada solicitud) {
        BigDecimal montoMaximo = solicitud.ingresosMensuales()
                .multiply(BigDecimal.valueOf(multiploMaximoDeIngresos));

        if (solicitud.montoSolicitado().compareTo(montoMaximo) > 0) {
            return Veredicto.crearVeredictoRechazado(
                    String.format(PLANTILLA_RECHAZADO, multiploMaximoDeIngresos));
        }
        return Veredicto.crearVeredictoAprobado(DETALLE_APROBADO);
    }
}
