package com.weiz.motordedecision.domain.validacion;

/**
 * Segunda validacion de la cadena: que el score alcance el minimo del negocio.
 *
 * El minimo entra por el constructor desde
 * {@code reglas.validacion.score-minimo}. Es el umbral por debajo del cual la
 * solicitud ya no es viable de ninguna forma; los umbrales que separan un
 * aprobado de un preaprobado son otra cosa y viven en las reglas de decision.
 *
 * Depende del informe del buro: sin informe no se pronuncia
 * ({@link ValidacionDependienteDelBuro}).
 */
public class ValidacionScoreCrediticio implements ValidacionDependienteDelBuro {

    private static final String NOMBRE = "Score";
    private static final String PLANTILLA_APROBADO = "Score %d >= %d";
    private static final String PLANTILLA_RECHAZADO = "Score %d < %d";

    private final int scoreMinimo;

    /**
     * @param scoreMinimo score por debajo del cual la solicitud se rechaza
     */
    public ValidacionScoreCrediticio(int scoreMinimo) {
        this.scoreMinimo = scoreMinimo;
    }

    @Override
    public String obtenerNombre() {
        return NOMBRE;
    }

    @Override
    public Veredicto validar(SolicitudEvaluada solicitud) {
        int score = solicitud.informeBuro().score();
        if (score < scoreMinimo) {
            return Veredicto.crearVeredictoRechazado(String.format(PLANTILLA_RECHAZADO, score, scoreMinimo));
        }
        return Veredicto.crearVeredictoAprobado(String.format(PLANTILLA_APROBADO, score, scoreMinimo));
    }
}
