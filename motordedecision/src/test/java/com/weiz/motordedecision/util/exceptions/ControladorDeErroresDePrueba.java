package com.weiz.motordedecision.util.exceptions;

import com.weiz.motordedecision.util.exceptions.negocio.SolicitudNoEncontrada;
import com.weiz.motordedecision.util.exceptions.tecnica.BuroNoDisponible;
import com.weiz.motordedecision.util.exceptions.tecnica.ErrorDePersistencia;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Controlador de prueba que provoca una excepcion por cada familia de la tabla
 * de {@code docs/error-handling.md} §3.
 *
 * Vive en {@code src/test} a proposito: los endpoints reales del motor son las
 * features 13 y 14, y el manejador debe poder probarse sin esperarlas. El
 * cuerpo que recibe ({@link SolicitudDePrueba}) tampoco es el DTO de la feature
 * 13: es el minimo que hace falta para ejercitar la validacion de cuerpo con
 * dos campos.
 */
@RestController
@RequestMapping(ControladorDeErroresDePrueba.RUTA_BASE)
public class ControladorDeErroresDePrueba {

    static final String RUTA_BASE = "/prueba-errores";
    static final String NOMBRE_HEADER = "X-Correlacion";
    static final String NOMBRE_PARAMETRO = "numeroDocumento";
    static final String MENSAJE_ARGUMENTO_ILEGAL = "El identificador de la solicitud no puede ser negativo";
    static final String TIPO_DOCUMENTO_CONSULTADO = "CC";
    static final String NUMERO_DOCUMENTO_CONSULTADO = "1234567890";
    static final String DETALLE_SOLICITUD_NO_ENCONTRADA = "No hay solicitudes para CC 1234567890";
    static final String MENSAJE_BURO_NO_DISPONIBLE = "El servicio de buro no respondio la consulta del titular";
    static final String MENSAJE_PERSISTENCIA = "Fallo al guardar la solicitud radicada";
    static final String MENSAJE_CAUSA_DEL_BURO = "connect timed out";
    static final String MENSAJE_CAUSA_DE_PERSISTENCIA = "no se pudo obtener una conexion del pool";
    static final String MENSAJE_INESPERADO_INTERNO = "Indice fuera de rango en la cadena de validaciones";

    private static final String NUMERO_DOCUMENTO_INVALIDO = "abc";
    private static final BigDecimal MONTO_VALIDO = new BigDecimal("2000000");

    /**
     * Cuerpo minimo con dos campos validados, para comprobar que un envio con
     * varios campos malos devuelve todos y no solo el primero.
     *
     * Los mensajes se redactan para una persona y no repiten el identificador
     * tecnico del campo, que ya viaja en {@code field} (D-021).
     */
    public record SolicitudDePrueba(

            @NotBlank(message = "Numero de documento es requerido")
            @Pattern(regexp = "^\\d{6,15}$",
                    message = "Numero de documento debe tener entre 6 y 15 digitos numericos")
            String numeroDocumento,

            @NotNull(message = "Monto solicitado es requerido")
            @DecimalMin(value = "1000000", message = "Monto solicitado debe ser mayor o igual a 1000000")
            BigDecimal montoSolicitado) {
    }

    @PostMapping("/cuerpo")
    public SolicitudDePrueba recibirCuerpo(@Valid @RequestBody SolicitudDePrueba solicitud) {
        return solicitud;
    }

    @GetMapping("/header")
    public String recibirHeader(@RequestHeader(NOMBRE_HEADER) String correlacion) {
        return correlacion;
    }

    @GetMapping("/parametro")
    public String recibirParametro(@RequestParam(NOMBRE_PARAMETRO) String numeroDocumento) {
        return numeroDocumento;
    }

    @GetMapping("/ruta/{identificador}")
    public Integer recibirRuta(@PathVariable("identificador") Integer identificador) {
        return identificador;
    }

    @GetMapping("/parametro-anotado")
    public String recibirParametroAnotado(
            @RequestParam("correo") @NotBlank(message = "Correo es requerido") String correo) {
        return correo;
    }

    /**
     * Reproduce una validacion de metodo lanzada fuera del enlace de parametros,
     * como la que haria un servicio anotado con {@code @Validated}.
     *
     * Se valida a proposito el ejecutable y no el objeto suelto: solo asi el
     * property path llega en varios tramos ({@code metodo.argumento.campo}), que
     * es el caso que el manejador tiene que recortar antes de responder.
     */
    @GetMapping("/restriccion")
    public void lanzarViolacionDeRestriccion() throws NoSuchMethodException {
        Method metodoValidado = ControladorDeErroresDePrueba.class
                .getMethod("recibirCuerpo", SolicitudDePrueba.class);
        Object[] argumentos = {new SolicitudDePrueba(NUMERO_DOCUMENTO_INVALIDO, MONTO_VALIDO)};

        try (ValidatorFactory fabricaDeValidadores = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<ControladorDeErroresDePrueba>> violaciones = fabricaDeValidadores.getValidator()
                    .forExecutables().validateParameters(this, metodoValidado, argumentos);

            throw new ConstraintViolationException(violaciones);
        }
    }

    @GetMapping("/argumento-ilegal")
    public void lanzarArgumentoIlegal() {
        throw new IllegalArgumentException(MENSAJE_ARGUMENTO_ILEGAL);
    }

    @GetMapping("/negocio")
    public void lanzarErrorDeNegocio() {
        throw new SolicitudNoEncontrada(TIPO_DOCUMENTO_CONSULTADO, NUMERO_DOCUMENTO_CONSULTADO);
    }

    @GetMapping("/buro-caido")
    public void lanzarBuroNoDisponible() {
        throw new BuroNoDisponible(MENSAJE_BURO_NO_DISPONIBLE,
                new ResourceAccessException(MENSAJE_CAUSA_DEL_BURO));
    }

    @GetMapping("/persistencia")
    public void lanzarErrorDePersistencia() {
        throw new ErrorDePersistencia(MENSAJE_PERSISTENCIA,
                new DataAccessResourceFailureException(MENSAJE_CAUSA_DE_PERSISTENCIA));
    }

    @GetMapping("/inesperado")
    public void lanzarErrorInesperado() {
        throw new IllegalStateException(MENSAJE_INESPERADO_INTERNO);
    }
}
