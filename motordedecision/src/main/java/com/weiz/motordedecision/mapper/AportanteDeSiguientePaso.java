package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;

import java.util.Map;

/**
 * Aporta la seccion {@code siguientePaso}: que tiene que pasar ahora.
 *
 * Es el unico aportante que a veces no aporta nada. Los textos entran por
 * constructor desde {@code respuesta.siguiente-paso-por-estado} (D-046), y un
 * estado que no este en ese mapa no tiene siguiente paso: la seccion no viaja.
 * Asi el rechazo temprano del enunciado se resuelve con un dato ausente y no
 * con un {@code if} que nombre a {@code RECHAZADO_FRAUDE}.
 */
public class AportanteDeSiguientePaso implements AportanteDeSeccion {

    private final Map<EstadoSolicitud, String> textosPorEstado;

    /**
     * @param textosPorEstado texto del siguiente paso en cada estado; los
     *                        estados ausentes no llevan siguiente paso
     */
    public AportanteDeSiguientePaso(Map<EstadoSolicitud, String> textosPorEstado) {
        this.textosPorEstado = Map.copyOf(textosPorEstado);
    }

    @Override
    public void aportarA(SolicitudCreditoResponse.Builder constructor, Solicitud solicitud, EstadoSolicitud estado) {
        String texto = textosPorEstado.get(estado);
        if (texto != null) {
            constructor.agregarSiguientePaso(texto);
        }
    }
}
