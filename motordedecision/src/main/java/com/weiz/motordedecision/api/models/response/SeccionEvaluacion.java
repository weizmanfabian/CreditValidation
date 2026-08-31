package com.weiz.motordedecision.api.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Optional;

/**
 * Lo que la evaluacion vio: el score del buro y el rastro de la cadena.
 *
 * El {@code scoreBureau} es opcional porque no toda solicitud llega a
 * consultar el buro —un documento bloqueado corta antes, y un buro caido no
 * devuelve score—. En esos casos el campo no viaja, que es distinto de viajar
 * en cero: cero seria un score malisimo, y no lo hay.
 *
 * El nombre del campo esta en ingles porque asi lo publica el enunciado (linea
 * 112); es contrato de cable, no una eleccion de estilo.
 *
 * @param scoreBureau puntaje del buro, o {@code null} si no hubo consulta
 * @param validaciones rastro de la cadena, en orden de ejecucion; si llega
 *                     nulo se guarda como lista vacia
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeccionEvaluacion(Integer scoreBureau, List<ValidacionRealizada> validaciones) {

    public SeccionEvaluacion {
        // Sin validaciones la seccion sigue teniendo sentido: viaja con la lista vacia,
        // nunca con un nulo que obligue a quien la lea a preguntar por el
        validaciones = List.copyOf(Optional.ofNullable(validaciones).orElse(List.of()));
    }
}
