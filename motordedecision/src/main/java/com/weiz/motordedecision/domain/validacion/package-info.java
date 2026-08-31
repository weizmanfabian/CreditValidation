/**
 * Las validaciones crediticias y la cadena que las ejecuta en orden.
 *
 * Java puro: sin Spring, sin JPA, sin anotaciones de framework. Cada validacion
 * es una Strategy independiente y la cadena se detiene en la primera que
 * rechaza (docs/architecture.md §4).
 *
 * Una validacion que rechaza no lanza excepcion: devuelve un resultado.
 */
package com.weiz.motordedecision.domain.validacion;
