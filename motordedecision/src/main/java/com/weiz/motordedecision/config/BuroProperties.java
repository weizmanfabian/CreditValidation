package com.weiz.motordedecision.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion del servicio de buro de credito.
 *
 * La direccion del buro nunca se escribe en el codigo: cambia entre el IDE
 * (localhost:8081) y el compose de la raiz, donde el buro es otro servicio de
 * la red de Docker (docs/architecture.md §6).
 *
 * @param url raiz del buro, sin la ruta del recurso
 */
@Validated
@ConfigurationProperties(prefix = "buro")
public record BuroProperties(@NotBlank String url) {
}
