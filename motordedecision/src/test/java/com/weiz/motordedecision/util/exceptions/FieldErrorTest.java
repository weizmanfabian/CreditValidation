package com.weiz.motordedecision.util.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija las cuatro localizaciones publicadas en {@code docs/error-handling.md} §2
 * y los nombres de sus factorias, que son contrato de este modulo: el JSON es el
 * mismo de {@code experianrestapi}, pero los metodos empiezan por verbo.
 */
class FieldErrorTest {

    private static final String CAMPO = "numeroDocumento";
    private static final String MENSAJE = "Numero de documento es requerido";

    private final JsonMapper mapeadorJson = JsonMapper.builder().build();

    static Stream<Arguments> proveerErroresPorOrigen() {
        return Stream.of(
                Arguments.of(FieldError.crearDeCuerpo(CAMPO, MENSAJE), "body"),
                Arguments.of(FieldError.crearDeHeader(CAMPO, MENSAJE), "header"),
                Arguments.of(FieldError.crearDeParametro(CAMPO, MENSAJE), "query"),
                Arguments.of(FieldError.crearDeRuta(CAMPO, MENSAJE), "path"));
    }

    @ParameterizedTest(name = "la factoria del origen produce location={1}")
    @MethodSource("proveerErroresPorOrigen")
    @DisplayName("Cada factoria fija su propia localizacion y conserva campo y mensaje")
    void crear_segunElOrigenDelDato_fijaLaLocalizacionQueLeCorresponde(FieldError error, String localizacion) {
        assertThat(error)
                .extracting(FieldError::field, FieldError::message, FieldError::location)
                .containsExactly(CAMPO, MENSAJE, localizacion);
    }

    @Test
    @DisplayName("El error de campo se serializa con field, message y location")
    void serializar_conErrorDeCuerpo_produceLosTresCamposDelContrato() {
        String json = mapeadorJson.writeValueAsString(FieldError.crearDeCuerpo(CAMPO, MENSAJE));

        assertThat(json).isEqualTo(
                "{\"field\":\"numeroDocumento\",\"message\":\"Numero de documento es requerido\","
                        + "\"location\":\"body\"}");
    }
}
