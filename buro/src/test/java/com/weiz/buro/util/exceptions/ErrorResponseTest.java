package com.weiz.buro.util.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija el contrato JSON del cuerpo de error contra {@code docs/error-handling.md}
 * §2: los cuatro campos con su nombre exacto y, sobre todo, que lo que no aplica
 * al caso no viaje como {@code null}.
 */
class ErrorResponseTest {

    private static final String MENSAJE_DATOS_INVALIDOS = "Error en los datos proporcionados";

    private final JsonMapper mapeadorJson = JsonMapper.builder().build();

    @Test
    @DisplayName("Un error de validacion serializa message y errors, sin codigo ni detalles")
    void serializar_conErrorDeValidacion_omiteCodigoYDetalles() {
        ErrorResponse error = ErrorResponse.crearConCampos(MENSAJE_DATOS_INVALIDOS, List.of(
                FieldError.crearDeCuerpo("numeroDocumento", "numeroDocumento es requerido")));

        String json = mapeadorJson.writeValueAsString(error);

        assertThat(json)
                .contains("\"message\":\"Error en los datos proporcionados\"")
                .contains("\"field\":\"numeroDocumento\"")
                .contains("\"location\":\"body\"")
                .doesNotContain("codigo")
                .doesNotContain("detalles");
    }

    @Test
    @DisplayName("Un error de negocio serializa codigo, message y detalles, sin errors")
    void serializar_conErrorDeNegocio_incluyeCodigoYOmiteErrors() {
        ErrorResponse error = ErrorResponse.crearDeNegocio("TIPO_DOCUMENTO_NO_SOPORTADO",
                "El tipo de documento no es CC, CE ni PA", "El tipo NIT no se consulta en el buro");

        String json = mapeadorJson.writeValueAsString(error);

        assertThat(json)
                .contains("\"codigo\":\"TIPO_DOCUMENTO_NO_SOPORTADO\"")
                .contains("\"detalles\":\"El tipo NIT no se consulta en el buro\"")
                .doesNotContain("errors");
    }

    @Test
    @DisplayName("Un fallo tecnico serializa solo message: no filtra nada del interior")
    void serializar_conErrorTecnico_dejaSoloElMensajeGenerico() {
        ErrorResponse error = ErrorResponse.crearConMensaje("Servicio temporalmente no disponible");

        String json = mapeadorJson.writeValueAsString(error);

        assertThat(json).isEqualTo("{\"message\":\"Servicio temporalmente no disponible\"}");
    }

    @Test
    @DisplayName("Un error de una sola causa serializa message y detalles, sin errors")
    void serializar_conErrorDeUnaSolaCausa_incluyeDetallesYOmiteErrors() {
        ErrorResponse error = ErrorResponse.crearConDetalles(
                "El cuerpo de la peticion no es un JSON valido", "Unexpected character");

        String json = mapeadorJson.writeValueAsString(error);

        assertThat(json)
                .contains("\"detalles\":\"Unexpected character\"")
                .doesNotContain("errors");
    }
}
