/**
 * Subclases concretas de {@link com.weiz.buro.util.exceptions.ExcepcionTecnica}:
 * recursos o dependencias que fallan y se responden con 500 generico.
 *
 * El buro es un simulador sin base de datos ni dependencias externas, asi que
 * hoy no tiene ninguna: la base y su manejador son la red de seguridad para
 * cuando aparezca la primera. Ver D-013 en {@code docs/decisions.md}.
 */
package com.weiz.buro.util.exceptions.tecnica;
