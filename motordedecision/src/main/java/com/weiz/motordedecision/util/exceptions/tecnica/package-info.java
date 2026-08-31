/**
 * Subclases concretas de
 * {@link com.weiz.motordedecision.util.exceptions.ExcepcionTecnica}: recursos o
 * dependencias que fallan y se responden con 500 generico, sin filtrar nada del
 * interior.
 *
 * Cada una envuelve por tipo la excepcion de terceros que le corresponde
 * —{@code RestClientException} el buro, {@code DataAccessException} la base de
 * datos—, de modo que la causa original nunca se pierde y ninguna excepcion de
 * una libreria escapa hacia el manejador (docs/conventions.md §6).
 */
package com.weiz.motordedecision.util.exceptions.tecnica;
