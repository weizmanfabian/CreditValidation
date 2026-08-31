package com.weiz.motordedecision.infraestructura.client;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;

/**
 * Consulta el buro de credito.
 *
 * El contrato es deliberadamente ciego al transporte: recibe los datos del
 * titular, devuelve un resultado de dominio y no lanza nada cuando el buro
 * falla. Quien la use no puede saber si detras hay HTTP, y por eso tampoco
 * puede filtrar un error de HTTP hacia el controlador
 * (docs/architecture.md §3, regla 4).
 */
public interface BuroClient {

    /**
     * Consulta el informe crediticio del titular.
     *
     * @param tipoDocumento tipo de documento del titular
     * @param numeroDocumento numero de documento del titular
     * @return el informe, o un resultado de buro no disponible si no se pudo obtener
     */
    ResultadoConsultaBuro consultarInforme(String tipoDocumento, String numeroDocumento);
}
