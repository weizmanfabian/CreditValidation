/**
 * Acceso a datos de las entidades de domain/entities.
 *
 * Ademas del repositorio de Spring Data, aqui viven las dos piezas que la
 * persistencia de solicitudes necesita y que no caben en una interfaz de
 * repositorio: el calculo del identificador de negocio
 * ({@link com.weiz.motordedecision.domain.dataaccessors.GeneradorIdSolicitud})
 * y el envoltorio de los fallos de la base en excepciones del modulo
 * ({@link com.weiz.motordedecision.domain.dataaccessors.SolicitudDataAccessor}).
 */
package com.weiz.motordedecision.domain.dataaccessors;
