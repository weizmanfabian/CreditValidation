/**
 * Resultado de dominio de la consulta al buro de credito.
 *
 * Es lo que el motor entiende del buro, ya despegado del transporte: ni un
 * {@code ResponseEntity} ni el DTO de {@code dto/buro} suben de
 * {@code infraestructura/client} (docs/architecture.md §3, regla 4). Java puro:
 * sin Spring, sin JPA y sin anotaciones de serializacion.
 */
package com.weiz.motordedecision.domain.buro;
