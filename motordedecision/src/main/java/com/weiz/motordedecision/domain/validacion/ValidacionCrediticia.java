package com.weiz.motordedecision.domain.validacion;

/**
 * Una validacion crediticia: un aspecto de la solicitud, evaluado aparte.
 *
 * Es el Strategy sobre el que se monta {@link CadenaDeValidaciones}. Cada
 * implementacion es independiente de las demas —no se llaman entre si, no
 * comparten estado y no saben en que posicion de la cadena van—, asi que
 * agregar una validacion nueva es escribir una clase mas y sumarla a la lista
 * que recibe la cadena: ninguna de las existentes se toca (OCP).
 *
 * Java puro, sin anotaciones de framework: la configuracion que cada
 * validacion necesita entra por su constructor (docs/architecture.md §3,
 * regla 2).
 *
 * Una validacion que rechaza no lanza excepcion: devuelve un
 * {@link Veredicto}.
 */
public interface ValidacionCrediticia {

    /**
     * Devuelve el nombre con el que esta validacion queda registrada y viaja a
     * la respuesta. Es contrato publico: se lee en el JSON del enunciado.
     *
     * @return nombre de la validacion
     */
    String obtenerNombre();

    /**
     * Indica si esta validacion tiene los insumos que necesita para
     * pronunciarse. Por omision los tiene siempre; la sobrescriben las que
     * dependen de un informe del buro que puede no existir.
     *
     * @param solicitud datos de la solicitud a evaluar
     * @return {@code true} si la validacion se puede ejecutar
     */
    default boolean puedeEvaluarse(SolicitudEvaluada solicitud) {
        return true;
    }

    /**
     * Evalua su aspecto de la solicitud.
     *
     * @param solicitud datos de la solicitud a evaluar
     * @return el veredicto, aprobado o rechazado, siempre con su detalle
     */
    Veredicto validar(SolicitudEvaluada solicitud);
}
