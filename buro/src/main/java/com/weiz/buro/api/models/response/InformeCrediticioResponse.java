package com.weiz.buro.api.models.response;

import com.weiz.buro.util.enums.EstadoTitular;

import java.time.LocalDateTime;

/**
 * Informe crediticio que devuelve el buro simulado, con la forma exacta del
 * enunciado: {@code score}, {@code estado}, {@code reporteNegativo} y
 * {@code fechaConsulta}.
 *
 * El buro no persiste nada: cada informe se calcula a partir del numero de
 * documento consultado.
 *
 * @param score puntaje crediticio del titular
 * @param estado estado del titular segun el buro
 * @param reporteNegativo si el titular arrastra antecedentes negativos
 * @param fechaConsulta instante en que se resolvio la consulta
 */
public record InformeCrediticioResponse(

        int score,

        EstadoTitular estado,

        boolean reporteNegativo,

        LocalDateTime fechaConsulta) {
}
