package com.weiz.motordedecision.domain.validacion;

import java.util.List;
import java.util.Set;

/**
 * Primera validacion de la cadena: que el documento no este bloqueado.
 *
 * La lista de documentos bloqueados no se escribe aqui, entra por el
 * constructor desde la configuracion ({@code reglas.validacion.documentos-bloqueados}),
 * porque es un dato de negocio que cambia sin recompilar
 * (docs/conventions.md §9).
 *
 * Va de primera a proposito: es la unica que no necesita consultar nada, y su
 * rechazo es el mas grave —termina en {@code RECHAZADO_FRAUDE}—, asi que corta
 * la evaluacion antes de gastar una llamada al buro.
 */
public class ValidacionIdentidad implements ValidacionCrediticia {

    private static final String NOMBRE = "Identidad";
    private static final String DETALLE_APROBADO = "Documento no bloqueado";
    private static final String DETALLE_RECHAZADO = "Documento reportado en la lista de bloqueados";

    /** Conjunto, no lista: la pertenencia se consulta una vez por solicitud. */
    private final Set<String> documentosBloqueados;

    /**
     * @param documentosBloqueados documentos que el negocio tiene vetados
     */
    public ValidacionIdentidad(List<String> documentosBloqueados) {
        this.documentosBloqueados = Set.copyOf(documentosBloqueados);
    }

    @Override
    public String obtenerNombre() {
        return NOMBRE;
    }

    @Override
    public Veredicto validar(SolicitudEvaluada solicitud) {
        if (documentosBloqueados.contains(solicitud.numeroDocumento())) {
            return Veredicto.crearVeredictoRechazado(DETALLE_RECHAZADO);
        }
        return Veredicto.crearVeredictoAprobado(DETALLE_APROBADO);
    }
}
