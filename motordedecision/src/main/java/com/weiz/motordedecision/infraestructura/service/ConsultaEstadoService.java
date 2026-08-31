package com.weiz.motordedecision.infraestructura.service;

import com.weiz.motordedecision.api.models.response.EstadoDeSolicitudResponse;
import com.weiz.motordedecision.util.enums.TipoDocumento;

import java.util.List;

/**
 * Consulta el estado de las solicitudes de un documento (enunciado §5).
 *
 * El controlador depende de esta interfaz y no de su implementacion, igual que
 * en la radicacion: la consulta puede cambiar sin tocar la capa que expone el
 * API (docs/architecture.md §4).
 */
public interface ConsultaEstadoService {

    /**
     * Busca todas las solicitudes radicadas con ese tipo y numero de documento.
     *
     * Un documento sin solicitudes no es un error: la respuesta es la lista
     * vacia con 200, porque preguntar por un documento que nunca ha radicado
     * es un escenario normal de la pantalla de consulta (D-053).
     *
     * @param tipoDocumento tipo de documento, ya validado contra el catalogo
     * @param numeroDocumento numero de documento tal como llego en la ruta
     * @return las solicitudes de la mas reciente a la mas antigua, con su
     *         estado actual y su fecha; lista vacia si no hay ninguna
     */
    List<EstadoDeSolicitudResponse> consultarPorDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
