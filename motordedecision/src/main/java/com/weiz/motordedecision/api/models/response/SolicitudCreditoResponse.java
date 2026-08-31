package com.weiz.motordedecision.api.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;

import java.time.LocalDateTime;

/**
 * La respuesta compuesta que el motor devuelve al radicar una solicitud
 * (enunciado, lineas 86-124).
 *
 * Tiene un sobre y cuatro secciones. El sobre —identificador, fecha y estado—
 * viaja siempre: sin el, la respuesta no identifica nada. Las secciones son
 * opcionales y se agregan una a una con {@link Builder}: no todas las
 * solicitudes generan la misma respuesta, y lo que no aplica al caso no viaja
 * como {@code null} sino que no viaja (D-047).
 *
 * Agregar una seccion nueva es agregar un componente a este record y su metodo
 * {@code agregarXxx} al constructor. Ninguna de las secciones existentes se
 * toca, y quien decide si la seccion aplica es su aportante, en {@code mapper}.
 *
 * El detalle de la construccion esta en {@link #crearConstructorPara}.
 *
 * @param idSolicitud identificador de negocio {@code SOL-yyyyMMdd-NNN}
 * @param fechaCreacion instante en que se radico la solicitud
 * @param estado estado final, uno de los cinco de {@link EstadoSolicitud}
 * @param solicitante quien pide el credito
 * @param detalle cifras del credito, con la tasa cuando la hay
 * @param evaluacion score del buro y rastro de la cadena
 * @param siguientePaso que ocurre despues, o {@code null} si no hay siguiente paso
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"idSolicitud", "fechaCreacion", "estado", "solicitante", "detalle", "evaluacion", "siguientePaso"})
public record SolicitudCreditoResponse(

        String idSolicitud,

        LocalDateTime fechaCreacion,

        String estado,

        SeccionSolicitante solicitante,

        SeccionDetalleFinanciero detalle,

        SeccionEvaluacion evaluacion,

        String siguientePaso) {

    /**
     * Abre la construccion de una respuesta con su sobre ya puesto.
     *
     * Las tres piezas del sobre se piden aqui y no en un {@code agregarXxx}
     * porque son obligatorias: un constructor que permitiera olvidarlas
     * dejaria armar una respuesta sin identificador.
     *
     * @param idSolicitud identificador de negocio de la solicitud
     * @param fechaCreacion instante en que se radico
     * @param estado estado final que decidio el motor
     * @return el constructor, listo para recibir secciones
     */
    public static Builder crearConstructorPara(String idSolicitud,
                                               LocalDateTime fechaCreacion,
                                               EstadoSolicitud estado) {

        return new Builder(idSolicitud, fechaCreacion, estado);
    }

    /**
     * Arma la respuesta seccion a seccion.
     *
     * Cada {@code agregarXxx} es independiente de los demas: se puede llamar,
     * o no, en cualquier orden, y lo que no se agregue queda fuera del JSON.
     * Esa independencia es lo que permite que un aportante de seccion nuevo no
     * obligue a tocar los existentes.
     */
    public static final class Builder {

        private final String idSolicitud;
        private final LocalDateTime fechaCreacion;
        private final EstadoSolicitud estado;

        private SeccionSolicitante solicitante;
        private SeccionDetalleFinanciero detalle;
        private SeccionEvaluacion evaluacion;
        private String siguientePaso;

        private Builder(String idSolicitud, LocalDateTime fechaCreacion, EstadoSolicitud estado) {
            this.idSolicitud = idSolicitud;
            this.fechaCreacion = fechaCreacion;
            this.estado = estado;
        }

        /**
         * @param solicitante seccion con el nombre y el documento
         * @return este mismo constructor
         */
        public Builder agregarSolicitante(SeccionSolicitante solicitante) {
            this.solicitante = solicitante;
            return this;
        }

        /**
         * @param detalle seccion con el monto, el plazo y la tasa si la hay
         * @return este mismo constructor
         */
        public Builder agregarDetalle(SeccionDetalleFinanciero detalle) {
            this.detalle = detalle;
            return this;
        }

        /**
         * @param evaluacion seccion con el score y el rastro de la cadena
         * @return este mismo constructor
         */
        public Builder agregarEvaluacion(SeccionEvaluacion evaluacion) {
            this.evaluacion = evaluacion;
            return this;
        }

        /**
         * @param siguientePaso texto de lo que ocurre despues
         * @return este mismo constructor
         */
        public Builder agregarSiguientePaso(String siguientePaso) {
            this.siguientePaso = siguientePaso;
            return this;
        }

        /**
         * Cierra la construccion con las secciones que se hayan agregado.
         *
         * @return la respuesta lista para serializar
         */
        public SolicitudCreditoResponse construir() {
            return new SolicitudCreditoResponse(idSolicitud, fechaCreacion, estado.name(),
                    solicitante, detalle, evaluacion, siguientePaso);
        }
    }
}
