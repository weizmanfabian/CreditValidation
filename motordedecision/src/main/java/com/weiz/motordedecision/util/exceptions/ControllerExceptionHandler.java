package com.weiz.motordedecision.util.exceptions;

import com.weiz.motordedecision.util.exceptions.negocio.CodigoErrorNegocio;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

/**
 * Traduce toda excepcion que sale de un controlador al cuerpo unico
 * {@link ErrorResponse} ({@code docs/error-handling.md} §3).
 *
 * Es el mismo manejador que publica el buro, con el catalogo de codigos del
 * motor: los dos modulos responden errores con la misma forma.
 *
 * Es la unica clase del modulo que conoce HTTP: aqui vive la traduccion de
 * {@link CodigoErrorNegocio} a estado, y no en el enum, para que el catalogo de
 * negocio siga sirviendo si el servicio se consume por otro transporte.
 *
 * Los 400 de validacion no se registran en el log: la respuesta ya lleva el
 * campo y el motivo, y el cliente los ve. Se registra lo que el cliente no
 * puede ver: {@code WARN} sin traza para el negocio en
 * {@link #manejarErrorDeNegocio}, y {@code ERROR} con la traza completa para lo
 * tecnico en {@link #manejarErrorTecnico} y {@link #manejarErrorInterno}.
 */
@RestControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    private static final String MENSAJE_DATOS_INVALIDOS = "Error en los datos proporcionados";
    private static final String MENSAJE_CUERPO_ILEGIBLE = "El cuerpo de la peticion no es un JSON valido";
    private static final String MENSAJE_ERROR_INTERNO = "Servicio temporalmente no disponible";
    private static final String MENSAJE_PETICION_NO_ATENDIBLE = "La peticion no se puede atender en este recurso";

    private static final String PLANTILLA_HEADER_REQUERIDO = "%s es requerido en la cabecera";
    private static final String PLANTILLA_PARAMETRO_REQUERIDO = "%s es requerido como parametro de la consulta";
    private static final String PLANTILLA_TIPO_INVALIDO = "%s no tiene un valor valido para el tipo %s";
    private static final String TIPO_DESCONOCIDO = "esperado";
    private static final String CAMPO_DESCONOCIDO = "parametro";
    private static final String CUERPO_SIN_CAUSA_LEGIBLE = "El cuerpo recibido no se pudo interpretar como JSON";

    /**
     * Traduccion de codigo de negocio a estado HTTP. Vive aqui, y no en el enum,
     * por la regla de {@code docs/error-handling.md} §4.3. Un codigo sin entrada
     * en la tabla responde 400, que es el 4xx por defecto de un error de negocio.
     */
    private static final Map<CodigoErrorNegocio, HttpStatus> ESTADOS_POR_CODIGO_DE_NEGOCIO =
            Map.of(CodigoErrorNegocio.SOLICITUD_NO_ENCONTRADA, HttpStatus.NOT_FOUND);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarErroresDeValidacion(MethodArgumentNotValidException excepcion) {
        // Cada restriccion incumplida del cuerpo produce su propio FieldError: se
        // devuelven todos los campos malos de un envio, no solo el primero
        List<FieldError> errores = excepcion.getBindingResult().getFieldErrors().stream()
                .map(error -> FieldError.crearDeCuerpo(error.getField(), error.getDefaultMessage()))
                .toList();

        return construirRespuestaDeCampos(errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> manejarErroresDeParametrosAnotados(
            HandlerMethodValidationException excepcion) {

        // Un resultado por parametro anotado del metodo del controlador
        List<FieldError> errores = excepcion.getParameterValidationResults().stream()
                // De cada parametro salen tantos errores como restricciones incumplio
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(error -> crearErrorSegunOrigen(resultado.getMethodParameter(),
                                resolverNombreDelParametro(resultado.getMethodParameter()),
                                error.getDefaultMessage())))
                .toList();

        return construirRespuestaDeCampos(errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarViolacionesDeRestriccion(ConstraintViolationException excepcion) {
        // El property path llega como "metodo.argumento.campo": al cliente solo le
        // sirve el ultimo tramo, que es el nombre del campo que envio
        List<FieldError> errores = excepcion.getConstraintViolations().stream()
                .map(violacion -> FieldError.crearDeCuerpo(
                        extraerUltimoTramo(violacion.getPropertyPath()), violacion.getMessage()))
                .toList();

        return construirRespuestaDeCampos(errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpMessageNotReadableException excepcion) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.crearConDetalles(MENSAJE_CUERPO_ILEGIBLE, resolverCausaLegible(excepcion)));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> manejarHeaderFaltante(MissingRequestHeaderException excepcion) {
        String nombre = excepcion.getHeaderName();
        FieldError error = FieldError.crearDeHeader(nombre, PLANTILLA_HEADER_REQUERIDO.formatted(nombre));

        return construirRespuestaDeCampos(List.of(error));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametroFaltante(
            MissingServletRequestParameterException excepcion) {

        String nombre = excepcion.getParameterName();
        FieldError error = FieldError.crearDeParametro(nombre, PLANTILLA_PARAMETRO_REQUERIDO.formatted(nombre));

        return construirRespuestaDeCampos(List.of(error));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoDeArgumentoInvalido(
            MethodArgumentTypeMismatchException excepcion) {

        String nombre = excepcion.getName();
        String mensaje = PLANTILLA_TIPO_INVALIDO.formatted(nombre, resolverTipoRequerido(excepcion));

        return construirRespuestaDeCampos(List.of(crearErrorSegunOrigen(excepcion.getParameter(), nombre, mensaje)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoIlegal(IllegalArgumentException excepcion) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.crearConDetalles(MENSAJE_DATOS_INVALIDOS, excepcion.getMessage()));
    }

    @ExceptionHandler(ExcepcionNegocio.class)
    public ResponseEntity<ErrorResponse> manejarErrorDeNegocio(ExcepcionNegocio excepcion) {
        CodigoErrorNegocio codigo = excepcion.obtenerCodigo();
        String detalles = excepcion.getMessage();

        log.warn("Regla de negocio incumplida [{}]: {}", codigo, detalles);

        return ResponseEntity.status(determinarEstadoHttp(codigo))
                .body(ErrorResponse.crearDeNegocio(codigo.name(), codigo.obtenerDescripcion(), detalles));
    }

    @ExceptionHandler(ExcepcionTecnica.class)
    public ResponseEntity<ErrorResponse> manejarErrorTecnico(ExcepcionTecnica excepcion) {
        log.error("Fallo tecnico atendiendo la peticion", excepcion);

        return construirRespuestaDeErrorInterno();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorInterno(Exception excepcion) {
        // Spring lanza excepciones que ya traen su propio estado de cliente: una
        // ruta inexistente (404), un verbo no soportado (405) o un tipo de
        // contenido invalido (415). Devolverlas como 500 disfrazaria un error de
        // la peticion de fallo del servicio, y falsearia las metricas de error.
        if (excepcion instanceof org.springframework.web.ErrorResponse errorConEstado
                && errorConEstado.getStatusCode().is4xxClientError()) {
            HttpStatusCode estado = errorConEstado.getStatusCode();
            log.warn("Peticion no atendible con estado {}", estado);

            return ResponseEntity.status(estado).body(ErrorResponse.crearConMensaje(MENSAJE_PETICION_NO_ATENDIBLE));
        }

        log.error("Fallo no previsto atendiendo la peticion", excepcion);

        return construirRespuestaDeErrorInterno();
    }

    private ResponseEntity<ErrorResponse> construirRespuestaDeCampos(List<FieldError> errores) {
        return ResponseEntity.badRequest().body(ErrorResponse.crearConCampos(MENSAJE_DATOS_INVALIDOS, errores));
    }

    /**
     * Construye el 500. El mensaje interno no viaja al cliente: exponerlo
     * filtraria detalles de implementacion, y ya quedo en el log con su traza.
     */
    private ResponseEntity<ErrorResponse> construirRespuestaDeErrorInterno() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.crearConMensaje(MENSAJE_ERROR_INTERNO));
    }

    private HttpStatus determinarEstadoHttp(CodigoErrorNegocio codigo) {
        return ESTADOS_POR_CODIGO_DE_NEGOCIO.getOrDefault(codigo, HttpStatus.BAD_REQUEST);
    }

    private FieldError crearErrorSegunOrigen(MethodParameter parametro, String campo, String mensaje) {
        if (parametro.hasParameterAnnotation(PathVariable.class)) {
            return FieldError.crearDeRuta(campo, mensaje);
        }
        if (parametro.hasParameterAnnotation(RequestHeader.class)) {
            return FieldError.crearDeHeader(campo, mensaje);
        }
        return FieldError.crearDeParametro(campo, mensaje);
    }

    private String resolverNombreDelParametro(MethodParameter parametro) {
        String nombre = parametro.getParameterName();
        return nombre != null ? nombre : CAMPO_DESCONOCIDO;
    }

    private String resolverTipoRequerido(MethodArgumentTypeMismatchException excepcion) {
        Class<?> tipo = excepcion.getRequiredType();
        return tipo != null ? tipo.getSimpleName() : TIPO_DESCONOCIDO;
    }

    /**
     * Extrae la primera linea de la causa mas concreta del parseo. Jackson anade
     * a partir de ahi la posicion y un volcado del origen, ruido que no orienta a
     * quien esta corrigiendo su JSON.
     */
    private String resolverCausaLegible(HttpMessageNotReadableException excepcion) {
        String mensaje = excepcion.getMostSpecificCause().getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return CUERPO_SIN_CAUSA_LEGIBLE;
        }
        return mensaje.split("\\R", 2)[0].trim();
    }

    /**
     * Devuelve el ultimo tramo del property path de una violacion, que es el
     * nombre del campo tal como lo envio el cliente.
     */
    private String extraerUltimoTramo(Path ruta) {
        String rutaCompleta = ruta.toString();
        return rutaCompleta.substring(rutaCompleta.lastIndexOf('.') + 1);
    }
}
