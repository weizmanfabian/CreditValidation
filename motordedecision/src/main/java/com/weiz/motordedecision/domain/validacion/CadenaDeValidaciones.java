package com.weiz.motordedecision.domain.validacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Ejecuta las validaciones en orden y se detiene en la primera que rechaza.
 *
 * Es la Chain of Responsibility del enunciado. El orden no lo decide Spring ni
 * el orden de escaneo de los componentes: lo decide la lista que recibe por
 * constructor, que se arma en {@code ConfiguracionCadenaDeValidaciones}. Se lee
 * de un vistazo y no depende de nada implicito.
 *
 * Es Java puro y no conoce ninguna validacion concreta, asi que agregar una
 * quinta no obliga a tocar esta clase ni las cuatro existentes: se escribe la
 * clase nueva y se suma a esa lista.
 *
 * El detalle de la ejecucion esta en {@link #evaluarSolicitud}.
 */
public class CadenaDeValidaciones {

    private final List<ValidacionCrediticia> validaciones;

    /**
     * @param validaciones las validaciones, en el orden exacto en que se ejecutan
     */
    public CadenaDeValidaciones(List<ValidacionCrediticia> validaciones) {
        this.validaciones = List.copyOf(validaciones);
    }

    /**
     * Evalua la solicitud recorriendo la cadena.
     *
     * La cadena se detiene por dos motivos, y ninguno de los dos es una
     * excepcion:
     * - Una validacion rechaza. Su registro es el ultimo de la lista, y las
     *   validaciones siguientes no llegan a ejecutarse.
     * - Una validacion no tiene con que pronunciarse, tipicamente porque el
     *   buro no respondio. No deja registro: no hubo evaluacion que registrar.
     *
     * Lo que devuelve es el rastro de lo que si se evaluo. Traducirlo a un
     * estado final es trabajo de las reglas de decision, no de la cadena.
     *
     * @param solicitud datos de la solicitud a evaluar
     * @return los registros producidos, en orden de ejecucion; vacio si la
     *         primera validacion no se pudo evaluar
     */
    public List<RegistroValidacion> evaluarSolicitud(SolicitudEvaluada solicitud) {
        List<RegistroValidacion> registros = new ArrayList<>();

        short orden = 0;
        for (ValidacionCrediticia validacion : validaciones) {
            if (!validacion.puedeEvaluarse(solicitud)) {
                break;
            }

            orden++;
            Veredicto veredicto = validacion.validar(solicitud);
            registros.add(new RegistroValidacion(orden, validacion.obtenerNombre(),
                    veredicto.resultado(), veredicto.detalle()));

            if (veredicto.esRechazo()) {
                break;
            }
        }

        return List.copyOf(registros);
    }
}
