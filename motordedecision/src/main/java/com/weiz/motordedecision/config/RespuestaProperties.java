package com.weiz.motordedecision.config;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Lo que la respuesta dice en cada estado: la tasa que se ofrece y el texto del
 * siguiente paso.
 *
 * Los dos son datos de negocio y no literales del codigo
 * (docs/conventions.md §9): la tasa la mueve el area de riesgo y el texto lo
 * escribe quien atiende al cliente, ninguno de los dos recompilando.
 *
 * Las claves son estados, y **la ausencia significa algo**: un estado que no
 * este en {@code tasaEstimadaPorEstado} no lleva tasa, y uno que no este en
 * {@code siguientePasoPorEstado} no lleva siguiente paso. Ahi vive la regla del
 * rechazo temprano del enunciado, escrita como dato (D-047).
 *
 * Los dos mapas son obligatorios: uno vacio dejaria toda respuesta sin tasa y
 * sin siguiente paso, en silencio.
 *
 * @param tasaEstimadaPorEstado tasa mensual ofrecida en cada estado
 * @param siguientePasoPorEstado texto del siguiente paso en cada estado
 */
@Validated
@ConfigurationProperties(prefix = "respuesta")
public record RespuestaProperties(

        @NotEmpty Map<EstadoSolicitud, BigDecimal> tasaEstimadaPorEstado,

        @NotEmpty Map<EstadoSolicitud, String> siguientePasoPorEstado) {

    public RespuestaProperties {
        // Un mapa ausente se enlaza como nulo: lo dejamos vacio para que la
        // queja la ponga @NotEmpty al arrancar, y no un NullPointerException
        tasaEstimadaPorEstado = copiarOVaciar(tasaEstimadaPorEstado);
        siguientePasoPorEstado = copiarOVaciar(siguientePasoPorEstado);
    }

    private static <T> Map<EstadoSolicitud, T> copiarOVaciar(Map<EstadoSolicitud, T> valores) {
        return valores == null ? Map.of() : Map.copyOf(valores);
    }
}
