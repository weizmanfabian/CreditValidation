package com.weiz.motordedecision.dto.buro;

/**
 * Cuerpo que el motor envia al buro en {@code POST /api/buro/consulta}.
 *
 * Es el espejo del contrato que publica el modulo {@code buro}, no el modelo
 * del formulario del frontend. Viaja como cadenas: el catalogo de tipos de
 * documento del motor llega en la feature 11 y hasta entonces el dominio
 * acotado se mapea como {@code String} (D-035).
 *
 * @param tipoDocumento tipo de documento del titular, {@code CC}, {@code CE} o {@code PA}
 * @param numeroDocumento numero de documento del titular
 */
public record ConsultaBuroRequest(String tipoDocumento, String numeroDocumento) {
}
