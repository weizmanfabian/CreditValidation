package com.weiz.motordedecision.util.exceptions.tecnica;

import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.exceptions.ExcepcionTecnica;

/**
 * La columna {@code estado} de una solicitud guardada trae un valor que no esta
 * en {@link EstadoSolicitud}.
 *
 * Es tecnica y no de negocio: quien escribe esa columna es el propio motor, asi
 * que un valor fuera del catalogo significa que el dato esta corrupto o que
 * alguien lo escribio por fuera —no que el cliente pidiera nada mal—. Repetir
 * la peticion daria el mismo resultado, pero la culpa es nuestra, y por eso se
 * responde 500 con el texto generico y el detalle se queda en el log
 * ({@code docs/error-handling.md} §4).
 *
 * Existe para que ningun {@link IllegalArgumentException} crudo salga de
 * {@code mapper} hacia el manejador: el contrato del modulo es que toda
 * excepcion propia hereda de una de las dos bases
 * ({@code docs/conventions.md} §6).
 */
public class EstadoDeSolicitudDesconocido extends ExcepcionTecnica {

    private static final String PLANTILLA = "La solicitud %s tiene el estado desconocido '%s'";

    /**
     * @param idSolicitud identificador de negocio de la solicitud afectada
     * @param estado valor que traia la columna
     * @param causa fallo original al traducir el valor al catalogo
     */
    public EstadoDeSolicitudDesconocido(String idSolicitud, String estado, IllegalArgumentException causa) {
        super(PLANTILLA.formatted(idSolicitud, estado), causa);
    }
}
