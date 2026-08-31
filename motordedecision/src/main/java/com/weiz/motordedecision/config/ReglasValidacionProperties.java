package com.weiz.motordedecision.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Los datos de negocio que gobiernan la cadena de validaciones.
 *
 * Ninguno de los tres es un literal del codigo: la lista de bloqueados la
 * define el area de riesgo y los dos umbrales se ajustan sin recompilar
 * (docs/conventions.md §9).
 *
 * Los tres son obligatorios y estan validados a proposito. Un
 * {@code score-minimo} ausente se enlazaria como cero y dejaria pasar
 * cualquier score; una lista vacia dejaria de bloquear a nadie. Fallar al
 * arrancar es preferible a evaluar creditos con reglas apagadas en silencio.
 *
 * @param documentosBloqueados documentos vetados por el negocio
 * @param scoreMinimo score por debajo del cual la solicitud se rechaza
 * @param multiploMaximoDeIngresos veces los ingresos mensuales que puede valer
 *                                 el monto solicitado
 */
@Validated
@ConfigurationProperties(prefix = "reglas.validacion")
public record ReglasValidacionProperties(

        @NotEmpty List<String> documentosBloqueados,

        @Positive int scoreMinimo,

        @Positive int multiploMaximoDeIngresos) {
}
