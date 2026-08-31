package com.weiz.motordedecision.domain.dataaccessors;

import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.exceptions.tecnica.ErrorDePersistencia;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Puerta de entrada a la persistencia de solicitudes.
 *
 * Existe por dos motivos que no caben en un repositorio de Spring Data: asignar
 * el identificador de negocio antes de guardar ({@link GeneradorIdSolicitud}) y
 * envolver toda {@link DataAccessException} en un {@link ErrorDePersistencia},
 * para que ninguna excepcion de la libreria de acceso a datos escape a las capas
 * de arriba ({@code docs/conventions.md} §6).
 *
 * El detalle de la radicacion esta en {@link #guardarSolicitud}.
 */
@Component
public class SolicitudDataAccessor {

    private static final String FALLO_AL_GUARDAR = "No se pudo guardar la solicitud";
    private static final String FALLO_AL_BUSCAR = "No se pudo consultar las solicitudes del documento";

    private final SolicitudRepository solicitudRepository;
    private final GeneradorIdSolicitud generadorIdSolicitud;

    public SolicitudDataAccessor(SolicitudRepository solicitudRepository,
                                 GeneradorIdSolicitud generadorIdSolicitud) {
        this.solicitudRepository = solicitudRepository;
        this.generadorIdSolicitud = generadorIdSolicitud;
    }

    /**
     * Guarda la solicitud y su rastro de validaciones en la misma transaccion.
     *
     * Pasos:
     * - Si la solicitud llega sin fecha de creacion, se le pone la de ahora. La
     *   fecha y el consecutivo tienen que hablar del mismo dia, asi que se
     *   resuelven juntos y en este orden.
     * - Si llega sin identificador de negocio, se le asigna el siguiente libre
     *   de ese dia. El que ya trae uno lo conserva: es lo que permite reponer
     *   datos con identificadores conocidos.
     * - Se vuelca a la base de inmediato ({@code saveAndFlush}) para que un
     *   choque de identificadores salte aqui, dentro del {@code try}, y no al
     *   cerrar la transaccion, donde ya no se podria envolver.
     *
     * Dos radicaciones del mismo dia que compitan por el consecutivo terminan
     * con la segunda violando la restriccion {@code UNIQUE} de
     * {@code id_solicitud}: falla en voz alta con un {@link ErrorDePersistencia}
     * —un 500— en vez de duplicar el identificador de negocio. Reintentar con el
     * siguiente consecutivo exige una transaccion nueva por intento y es
     * responsabilidad de quien abre la transaccion, no de este componente.
     *
     * @param solicitud solicitud a radicar, con o sin identificador de negocio
     * @return la solicitud persistida, ya con su clave tecnica
     * @throws ErrorDePersistencia si la base de datos rechaza la insercion
     */
    @Transactional
    public Solicitud guardarSolicitud(Solicitud solicitud) {
        asegurarFechaDeCreacion(solicitud);
        asegurarIdentificadorDeNegocio(solicitud);

        try {
            return solicitudRepository.saveAndFlush(solicitud);
        } catch (DataAccessException causa) {
            throw new ErrorDePersistencia(FALLO_AL_GUARDAR, causa);
        }
    }

    /**
     * @param tipoDocumento tipo de documento consultado
     * @param numeroDocumento numero de documento consultado
     * @return las solicitudes del documento, de la mas reciente a la mas
     *         antigua; lista vacia si no hay ninguna
     * @throws ErrorDePersistencia si la base de datos no atiende la consulta
     */
    @Transactional(readOnly = true)
    public List<Solicitud> buscarPorDocumento(String tipoDocumento, String numeroDocumento) {
        try {
            return solicitudRepository.findByTipoDocumentoAndNumeroDocumentoOrderByFechaCreacionDesc(
                    tipoDocumento, numeroDocumento);
        } catch (DataAccessException causa) {
            throw new ErrorDePersistencia(FALLO_AL_BUSCAR, causa);
        }
    }

    private void asegurarFechaDeCreacion(Solicitud solicitud) {
        if (solicitud.getFechaCreacion() == null) {
            solicitud.setFechaCreacion(LocalDateTime.now());
        }
    }

    private void asegurarIdentificadorDeNegocio(Solicitud solicitud) {
        if (solicitud.getIdSolicitud() == null) {
            solicitud.setIdSolicitud(
                    generadorIdSolicitud.generarIdSolicitud(solicitud.getFechaCreacion().toLocalDate()));
        }
    }
}
