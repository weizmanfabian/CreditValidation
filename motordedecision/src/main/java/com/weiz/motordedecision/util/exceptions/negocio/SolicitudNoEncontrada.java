package com.weiz.motordedecision.util.exceptions.negocio;

import com.weiz.motordedecision.util.exceptions.ExcepcionNegocio;

/**
 * No hay ninguna solicitud radicada para el documento consultado.
 *
 * Es de negocio y no tecnica por la prueba de {@code docs/error-handling.md}
 * §4.1: repetir la misma consulta dentro de un minuto daria exactamente el
 * mismo resultado, asi que la culpa es de la peticion y no del servicio. El
 * manejador la traduce a 404 con el codigo
 * {@link CodigoErrorNegocio#SOLICITUD_NO_ENCONTRADA}.
 */
public class SolicitudNoEncontrada extends ExcepcionNegocio {

    private static final String PLANTILLA_DETALLE = "No hay solicitudes para %s %s";

    /**
     * @param tipoDocumento tipo de documento consultado, tal como llego en la ruta
     * @param numeroDocumento numero de documento consultado
     */
    public SolicitudNoEncontrada(String tipoDocumento, String numeroDocumento) {
        super(CodigoErrorNegocio.SOLICITUD_NO_ENCONTRADA,
                PLANTILLA_DETALLE.formatted(tipoDocumento, numeroDocumento));
    }
}
