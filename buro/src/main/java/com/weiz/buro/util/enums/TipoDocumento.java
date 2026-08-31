package com.weiz.buro.util.enums;

/**
 * Tipos de documento que acepta el buro: cedula de ciudadania, cedula de
 * extranjeria y pasaporte.
 *
 * Es el conjunto cerrado que el enunciado fija para la consulta. Al viajar como
 * enum y no como cadena, un valor fuera del catalogo se rechaza al deserializar
 * el cuerpo y nunca llega al dominio.
 */
public enum TipoDocumento {

    CC,
    CE,
    PA
}
