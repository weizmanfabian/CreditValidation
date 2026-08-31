package com.weiz.motordedecision.domain.validacion;

import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;

/**
 * Lo que una validacion responde: si aprueba o rechaza, y por que.
 *
 * No incluye el nombre de la validacion ni su posicion en la cadena. Eso lo
 * sabe la cadena, no la regla, y por eso lo anade
 * {@link CadenaDeValidaciones} al construir el {@link RegistroValidacion}. Asi
 * una validacion nueva no tiene que repetir su propio nombre en cada rama.
 *
 * Se construye por sus dos metodos de fabrica, que son los que dan nombre a
 * cada forma.
 *
 * @param resultado si la validacion aprobo o rechazo
 * @param detalle explicacion en lenguaje de negocio, la que ve el solicitante
 */
public record Veredicto(ResultadoEvaluacion resultado, String detalle) {

    /**
     * Crea el veredicto de una validacion que la solicitud supera.
     *
     * @param detalle explicacion de por que la supera
     * @return veredicto aprobado
     */
    public static Veredicto crearVeredictoAprobado(String detalle) {
        return new Veredicto(ResultadoEvaluacion.APROBADO, detalle);
    }

    /**
     * Crea el veredicto de una validacion que la solicitud no supera. Es un
     * resultado, no una excepcion: quien llama decide que hacer con el.
     *
     * @param detalle explicacion de por que no la supera
     * @return veredicto rechazado
     */
    public static Veredicto crearVeredictoRechazado(String detalle) {
        return new Veredicto(ResultadoEvaluacion.RECHAZADO, detalle);
    }

    /**
     * Indica si este veredicto detiene la cadena.
     *
     * @return {@code true} si la validacion rechazo la solicitud
     */
    public boolean esRechazo() {
        return resultado == ResultadoEvaluacion.RECHAZADO;
    }
}
