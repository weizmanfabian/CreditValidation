package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;

/**
 * Quien sabe armar una seccion de la respuesta y decidir si esa seccion aplica
 * al caso.
 *
 * Es lo que hace extensible la construccion: una seccion nueva es una
 * implementacion mas y una linea en la lista de
 * {@code ConfiguracionDeRespuesta}. Las existentes no se enteran, porque
 * ninguna sabe de las demas ni del orden en que se ejecutan.
 *
 * Un aportante que no aplica al caso —el siguiente paso de un rechazo por
 * fraude, por ejemplo— simplemente no llama a ningun {@code agregarXxx}. Esa es
 * la unica forma de omitir una seccion: no hay banderas ni {@code null} de
 * relleno.
 */
public interface AportanteDeSeccion {

    /**
     * Agrega su seccion al constructor, si aplica al caso.
     *
     * @param constructor respuesta en construccion
     * @param solicitud solicitud persistida, con su rastro de validaciones
     * @param estado estado final, ya tipado
     */
    void aportarA(SolicitudCreditoResponse.Builder constructor, Solicitud solicitud, EstadoSolicitud estado);
}
