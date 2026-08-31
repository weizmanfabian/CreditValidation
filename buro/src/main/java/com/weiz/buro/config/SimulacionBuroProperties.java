package com.weiz.buro.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Valores de la simulacion del buro, enlazados desde el prefijo
 * {@code buro.simulacion} de {@code application.yml}.
 *
 * El documento que dispara el caso caido y la duracion del retardo son
 * configuracion, no literales del codigo ({@code docs/conventions.md} §9): asi
 * se ajusta el escenario para una demostracion o para los tests sin recompilar.
 *
 * Se validan al arrancar: un documento que no sea una cadena de digitos haria
 * fallar al generador en tiempo de peticion, y es mejor que el contexto no
 * levante a que el endpoint responda 500.
 *
 * @param documentoServicioCaido documento que representa el servicio caido
 * @param retardoServicioCaido cuanto se retrasa la respuesta de ese documento
 */
@Validated
@ConfigurationProperties(prefix = "buro.simulacion")
public record SimulacionBuroProperties(

        @NotBlank(message = "buro.simulacion.documento-servicio-caido es requerido")
        @Pattern(regexp = "^\\d{6,15}$",
                message = "buro.simulacion.documento-servicio-caido debe tener entre 6 y 15 digitos numericos")
        String documentoServicioCaido,

        @NotNull(message = "buro.simulacion.retardo-servicio-caido es requerido")
        Duration retardoServicioCaido) {
}
