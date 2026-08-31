package com.weiz.motordedecision.domain.decision;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import com.weiz.motordedecision.domain.validacion.RegistroValidacion;
import com.weiz.motordedecision.domain.validacion.SolicitudEvaluada;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Constructor de casos de decision, para que cada test escriba solo el dato que
 * le importa.
 *
 * Arma el rastro tal como lo dejaria la cadena de validaciones de verdad: los
 * nombres son los del enunciado, en su orden, y el corte en la validacion que
 * rechaza es el mismo que hace {@code CadenaDeValidaciones}. Si el rastro se
 * inventara, los tests probarian una cadena que no existe.
 *
 * Los datos son falsos por definicion (docs/conventions.md §11): el documento
 * 1234567890 es el que usa el enunciado en sus ejemplos.
 */
public final class CasosDeDecisionDePrueba {

    public static final String IDENTIDAD = "Identidad";
    public static final String SCORE = "Score";
    public static final String REPORTE_NEGATIVO = "Reporte negativo";

    public static final BigDecimal INGRESOS = new BigDecimal("4000000");
    public static final BigDecimal MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS = new BigDecimal("15000000");
    public static final BigDecimal MONTO_DE_CINCO_VECES_LOS_INGRESOS = new BigDecimal("20000000");
    public static final BigDecimal MONTO_DE_CINCO_VECES_LOS_INGRESOS_MAS_UNO = new BigDecimal("20000001");
    public static final BigDecimal MONTO_DE_SIETE_VECES_Y_MEDIA_LOS_INGRESOS = new BigDecimal("30000000");
    public static final BigDecimal MONTO_DE_OCHO_VECES_LOS_INGRESOS = new BigDecimal("32000000");
    public static final BigDecimal MONTO_DE_OCHO_VECES_LOS_INGRESOS_MAS_UNO = new BigDecimal("32000001");

    public static final int SCORE_ALTO = 750;
    public static final int SCORE_MINIMO_DE_APROBADO = 700;
    public static final int SCORE_JUSTO_POR_DEBAJO_DE_APROBADO = 699;
    public static final int SCORE_INTERMEDIO = 640;
    public static final int SCORE_MINIMO_DE_PREAPROBADO = 600;

    private static final String DOCUMENTO = "1234567890";
    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final LocalDateTime FECHA_CONSULTA = LocalDateTime.of(2026, 8, 24, 10, 30);
    private static final List<String> CADENA = List.of(IDENTIDAD, SCORE, "Capacidad de pago", REPORTE_NEGATIVO);
    private static final String DETALLE = "Detalle de prueba";

    private CasosDeDecisionDePrueba() {
    }

    /**
     * Crea el caso de una solicitud que recorrio la cadena entera sin rechazos.
     *
     * @param score score que reporto el buro
     * @param monto monto solicitado
     * @return caso con las cuatro validaciones aprobadas
     */
    public static CasoDeDecision crearCasoConLaCadenaCompleta(int score, BigDecimal monto) {
        return new CasoDeDecision(crearSolicitudConInforme(score, monto), crearRastroHasta(CADENA.size()));
    }

    /**
     * Crea el caso de una solicitud que la cadena detuvo en una validacion.
     *
     * @param nombreValidacion validacion que rechazo, la ultima del rastro
     * @return caso con el rastro cortado en esa validacion
     */
    public static CasoDeDecision crearCasoRechazadoEn(String nombreValidacion) {
        List<RegistroValidacion> rastro = crearRastroHasta(CADENA.indexOf(nombreValidacion));
        rastro.add(crearRegistro(rastro.size() + 1, nombreValidacion, ResultadoEvaluacion.RECHAZADO));

        return new CasoDeDecision(
                crearSolicitudConInforme(SCORE_ALTO, MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS), rastro);
    }

    /**
     * Crea el caso de una solicitud cuyo buro no respondio: la cadena se detuvo
     * despues de identidad, sin registrar lo que necesitaba el informe (D-041).
     *
     * @param resultadoDeIdentidad como termino la unica validacion que si corrio
     * @return caso sin informe del buro
     */
    public static CasoDeDecision crearCasoSinInformeDeBuro(ResultadoEvaluacion resultadoDeIdentidad) {
        SolicitudEvaluada solicitud = new SolicitudEvaluada(DOCUMENTO,
                MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS, INGRESOS,
                ResultadoConsultaBuro.crearResultadoBuroNoDisponible());

        return new CasoDeDecision(solicitud, List.of(crearRegistro(1, IDENTIDAD, resultadoDeIdentidad)));
    }

    private static SolicitudEvaluada crearSolicitudConInforme(int score, BigDecimal monto) {
        return new SolicitudEvaluada(DOCUMENTO, monto, INGRESOS,
                ResultadoConsultaBuro.crearResultadoConInforme(score, ESTADO_ACTIVO, false, FECHA_CONSULTA));
    }

    private static List<RegistroValidacion> crearRastroHasta(int cantidadAprobada) {
        List<RegistroValidacion> rastro = new ArrayList<>();
        for (int posicion = 1; posicion <= cantidadAprobada; posicion++) {
            rastro.add(crearRegistro(posicion, CADENA.get(posicion - 1), ResultadoEvaluacion.APROBADO));
        }
        return rastro;
    }

    private static RegistroValidacion crearRegistro(int orden, String nombre, ResultadoEvaluacion resultado) {
        return new RegistroValidacion((short) orden, nombre, resultado, DETALLE);
    }
}
