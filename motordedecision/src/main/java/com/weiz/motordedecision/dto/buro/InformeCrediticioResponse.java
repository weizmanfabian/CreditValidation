package com.weiz.motordedecision.dto.buro;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Informe crediticio tal como lo devuelve el buro.
 *
 * El patron de {@code fechaConsulta} se repite aqui porque es contrato: el buro
 * la serializa como {@code yyyy-MM-dd HH:mm:ss} (D-022) y sin el patron
 * Jackson no sabria leerla. Este DTO muere en
 * {@code infraestructura/client}, que lo convierte en un
 * {@link com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro}.
 *
 * @param score puntaje crediticio del titular
 * @param estado estado del titular segun el buro
 * @param reporteNegativo si el titular arrastra antecedentes negativos
 * @param fechaConsulta instante en que el buro resolvio la consulta
 */
public record InformeCrediticioResponse(

        int score,

        String estado,

        boolean reporteNegativo,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime fechaConsulta) {
}
