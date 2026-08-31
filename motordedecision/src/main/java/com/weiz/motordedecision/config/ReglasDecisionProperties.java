package com.weiz.motordedecision.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Los numeros de la tabla de decision del enunciado.
 *
 * Ninguno es un literal del codigo: los cuatro umbrales los ajusta el area de
 * riesgo sin recompilar (docs/conventions.md §9), igual que los de la cadena de
 * validaciones.
 *
 * Todos son obligatorios y estan validados a proposito. Un {@code score-minimo}
 * ausente se enlazaria como cero y aprobaria cualquier solicitud; un multiplo
 * ausente no dejaria pasar ninguna. Fallar al arrancar es preferible a decidir
 * creditos con la tabla en blanco.
 *
 * @param validacionDeIdentidad nombre de la validacion cuyo rechazo significa
 *                              fraude y no un rechazo corriente
 * @param aprobado umbrales de la fila APROBADO
 * @param preaprobado umbrales de la fila PREAPROBADO
 */
@Validated
@ConfigurationProperties(prefix = "reglas.decision")
public record ReglasDecisionProperties(

        @NotBlank String validacionDeIdentidad,

        @NotNull @Valid UmbralesDeEstado aprobado,

        @NotNull @Valid UmbralesDeEstado preaprobado) {

    /**
     * Los dos numeros que definen una fila de aprobacion de la tabla.
     *
     * @param scoreMinimo score a partir del cual la fila aplica
     * @param multiploMaximoDeIngresos veces los ingresos mensuales que puede
     *                                 valer el monto solicitado
     */
    public record UmbralesDeEstado(

            @Positive int scoreMinimo,

            @Positive int multiploMaximoDeIngresos) {
    }
}
