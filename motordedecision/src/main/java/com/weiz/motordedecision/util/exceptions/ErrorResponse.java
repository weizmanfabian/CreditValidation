package com.weiz.motordedecision.util.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Cuerpo unico de error del motor ({@code docs/error-handling.md} §2).
 *
 * Es el mismo contrato que publica el buro: un cliente que integre contra
 * cualquiera de los dos modulos recibe errores con la misma forma.
 *
 * Los cuatro campos son opcionales salvo {@code message}: al serializarse con
 * {@code NON_NULL}, lo que no aplica al caso concreto no viaja como {@code null}.
 * Un error de validacion trae {@code errors}, uno de negocio trae {@code codigo}
 * y {@code detalles}, y un fallo tecnico trae solo {@code message}.
 *
 * Los nombres de los campos estan en ingles porque son el contrato de cable que
 * consume el frontend; el resto del dominio se escribe en espanol.
 *
 * @param codigo identificador estable del error de negocio, para que el cliente reaccione sin parsear textos
 * @param message resumen legible del tipo de error
 * @param detalles causa concreta cuando el error no tiene un campo asociado
 * @param errors campos rechazados, uno por restriccion incumplida
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"codigo", "message", "detalles", "errors"})
public record ErrorResponse(String codigo, String message, String detalles, List<FieldError> errors) {

    public ErrorResponse {
        errors = errors == null ? null : List.copyOf(errors);
    }

    /**
     * Crea el cuerpo de un error de validacion, con un elemento por campo rechazado.
     */
    public static ErrorResponse crearConCampos(String mensaje, List<FieldError> errores) {
        return new ErrorResponse(null, mensaje, null, errores);
    }

    /**
     * Crea el cuerpo de un error de una sola causa, sin campo asociado.
     */
    public static ErrorResponse crearConDetalles(String mensaje, String detalles) {
        return new ErrorResponse(null, mensaje, detalles, null);
    }

    /**
     * Crea el cuerpo de un error que no aporta nada mas que su mensaje generico.
     */
    public static ErrorResponse crearConMensaje(String mensaje) {
        return new ErrorResponse(null, mensaje, null, null);
    }

    /**
     * Crea el cuerpo de un error de negocio: es el unico que lleva {@code codigo}.
     */
    public static ErrorResponse crearDeNegocio(String codigo, String mensaje, String detalles) {
        return new ErrorResponse(codigo, mensaje, detalles, null);
    }
}
