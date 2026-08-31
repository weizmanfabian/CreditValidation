package com.weiz.motordedecision.api.models.request;

import com.weiz.motordedecision.util.enums.TipoDocumento;
import com.weiz.motordedecision.util.validators.PlazoPermitido;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo de la radicacion de una solicitud: {@code POST /api/solicitudes}.
 *
 * Son los ocho datos del formulario del enunciado (lineas 17-25), con los
 * nombres y apellidos separados porque asi los guarda la tabla
 * {@code solicitud} y asi los vuelve a unir la respuesta.
 *
 * Cada anotacion lleva su mensaje en espanol, redactado para que lo lea una
 * persona: nombra el dato en palabras y dice que se espera de el. El
 * identificador tecnico del campo no se repite en el texto porque ya viaja en
 * {@code field} de la respuesta de error ({@code docs/error-handling.md} §2).
 *
 * Aqui se valida el formato, nada mas. Que el score alcance, que el monto quepa
 * en los ingresos o que el documento este bloqueado son reglas de negocio, y
 * esas viven en {@code domain/validacion} y devuelven un resultado en vez de un
 * error.
 *
 * @param tipoDocumento tipo de documento del solicitante
 * @param numeroDocumento numero de documento, solo digitos
 * @param nombres nombres del solicitante
 * @param apellidos apellidos del solicitante
 * @param correo correo electronico de contacto
 * @param celular telefono celular de contacto
 * @param montoSolicitado monto de credito pedido, en pesos
 * @param plazoMeses plazo pedido, en meses
 * @param ingresosMensuales ingresos mensuales declarados
 */
public record SolicitudCreditoRequest(

        @NotNull(message = "Tipo de documento es requerido y debe ser CC, CE o PA")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "Numero de documento es requerido y debe tener entre 6 y 15 digitos numericos")
        @Pattern(regexp = "^\\d{6,15}$",
                message = "Numero de documento debe tener entre 6 y 15 digitos numericos")
        String numeroDocumento,

        @NotBlank(message = "Nombres es requerido")
        @Size(max = 100, message = "Nombres no debe exceder 100 caracteres")
        String nombres,

        @NotBlank(message = "Apellidos es requerido")
        @Size(max = 100, message = "Apellidos no debe exceder 100 caracteres")
        String apellidos,

        @NotBlank(message = "Correo es requerido")
        @Email(message = "Correo debe tener un formato valido, por ejemplo persona@dominio.com")
        @Size(max = 255, message = "Correo no debe exceder 255 caracteres")
        String correo,

        @NotBlank(message = "Celular es requerido")
        @Pattern(regexp = "^3\\d{9}$", message = "Celular debe iniciar con 3 y tener 10 digitos")
        String celular,

        @NotNull(message = "Monto solicitado es requerido")
        @DecimalMin(value = "1000000", message = "Monto solicitado debe ser mayor o igual a 1000000")
        @DecimalMax(value = "50000000", message = "Monto solicitado no debe exceder 50000000")
        BigDecimal montoSolicitado,

        @NotNull(message = "Plazo en meses es requerido")
        @PlazoPermitido
        Integer plazoMeses,

        @NotNull(message = "Ingresos mensuales es requerido")
        @DecimalMin(value = "1", message = "Ingresos mensuales debe ser mayor que cero")
        BigDecimal ingresosMensuales) {
}
