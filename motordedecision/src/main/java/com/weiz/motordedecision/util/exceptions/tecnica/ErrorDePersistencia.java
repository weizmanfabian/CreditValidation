package com.weiz.motordedecision.util.exceptions.tecnica;

import com.weiz.motordedecision.util.exceptions.ExcepcionTecnica;
import org.springframework.dao.DataAccessException;

/**
 * La base de datos no atendio la operacion: conexion caida, restriccion violada
 * o cualquier otro fallo del acceso a datos.
 *
 * Envuelve la {@link DataAccessException} de Spring Data para que los
 * manejadores no queden acoplados a la libreria de acceso a datos
 * ({@code docs/conventions.md} §6). El constructor exige esa causa por tipo: la
 * traza original es el unico dato que sirve para diagnosticar, y va al log, no
 * a la respuesta.
 */
public class ErrorDePersistencia extends ExcepcionTecnica {

    /**
     * @param mensaje operacion que se intentaba, para el log
     * @param causa fallo original del acceso a datos
     */
    public ErrorDePersistencia(String mensaje, DataAccessException causa) {
        super(mensaje, causa);
    }
}
