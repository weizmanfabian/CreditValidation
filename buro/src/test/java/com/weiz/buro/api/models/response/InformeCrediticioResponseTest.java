package com.weiz.buro.api.models.response;

import com.weiz.buro.util.enums.EstadoTitular;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija el contrato JSON del informe contra el ejemplo del enunciado: los cuatro
 * campos con su nombre exacto, el estado como enum y la fecha con el formato
 * {@code yyyy-MM-dd HH:mm:ss} que impone el {@code @JsonFormat} del record.
 */
class InformeCrediticioResponseTest {

    private final JsonMapper mapeadorJson = JsonMapper.builder().build();

    @Test
    @DisplayName("El informe se serializa con score, estado, reporteNegativo y fechaConsulta")
    void serializar_conInformeCompleto_produceLosCuatroCamposDelEnunciado() {
        InformeCrediticioResponse informe = new InformeCrediticioResponse(
                750, EstadoTitular.ACTIVO, false, LocalDateTime.of(2026, 8, 24, 10, 30, 0));

        String json = mapeadorJson.writeValueAsString(informe);

        assertThat(json)
                .contains("\"score\":750")
                .contains("\"estado\":\"ACTIVO\"")
                .contains("\"reporteNegativo\":false")
                .contains("\"fechaConsulta\":\"2026-08-24 10:30:00\"");
    }
}
