package com.weiz.buro.api.models.request;

import com.weiz.buro.util.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de la consulta al buro: {@code POST /api/buro/consulta}.
 *
 * Cada anotacion lleva su mensaje en espanol, redactado para que lo lea una
 * persona: nombra el dato en palabras y dice que se espera de el. El
 * identificador tecnico del campo no se repite en el texto porque ya viaja en
 * {@code field} de la respuesta de error ({@code docs/error-handling.md} §2).
 *
 * @param tipoDocumento tipo de documento del titular consultado
 * @param numeroDocumento numero de documento, solo digitos
 */
public record ConsultaBuroRequest(

        @NotNull(message = "Tipo de documento es requerido y debe ser CC, CE o PA")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "Numero de documento es requerido y debe tener entre 6 y 15 digitos numericos")
        @Pattern(regexp = "^\\d{6,15}$",
                message = "Numero de documento debe tener entre 6 y 15 digitos numericos")
        String numeroDocumento) {
}
