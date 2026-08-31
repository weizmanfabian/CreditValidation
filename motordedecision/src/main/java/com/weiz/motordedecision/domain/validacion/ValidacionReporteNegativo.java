package com.weiz.motordedecision.domain.validacion;

/**
 * Cuarta validacion de la cadena: que el buro no reporte antecedentes.
 *
 * Es la unica sin configuracion: el buro reporta o no reporta, no hay umbral
 * que ajustar.
 *
 * Va de ultima aunque lea el mismo informe que la validacion de score porque
 * el enunciado fija ese orden. Las dos son independientes: ninguna sabe de la
 * otra ni depende de su veredicto.
 */
public class ValidacionReporteNegativo implements ValidacionDependienteDelBuro {

    private static final String NOMBRE = "Reporte negativo";
    private static final String DETALLE_APROBADO = "Sin reportes negativos";
    private static final String DETALLE_RECHAZADO = "El buro reporta antecedentes negativos";

    @Override
    public String obtenerNombre() {
        return NOMBRE;
    }

    @Override
    public Veredicto validar(SolicitudEvaluada solicitud) {
        if (solicitud.informeBuro().hayReporteNegativo()) {
            return Veredicto.crearVeredictoRechazado(DETALLE_RECHAZADO);
        }
        return Veredicto.crearVeredictoAprobado(DETALLE_APROBADO);
    }
}
