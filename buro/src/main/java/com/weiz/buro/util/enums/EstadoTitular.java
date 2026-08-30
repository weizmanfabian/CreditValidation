package com.weiz.buro.util.enums;

/**
 * Estado del titular que reporta el buro simulado.
 *
 * El mock del enunciado tiene un unico discriminador —si el documento termina
 * en par o en impar— y ese mismo discriminador decide si hay reporte negativo.
 * Por eso el catalogo tiene dos valores y no mas: {@code ACTIVO} para el titular
 * al dia y {@code EN_MORA} para el que arrastra reporte negativo.
 */
public enum EstadoTitular {

    ACTIVO,
    EN_MORA
}
