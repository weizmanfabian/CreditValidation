package com.weiz.buro.domain.informe;

import com.weiz.buro.api.models.response.InformeCrediticioResponse;

import java.time.Duration;
import java.util.Objects;

/**
 * Resuelve una consulta del buro simulado aplicando las tres reglas del mock del
 * enunciado: las dos de paridad, que delega en {@link GeneradorInformeCrediticio},
 * y la del documento que representa el servicio caido, que resuelve aqui.
 *
 * Esta clase existe para que la decision "este documento simula una caida" tenga
 * dueno fuera del controlador: la regla 1 de {@code docs/architecture.md} §3
 * prohibe logica de negocio en {@code api}. Sigue siendo Java puro, sin Spring
 * ni anotaciones de framework; el bean se declara desde {@code config}.
 *
 * Ni el documento que dispara la caida ni la duracion del retardo son literales:
 * llegan desde la configuracion del modulo.
 */
public final class SimuladorConsultaBuro {

    private final GeneradorInformeCrediticio generadorInformeCrediticio;
    private final PausaDeSimulacion pausaDeSimulacion;
    private final String documentoServicioCaido;
    private final Duration retardoServicioCaido;

    /**
     * Crea el simulador con sus colaboradores y los valores de la simulacion.
     *
     * @param generadorInformeCrediticio generador de las reglas par/impar
     * @param pausaDeSimulacion pausa que se interpone en el caso caido
     * @param documentoServicioCaido documento que representa el servicio caido
     * @param retardoServicioCaido cuanto se retrasa la respuesta de ese documento
     */
    public SimuladorConsultaBuro(GeneradorInformeCrediticio generadorInformeCrediticio,
                                 PausaDeSimulacion pausaDeSimulacion,
                                 String documentoServicioCaido,
                                 Duration retardoServicioCaido) {

        this.generadorInformeCrediticio =
                Objects.requireNonNull(generadorInformeCrediticio, "El generador del informe es obligatorio");
        this.pausaDeSimulacion =
                Objects.requireNonNull(pausaDeSimulacion, "La pausa de la simulacion es obligatoria");
        this.documentoServicioCaido =
                Objects.requireNonNull(documentoServicioCaido, "El documento del servicio caido es obligatorio");
        this.retardoServicioCaido =
                Objects.requireNonNull(retardoServicioCaido, "El retardo del servicio caido es obligatorio");
    }

    /**
     * Consulta el informe crediticio simulado del documento recibido.
     *
     * Si el documento es el que representa el servicio caido, la respuesta se
     * retrasa el tiempo configurado antes de calcularse. El informe se devuelve
     * igualmente: quien deja de esperar es el cliente, por su propio timeout.
     *
     * @param numeroDocumento numero de documento consultado, solo digitos
     * @return informe con score, estado, reporte negativo y fecha de consulta
     */
    public InformeCrediticioResponse consultarInforme(String numeroDocumento) {
        if (esDocumentoDelServicioCaido(numeroDocumento)) {
            pausaDeSimulacion.pausar(retardoServicioCaido);
        }

        return generadorInformeCrediticio.generarInforme(numeroDocumento);
    }

    private boolean esDocumentoDelServicioCaido(String numeroDocumento) {
        return documentoServicioCaido.equals(numeroDocumento);
    }
}
