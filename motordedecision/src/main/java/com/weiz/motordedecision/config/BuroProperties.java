package com.weiz.motordedecision.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuracion del servicio de buro de credito.
 *
 * La direccion del buro nunca se escribe en el codigo: cambia entre el IDE
 * (localhost:8081) y el compose de la raiz, donde el buro es otro servicio de
 * la red de Docker (docs/architecture.md §6).
 *
 * Los dos tiempos de espera tambien son configuracion, y por eso son
 * obligatorios: el buro simulado no devuelve error para el documento del caso
 * caido, solo tarda. Sin un tiempo de espera de lectura el motor esperaria
 * indefinidamente y el patron de resiliencia no llegaria a dispararse nunca.
 *
 * @param url raiz del buro, sin la ruta del recurso
 * @param tiempoEsperaConexion cuanto se espera a abrir la conexion
 * @param tiempoEsperaLectura cuanto se espera la respuesta ya conectado
 */
@Validated
@ConfigurationProperties(prefix = "buro")
public record BuroProperties(

        @NotBlank String url,

        @NotNull Duration tiempoEsperaConexion,

        @NotNull Duration tiempoEsperaLectura) {
}
