/**
 * Excepciones propias del motor y el manejador que las traduce a HTTP.
 *
 * Se dividen en negocio (4xx, con codigo publicado) y tecnica (5xx, conserva
 * siempre la causa). Una regla de negocio que rechaza no lanza excepcion
 * (docs/error-handling.md §4).
 */
package com.weiz.motordedecision.util.exceptions;
