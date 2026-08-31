package com.weiz.motordedecision.domain.validacion;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Constructor de solicitudes de prueba, para que cada test escriba solo el dato
 * que le importa y no repita los otros tres.
 *
 * Los datos son falsos por definicion (docs/conventions.md §11): el documento
 * 1234567890 es el mismo que usa el enunciado en sus ejemplos.
 */
final class SolicitudesDePrueba {

    static final String DOCUMENTO_LIMPIO = "1234567890";
    static final String DOCUMENTO_BLOQUEADO = "1010101010";
    static final BigDecimal MONTO = new BigDecimal("15000000");
    static final BigDecimal INGRESOS = new BigDecimal("4000000");
    static final int SCORE_ALTO = 750;

    private static final LocalDateTime FECHA_CONSULTA = LocalDateTime.of(2026, 8, 24, 10, 30);
    private static final String ESTADO_ACTIVO = "ACTIVO";

    private SolicitudesDePrueba() {
    }

    /**
     * Crea una solicitud sin motivos para ser rechazada por ninguna validacion.
     *
     * @return solicitud con informe de buro favorable
     */
    static SolicitudEvaluada crearSolicitudImpecable() {
        return crearSolicitudConInforme(DOCUMENTO_LIMPIO, MONTO, INGRESOS, SCORE_ALTO, false);
    }

    /**
     * Crea una solicitud con el informe del buro ya resuelto.
     *
     * @param numeroDocumento documento del solicitante
     * @param monto monto solicitado
     * @param ingresos ingresos mensuales declarados
     * @param score score que reporta el buro
     * @param hayReporteNegativo si el buro reporta antecedentes
     * @return solicitud lista para evaluar
     */
    static SolicitudEvaluada crearSolicitudConInforme(String numeroDocumento,
                                                      BigDecimal monto,
                                                      BigDecimal ingresos,
                                                      int score,
                                                      boolean hayReporteNegativo) {

        return new SolicitudEvaluada(numeroDocumento, monto, ingresos,
                ResultadoConsultaBuro.crearResultadoConInforme(
                        score, ESTADO_ACTIVO, hayReporteNegativo, FECHA_CONSULTA));
    }

    /**
     * Crea una solicitud cuyo buro no respondio.
     *
     * @param numeroDocumento documento del solicitante
     * @return solicitud sin informe de buro
     */
    static SolicitudEvaluada crearSolicitudSinInformeDeBuro(String numeroDocumento) {
        return new SolicitudEvaluada(numeroDocumento, MONTO, INGRESOS,
                ResultadoConsultaBuro.crearResultadoBuroNoDisponible());
    }
}
