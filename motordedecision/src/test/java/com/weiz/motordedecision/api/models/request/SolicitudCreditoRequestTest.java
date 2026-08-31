package com.weiz.motordedecision.api.models.request;

import com.weiz.motordedecision.util.enums.TipoDocumento;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import static com.weiz.motordedecision.api.models.request.PeticionesDePrueba.APELLIDOS;
import static com.weiz.motordedecision.api.models.request.PeticionesDePrueba.CELULAR;
import static com.weiz.motordedecision.api.models.request.PeticionesDePrueba.CORREO;
import static com.weiz.motordedecision.api.models.request.PeticionesDePrueba.INGRESOS_MENSUALES;
import static com.weiz.motordedecision.api.models.request.PeticionesDePrueba.MONTO_SOLICITADO;
import static com.weiz.motordedecision.api.models.request.PeticionesDePrueba.NOMBRES;
import static com.weiz.motordedecision.api.models.request.PeticionesDePrueba.NUMERO_DOCUMENTO;
import static com.weiz.motordedecision.api.models.request.PeticionesDePrueba.PLAZO_MESES;
import static com.weiz.motordedecision.api.models.request.PeticionesDePrueba.crearPeticionValida;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba la validacion declarativa de {@link SolicitudCreditoRequest} con un
 * {@link Validator} de Jakarta, sin levantar Spring
 * ({@code docs/error-handling.md} §8).
 *
 * Los limites del enunciado se fijan por parejas: el ultimo valor que pasa y
 * el primero que no (1.000.000/999.999 y 50.000.000/50.000.001), que es lo que
 * delataria un umbral movido.
 */
class SolicitudCreditoRequestTest {

    private static final String MENSAJE_MONTO_MINIMO = "Monto solicitado debe ser mayor o igual a 1000000";
    private static final String MENSAJE_MONTO_MAXIMO = "Monto solicitado no debe exceder 50000000";
    private static final String MENSAJE_PLAZO = "Plazo en meses debe ser 12, 24, 36 o 48";

    private static ValidatorFactory fabricaDeValidadores;
    private static Validator validador;

    @BeforeAll
    static void construirValidador() {
        fabricaDeValidadores = Validation.buildDefaultValidatorFactory();
        validador = fabricaDeValidadores.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabricaDeValidadores.close();
    }

    @Test
    @DisplayName("La peticion del ejemplo del enunciado no produce violaciones")
    void validar_conPeticionValida_noProduceViolaciones() {
        SolicitudCreditoRequest peticion = crearPeticionValida();

        Set<ConstraintViolation<SolicitudCreditoRequest>> violaciones = validador.validate(peticion);

        assertThat(violaciones).isEmpty();
    }

    @ParameterizedTest(name = "monto {0} es valido")
    @CsvSource({"1000000", "50000000"})
    @DisplayName("Los dos extremos del rango de monto del enunciado se aceptan")
    void validar_conMontoEnElLimiteDelRango_noProduceViolaciones(BigDecimal monto) {
        SolicitudCreditoRequest peticion = crearPeticionConMonto(monto);

        Set<ConstraintViolation<SolicitudCreditoRequest>> violaciones = validador.validate(peticion);

        assertThat(violaciones).isEmpty();
    }

    @ParameterizedTest(name = "plazo {0} meses es valido")
    @CsvSource({"12", "24", "36", "48"})
    @DisplayName("Los cuatro plazos del enunciado se aceptan")
    void validar_conCadaPlazoDelEnunciado_noProduceViolaciones(int plazoMeses) {
        SolicitudCreditoRequest peticion = crearPeticionConPlazo(plazoMeses);

        Set<ConstraintViolation<SolicitudCreditoRequest>> violaciones = validador.validate(peticion);

        assertThat(violaciones).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("crearPeticionesConUnCampoInvalido")
    @DisplayName("Cada campo invalido se reporta con su nombre y su mensaje en espanol")
    void validar_conUnCampoInvalido_reportaElCampoConSuMensaje(String escenario,
                                                               SolicitudCreditoRequest peticion,
                                                               String campoEsperado,
                                                               String mensajeEsperado) {

        Set<ConstraintViolation<SolicitudCreditoRequest>> violaciones = validador.validate(peticion);

        assertThat(violaciones)
                .as(escenario)
                .singleElement()
                .satisfies(violacion -> assertThat(violacion.getPropertyPath()).hasToString(campoEsperado))
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo(mensajeEsperado);
    }

    @Test
    @DisplayName("Con varios campos invalidos se reportan todos, no solo el primero")
    void validar_conVariosCamposInvalidos_reportaTodosLosCampos() {
        SolicitudCreditoRequest peticion = new SolicitudCreditoRequest(
                null, "12", null, null, "correo-sin-arroba", "1234567890",
                new BigDecimal("500000"), 18, BigDecimal.ZERO);

        Set<ConstraintViolation<SolicitudCreditoRequest>> violaciones = validador.validate(peticion);

        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactlyInAnyOrder("tipoDocumento", "numeroDocumento", "nombres", "apellidos",
                        "correo", "celular", "montoSolicitado", "plazoMeses", "ingresosMensuales");
    }

    private static Stream<Arguments> crearPeticionesConUnCampoInvalido() {
        return Stream.of(
                Arguments.of("tipo de documento ausente", crearPeticionConTipoDocumento(null),
                        "tipoDocumento", "Tipo de documento es requerido y debe ser CC, CE o PA"),
                Arguments.of("numero de documento ausente", crearPeticionConNumeroDocumento(null),
                        "numeroDocumento", "Numero de documento es requerido y debe tener entre 6 y 15 digitos numericos"),
                Arguments.of("numero de documento corto", crearPeticionConNumeroDocumento("12345"),
                        "numeroDocumento", "Numero de documento debe tener entre 6 y 15 digitos numericos"),
                Arguments.of("numero de documento con letras", crearPeticionConNumeroDocumento("abc123456"),
                        "numeroDocumento", "Numero de documento debe tener entre 6 y 15 digitos numericos"),
                Arguments.of("nombres en blanco", crearPeticionConNombres("   "),
                        "nombres", "Nombres es requerido"),
                Arguments.of("nombres demasiado largos", crearPeticionConNombres("N".repeat(101)),
                        "nombres", "Nombres no debe exceder 100 caracteres"),
                Arguments.of("apellidos en blanco", crearPeticionConApellidos("   "),
                        "apellidos", "Apellidos es requerido"),
                Arguments.of("correo sin formato", crearPeticionConCorreo("correo-sin-arroba"),
                        "correo", "Correo debe tener un formato valido, por ejemplo persona@dominio.com"),
                Arguments.of("correo ausente", crearPeticionConCorreo(null),
                        "correo", "Correo es requerido"),
                Arguments.of("celular que no empieza por 3", crearPeticionConCelular("1234567890"),
                        "celular", "Celular debe iniciar con 3 y tener 10 digitos"),
                Arguments.of("celular ausente", crearPeticionConCelular(null),
                        "celular", "Celular es requerido"),
                Arguments.of("monto un peso por debajo del minimo", crearPeticionConMonto(new BigDecimal("999999")),
                        "montoSolicitado", MENSAJE_MONTO_MINIMO),
                Arguments.of("monto un peso por encima del maximo", crearPeticionConMonto(new BigDecimal("50000001")),
                        "montoSolicitado", MENSAJE_MONTO_MAXIMO),
                Arguments.of("monto ausente", crearPeticionConMonto(null),
                        "montoSolicitado", "Monto solicitado es requerido"),
                Arguments.of("plazo fuera del conjunto", crearPeticionConPlazo(18),
                        "plazoMeses", MENSAJE_PLAZO),
                Arguments.of("plazo ausente", crearPeticionConPlazo(null),
                        "plazoMeses", "Plazo en meses es requerido"),
                Arguments.of("ingresos en cero", crearPeticionConIngresos(BigDecimal.ZERO),
                        "ingresosMensuales", "Ingresos mensuales debe ser mayor que cero"),
                Arguments.of("ingresos ausentes", crearPeticionConIngresos(null),
                        "ingresosMensuales", "Ingresos mensuales es requerido"));
    }

    private static SolicitudCreditoRequest crearPeticionConTipoDocumento(TipoDocumento tipoDocumento) {
        return new SolicitudCreditoRequest(tipoDocumento, NUMERO_DOCUMENTO, NOMBRES, APELLIDOS,
                CORREO, CELULAR, MONTO_SOLICITADO, PLAZO_MESES, INGRESOS_MENSUALES);
    }

    private static SolicitudCreditoRequest crearPeticionConNumeroDocumento(String numeroDocumento) {
        return new SolicitudCreditoRequest(TipoDocumento.CC, numeroDocumento, NOMBRES, APELLIDOS,
                CORREO, CELULAR, MONTO_SOLICITADO, PLAZO_MESES, INGRESOS_MENSUALES);
    }

    private static SolicitudCreditoRequest crearPeticionConNombres(String nombres) {
        return new SolicitudCreditoRequest(TipoDocumento.CC, NUMERO_DOCUMENTO, nombres, APELLIDOS,
                CORREO, CELULAR, MONTO_SOLICITADO, PLAZO_MESES, INGRESOS_MENSUALES);
    }

    private static SolicitudCreditoRequest crearPeticionConApellidos(String apellidos) {
        return new SolicitudCreditoRequest(TipoDocumento.CC, NUMERO_DOCUMENTO, NOMBRES, apellidos,
                CORREO, CELULAR, MONTO_SOLICITADO, PLAZO_MESES, INGRESOS_MENSUALES);
    }

    private static SolicitudCreditoRequest crearPeticionConCorreo(String correo) {
        return new SolicitudCreditoRequest(TipoDocumento.CC, NUMERO_DOCUMENTO, NOMBRES, APELLIDOS,
                correo, CELULAR, MONTO_SOLICITADO, PLAZO_MESES, INGRESOS_MENSUALES);
    }

    private static SolicitudCreditoRequest crearPeticionConCelular(String celular) {
        return new SolicitudCreditoRequest(TipoDocumento.CC, NUMERO_DOCUMENTO, NOMBRES, APELLIDOS,
                CORREO, celular, MONTO_SOLICITADO, PLAZO_MESES, INGRESOS_MENSUALES);
    }

    private static SolicitudCreditoRequest crearPeticionConMonto(BigDecimal montoSolicitado) {
        return new SolicitudCreditoRequest(TipoDocumento.CC, NUMERO_DOCUMENTO, NOMBRES, APELLIDOS,
                CORREO, CELULAR, montoSolicitado, PLAZO_MESES, INGRESOS_MENSUALES);
    }

    private static SolicitudCreditoRequest crearPeticionConPlazo(Integer plazoMeses) {
        return new SolicitudCreditoRequest(TipoDocumento.CC, NUMERO_DOCUMENTO, NOMBRES, APELLIDOS,
                CORREO, CELULAR, MONTO_SOLICITADO, plazoMeses, INGRESOS_MENSUALES);
    }

    private static SolicitudCreditoRequest crearPeticionConIngresos(BigDecimal ingresosMensuales) {
        return new SolicitudCreditoRequest(TipoDocumento.CC, NUMERO_DOCUMENTO, NOMBRES, APELLIDOS,
                CORREO, CELULAR, MONTO_SOLICITADO, PLAZO_MESES, ingresosMensuales);
    }
}
