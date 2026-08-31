package com.weiz.motordedecision.domain.dataaccessors;

import com.weiz.motordedecision.domain.entities.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code solicitud}.
 *
 * Solo declara las dos consultas que el motor necesita: la busqueda por
 * documento de la consulta de estado y la ultima solicitud radicada en un dia,
 * que es de donde sale el consecutivo del identificador de negocio
 * ({@link GeneradorIdSolicitud}).
 */
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /**
     * @param tipoDocumento tipo de documento, tal como se guardo
     * @param numeroDocumento numero de documento, tal como se guardo
     * @return las solicitudes del documento, de la mas reciente a la mas
     *         antigua; lista vacia si no hay ninguna
     */
    List<Solicitud> findByTipoDocumentoAndNumeroDocumentoOrderByFechaCreacionDesc(
            String tipoDocumento, String numeroDocumento);

    /**
     * La ultima solicitud radicada en un dia, buscando por el prefijo
     * {@code SOL-yyyyMMdd-} del identificador de negocio.
     *
     * El orden descendente es alfabetico, y coincide con el numerico porque el
     * consecutivo va rellenado a tres digitos con ceros a la izquierda.
     *
     * @param prefijo prefijo del identificador de negocio, con su guion final
     * @return la solicitud con el consecutivo mas alto del dia, si hay alguna
     */
    Optional<Solicitud> findTopByIdSolicitudStartingWithOrderByIdSolicitudDesc(String prefijo);
}
