package com.weiz.motordedecision.util.enums;

/**
 * Tipos de documento que acepta el motor: cedula de ciudadania, cedula de
 * extranjeria y pasaporte.
 *
 * Es el mismo conjunto cerrado que publica el buro y el que exige la
 * restriccion {@code ck_solicitud_tipo_documento} del esquema. Al viajar como
 * enum y no como cadena, un valor fuera del catalogo se rechaza al deserializar
 * el cuerpo y nunca llega ni a la base de datos ni al buro.
 */
public enum TipoDocumento {

    CC,

    CE,

    PA
}
