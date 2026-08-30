package com.weiz.buro.api.models.request;

import com.weiz.buro.util.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de la consulta al buro: {@code POST /api/buro/consulta}.
 *
 * Cada anotacion lleva su mensaje en espanol y ese mensaje nombra el campo, para
 * que una respuesta con varios errores diga de un vistazo cual corregir
 * ({@code docs/error-handling.md} §5).
 *
 * @param tipoDocumento tipo de documento del titular consultado
 * @param numeroDocumento numero de documento, solo digitos
 */
public record ConsultaBuroRequest(

        @NotNull(message = "tipoDocumento es requerido (CC, CE o PA)")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "numeroDocumento es requerido")
        @Pattern(regexp = "^\\d{6,15}$",
                message = "numeroDocumento debe tener entre 6 y 15 digitos numericos")
        String numeroDocumento) {
}
