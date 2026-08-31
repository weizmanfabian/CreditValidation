package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.exceptions.tecnica.EstadoDeSolicitudDesconocido;

import java.util.List;

/**
 * Convierte una solicitud persistida en la respuesta que ve el cliente.
 *
 * No sabe que secciones existen: pone el sobre —identificador, fecha y
 * estado— y deja que cada {@link AportanteDeSeccion} de la lista agregue la
 * suya si aplica. Por eso una seccion nueva no obliga a tocar esta clase ni las
 * secciones existentes: se escribe su aportante y se anade a la lista de
 * {@code ConfiguracionDeRespuesta}.
 *
 * El detalle esta en {@link #mapearARespuesta}.
 */
public class MapeadorDeSolicitud {

    /** Marca del estado ausente: ningun valor de {@code EstadoSolicitud} se llama asi. */
    private static final String ESTADO_AUSENTE = "(sin estado)";

    private final List<AportanteDeSeccion> aportantes;

    /**
     * @param aportantes las secciones que se ofrecen a aportar; el orden no
     *                   altera el resultado, porque cada una escribe la suya
     */
    public MapeadorDeSolicitud(List<AportanteDeSeccion> aportantes) {
        this.aportantes = List.copyOf(aportantes);
    }

    /**
     * Arma la respuesta de una solicitud ya evaluada y persistida.
     *
     * @param solicitud solicitud con su estado, su score y su rastro
     * @return la respuesta con las secciones que aplican al caso
     */
    public SolicitudCreditoResponse mapearARespuesta(Solicitud solicitud) {
        EstadoSolicitud estado = resolverEstado(solicitud);
        SolicitudCreditoResponse.Builder constructor = SolicitudCreditoResponse.crearConstructorPara(
                solicitud.getIdSolicitud(), solicitud.getFechaCreacion(), estado);

        // Cada aportante decide por su cuenta si su seccion aplica a este estado
        aportantes.forEach(aportante -> aportante.aportarA(constructor, solicitud, estado));

        return constructor.construir();
    }

    /**
     * Traduce la columna {@code estado} al catalogo del motor.
     *
     * Un valor fuera del catalogo no puede salir como
     * {@link IllegalArgumentException} crudo: el manejador lo tomaria por un
     * error del cliente y responderia 400. Se envuelve en una excepcion tecnica
     * del modulo, que responde 500 y deja la causa en el log
     * ({@code docs/conventions.md} §6).
     *
     * El estado nulo se traduce a un texto que ningun valor del enum tiene, para
     * que siga el mismo camino que un valor desconocido en vez de reventar con
     * un {@link NullPointerException}.
     */
    private static EstadoSolicitud resolverEstado(Solicitud solicitud) {
        String estado = solicitud.getEstado();
        try {
            return EstadoSolicitud.valueOf(estado == null ? ESTADO_AUSENTE : estado);
        } catch (IllegalArgumentException causa) {
            throw new EstadoDeSolicitudDesconocido(solicitud.getIdSolicitud(), estado, causa);
        }
    }
}
