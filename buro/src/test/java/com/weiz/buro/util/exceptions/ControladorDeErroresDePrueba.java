package com.weiz.buro.util.exceptions;

import com.weiz.buro.api.models.request.ConsultaBuroRequest;
import com.weiz.buro.util.enums.TipoDocumento;
import com.weiz.buro.util.exceptions.negocio.ExcepcionNegocioDePrueba;
import com.weiz.buro.util.exceptions.tecnica.ExcepcionTecnicaDePrueba;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Controlador de prueba que provoca una excepcion por cada familia de la tabla
 * de {@code docs/error-handling.md} §3.
 *
 * Vive en {@code src/test} a proposito: el endpoint real del buro es la feature
 * 5, y el manejador debe poder probarse sin esperarla.
 */
@RestController
@RequestMapping(ControladorDeErroresDePrueba.RUTA_BASE)
public class ControladorDeErroresDePrueba {

    static final String RUTA_BASE = "/prueba-errores";
    static final String NOMBRE_HEADER = "X-Correlacion";
    static final String NOMBRE_PARAMETRO = "numeroDocumento";
    static final String MENSAJE_ARGUMENTO_ILEGAL = "El identificador de la consulta no puede ser negativo";
    static final String MENSAJE_NEGOCIO = "El tipo de documento NIT no se puede consultar en el buro";
    static final String MENSAJE_TECNICO_INTERNO = "Fallo la conexion con el almacen de simulacion";
    static final String MENSAJE_CAUSA_ORIGINAL = "causa original";
    static final String MENSAJE_INESPERADO_INTERNO = "Indice fuera de rango en la tabla de simulacion";

    private static final String NUMERO_DOCUMENTO_INVALIDO = "abc";

    @PostMapping("/cuerpo")
    public ConsultaBuroRequest recibirCuerpo(@Valid @RequestBody ConsultaBuroRequest consulta) {
        return consulta;
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
            @RequestParam("correo") @NotBlank(message = "correo es requerido") String correo) {
        return correo;
    }

    /**
     * Reproduce una validacion lanzada fuera del enlace de parametros, como la
     * que haria un servicio sobre un objeto ya construido.
     */
    @GetMapping("/restriccion")
    public void lanzarViolacionDeRestriccion() {
        try (ValidatorFactory fabricaDeValidadores = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<ConsultaBuroRequest>> violaciones = fabricaDeValidadores.getValidator()
                    .validate(new ConsultaBuroRequest(TipoDocumento.CC, NUMERO_DOCUMENTO_INVALIDO));

            throw new ConstraintViolationException(violaciones);
        }
    }

    @GetMapping("/argumento-ilegal")
    public void lanzarArgumentoIlegal() {
        throw new IllegalArgumentException(MENSAJE_ARGUMENTO_ILEGAL);
    }

    @GetMapping("/negocio")
    public void lanzarErrorDeNegocio() {
        throw new ExcepcionNegocioDePrueba(MENSAJE_NEGOCIO);
    }

    @GetMapping("/tecnica")
    public void lanzarErrorTecnico() {
        throw new ExcepcionTecnicaDePrueba(MENSAJE_TECNICO_INTERNO,
                new IllegalStateException(MENSAJE_CAUSA_ORIGINAL));
    }

    @GetMapping("/inesperado")
    public void lanzarErrorInesperado() {
        throw new IllegalStateException(MENSAJE_INESPERADO_INTERNO);
    }
}
