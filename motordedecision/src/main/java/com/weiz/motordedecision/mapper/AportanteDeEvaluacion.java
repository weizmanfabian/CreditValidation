package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.SeccionEvaluacion;
import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.api.models.response.ValidacionRealizada;
import com.weiz.motordedecision.domain.entities.ResultadoValidacion;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aporta la seccion {@code evaluacion}: el score del buro y el rastro que dejo
 * la cadena de validaciones.
 *
 * Viaja en todos los estados, tambien en el rechazo temprano. El enunciado
 * excluye de ese caso la tasa y el siguiente paso, no el motivo: decirle a
 * alguien que su solicitud se rechazo sin decirle que validacion la rechazo
 * seria peor respuesta, y el rastro es justo lo que el enunciado manda
 * registrar (linea 73).
 */
public class AportanteDeEvaluacion implements AportanteDeSeccion {

    @Override
    public void aportarA(SolicitudCreditoResponse.Builder constructor, Solicitud solicitud, EstadoSolicitud estado) {
        constructor.agregarEvaluacion(new SeccionEvaluacion(
                solicitud.getScoreBuro(),
                mapearValidaciones(solicitud.getResultados())));
    }

    private List<ValidacionRealizada> mapearValidaciones(List<ResultadoValidacion> resultados) {
        // Una solicitud recien construida puede no tener aun su lista de resultados
        return Optional.ofNullable(resultados).orElse(List.of()).stream()
                // Descartamos elementos nulos por seguridad adicional
                .filter(Objects::nonNull)
                // De cada fila persistida sale la validacion que ve el solicitante
                .map(resultado -> new ValidacionRealizada(
                        resultado.getNombre(), resultado.getResultado(), resultado.getDetalle()))
                // El orden es el que fijo la cadena y lo conserva el @OrderBy de la entidad
                .toList();
    }
}
