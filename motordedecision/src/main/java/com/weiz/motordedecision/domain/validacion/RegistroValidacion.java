package com.weiz.motordedecision.domain.validacion;

import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;

/**
 * El paso de una validacion por la cadena, ya con su posicion y su nombre.
 *
 * Es el rastro que pide el enunciado —nombre, resultado y detalle— y lleva los
 * mismos cuatro campos que la entidad {@code ResultadoValidacion}, con los
 * mismos nombres, para que persistirlo sea directo. La unica diferencia es el
 * tipo del {@code resultado}: aqui es el enum del dominio y en la entidad es un
 * {@code String}, asi que el mapeo hara un {@code name()}.
 *
 * El {@code orden} empieza en 1 y es informacion de negocio: como la cadena se
 * detiene en la primera validacion que rechaza, el ultimo orden registrado dice
 * hasta donde llego la evaluacion.
 *
 * @param orden posicion en la cadena, empezando en 1
 * @param nombre nombre de la validacion, tal como lo publica el enunciado
 * @param resultado si la validacion aprobo o rechazo
 * @param detalle explicacion en lenguaje de negocio
 */
public record RegistroValidacion(

        short orden,

        String nombre,

        ResultadoEvaluacion resultado,

        String detalle) {
}
