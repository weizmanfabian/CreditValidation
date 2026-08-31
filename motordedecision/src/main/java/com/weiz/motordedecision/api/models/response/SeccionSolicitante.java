package com.weiz.motordedecision.api.models.response;

/**
 * Quien pide el credito, tal como lo publica el enunciado (linea 100).
 *
 * Lleva el documento en un solo campo —{@code "CC 1234567890"}, tipo y numero
 * separados por un espacio— porque asi lo escribe el enunciado. La entidad los
 * guarda en dos columnas; unirlos es trabajo de la seccion, no del modelo de
 * datos.
 *
 * @param nombre nombres y apellidos del solicitante
 * @param documento tipo y numero de documento, en ese orden
 */
public record SeccionSolicitante(String nombre, String documento) {
}
