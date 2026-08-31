package com.weiz.motordedecision.domain.buro;

import java.time.LocalDateTime;

/**
 * Lo que el motor sabe del titular despues de consultar el buro.
 *
 * Tiene dos formas, y {@code hayInformeDisponible} dice cual es. Cuando el buro
 * respondio, el resultado trae el informe completo. Cuando no —se cayo, tardo
 * mas de lo permitido o el circuito esta abierto—, el resultado dice justo eso
 * y el resto de campos no significan nada: son el vacio de un informe que no
 * existe, no un informe con score cero.
 *
 * Ese segundo caso no es un error: esta previsto por el enunciado y termina en
 * una solicitud {@code PENDIENTE_REVISION} con 200
 * ({@code docs/error-handling.md} §4.4). Por eso el buro caido llega hasta aqui
 * como un valor y no como una excepcion.
 *
 * Se construye por sus dos metodos de fabrica, nunca por el constructor
 * canonico: son ellos los que dan nombre a cada forma.
 *
 * @param hayInformeDisponible si el buro respondio y los demas campos valen
 * @param score puntaje crediticio del titular
 * @param estadoTitular estado que reporta el buro, {@code ACTIVO} o {@code EN_MORA}
 * @param hayReporteNegativo si el titular arrastra antecedentes negativos
 * @param fechaConsulta instante en que el buro resolvio la consulta
 */
public record ResultadoConsultaBuro(

        boolean hayInformeDisponible,

        int score,

        String estadoTitular,

        boolean hayReporteNegativo,

        LocalDateTime fechaConsulta) {

    /** Valor de relleno del informe ausente. Ninguna regla debe leerlo. */
    private static final int SCORE_SIN_INFORME = 0;

    /**
     * Crea el resultado de una consulta que el buro si respondio.
     *
     * @param score puntaje crediticio del titular
     * @param estadoTitular estado que reporta el buro
     * @param hayReporteNegativo si el titular arrastra antecedentes negativos
     * @param fechaConsulta instante en que el buro resolvio la consulta
     * @return resultado con el informe completo
     */
    public static ResultadoConsultaBuro crearResultadoConInforme(int score,
                                                                 String estadoTitular,
                                                                 boolean hayReporteNegativo,
                                                                 LocalDateTime fechaConsulta) {

        return new ResultadoConsultaBuro(true, score, estadoTitular, hayReporteNegativo, fechaConsulta);
    }

    /**
     * Crea el resultado de una consulta que no se pudo resolver.
     *
     * @return resultado sin informe, el que se traduce en PENDIENTE_REVISION
     */
    public static ResultadoConsultaBuro crearResultadoBuroNoDisponible() {
        return new ResultadoConsultaBuro(false, SCORE_SIN_INFORME, null, false, null);
    }
}
