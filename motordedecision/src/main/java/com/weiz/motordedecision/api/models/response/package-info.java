/**
 * Modelos de salida de la API.
 *
 * Se construyen por partes con el patron Builder: no todas las solicitudes
 * generan las mismas secciones de respuesta (docs/architecture.md §4). Las
 * entidades JPA nunca salen por HTTP: aqui llega lo que arma el mapper.
 */
package com.weiz.motordedecision.api.models.response;
