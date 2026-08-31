package com.weiz.motordedecision.domain.dataaccessors;

import com.weiz.motordedecision.domain.entities.Solicitud;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Calcula el identificador de negocio {@code SOL-yyyyMMdd-NNN}.
 *
 * El consecutivo {@code NNN} es por dia y se calcula aqui, en la aplicacion, no
 * con una secuencia de la base de datos (D-025): una secuencia habria que
 * reiniciarla cada medianoche, y eso es una tarea programada que el enunciado no
 * pide y que anade un punto de fallo.
 *
 * El numero sale de la ultima solicitud radicada ese mismo dia, mas uno. Dos
 * radicaciones simultaneas pueden calcular el mismo consecutivo; quien lo cierra
 * es la restriccion {@code UNIQUE} de {@code id_solicitud}, que hace fallar la
 * segunda insercion en voz alta en vez de duplicar el identificador. El detalle
 * esta en {@link SolicitudDataAccessor#guardarSolicitud}.
 */
@Component
public class GeneradorIdSolicitud {

    private static final DateTimeFormatter FORMATO_DEL_DIA = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String PLANTILLA_PREFIJO = "SOL-%s-";
    private static final String PLANTILLA_CONSECUTIVO = "%03d";
    private static final int PRIMER_CONSECUTIVO = 1;

    private final SolicitudRepository solicitudRepository;

    public GeneradorIdSolicitud(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    /**
     * @param dia dia de radicacion, el mismo que se guarda en {@code fecha_creacion}
     * @return el siguiente identificador de negocio libre para ese dia
     */
    public String generarIdSolicitud(LocalDate dia) {
        String prefijo = PLANTILLA_PREFIJO.formatted(FORMATO_DEL_DIA.format(dia));

        // Buscamos la ultima solicitud del dia; si no hay ninguna, empezamos en 001
        int consecutivo = solicitudRepository
                .findTopByIdSolicitudStartingWithOrderByIdSolicitudDesc(prefijo)
                // De su identificador nos interesa solo la cola numerica
                .map(Solicitud::getIdSolicitud)
                .map(GeneradorIdSolicitud::extraerConsecutivo)
                // El siguiente libre es el ultimo mas uno
                .map(ultimo -> ultimo + 1)
                .orElse(PRIMER_CONSECUTIVO);

        return prefijo + PLANTILLA_CONSECUTIVO.formatted(consecutivo);
    }

    /**
     * La cola numerica de un identificador que este generador ya emitio: siempre
     * tres digitos detras del ultimo guion.
     */
    private static int extraerConsecutivo(String idSolicitud) {
        return Integer.parseInt(idSolicitud.substring(idSolicitud.lastIndexOf('-') + 1));
    }
}
