package com.weiz.buro.api.models.request;

import com.weiz.buro.util.enums.TipoDocumento;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba la validacion declarativa de {@link ConsultaBuroRequest} con un
 * {@link Validator} de Jakarta, sin levantar Spring
 * ({@code docs/error-handling.md} §8).
 */
class ConsultaBuroRequestTest {

    private static final String NUMERO_DOCUMENTO_VALIDO = "1234567890";
    private static final String DOCUMENTO_SERVICIO_CAIDO = "0000000000";
    private static final String CAMPO_NUMERO_DOCUMENTO = "numeroDocumento";
    private static final String CAMPO_TIPO_DOCUMENTO = "tipoDocumento";

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
    @DisplayName("Una consulta con tipo y numero de documento validos no produce violaciones")
    void validar_conConsultaValida_noProduceViolaciones() {
        ConsultaBuroRequest consulta = new ConsultaBuroRequest(TipoDocumento.CC, NUMERO_DOCUMENTO_VALIDO);

        Set<ConstraintViolation<ConsultaBuroRequest>> violaciones = validador.validate(consulta);

        assertThat(violaciones).isEmpty();
    }

    @ParameterizedTest(name = "tipoDocumento={0} es aceptado")
    @CsvSource({"CC", "CE", "PA"})
    @DisplayName("Los tres tipos de documento del enunciado se aceptan sin violaciones")
    void validar_conCadaTipoDocumentoDelCatalogo_noProduceViolaciones(TipoDocumento tipoDocumento) {
        ConsultaBuroRequest consulta = new ConsultaBuroRequest(tipoDocumento, NUMERO_DOCUMENTO_VALIDO);

        Set<ConstraintViolation<ConsultaBuroRequest>> violaciones = validador.validate(consulta);

        assertThat(violaciones).isEmpty();
    }

    @Test
    @DisplayName("El catalogo de tipos de documento es exactamente CC, CE y PA")
    void obtenerValores_delCatalogoDeTipoDocumento_devuelveSoloLosTresDelEnunciado() {
        TipoDocumento[] tipos = TipoDocumento.values();

        assertThat(tipos).containsExactly(TipoDocumento.CC, TipoDocumento.CE, TipoDocumento.PA);
    }

    @Test
    @DisplayName("El documento 0000000000 del caso caido es una entrada valida y no la rechaza el patron")
    void validar_conElDocumentoDelServicioCaido_noProduceViolaciones() {
        ConsultaBuroRequest consulta = new ConsultaBuroRequest(TipoDocumento.CC, DOCUMENTO_SERVICIO_CAIDO);

        Set<ConstraintViolation<ConsultaBuroRequest>> violaciones = validador.validate(consulta);

        assertThat(violaciones).isEmpty();
    }

    @Test
    @DisplayName("Sin tipoDocumento se reporta el campo con el mensaje que nombra los valores validos")
    void validar_sinTipoDocumento_reportaElCampoConSuMensaje() {
        ConsultaBuroRequest consulta = new ConsultaBuroRequest(null, NUMERO_DOCUMENTO_VALIDO);

        Set<ConstraintViolation<ConsultaBuroRequest>> violaciones = validador.validate(consulta);

        assertThat(violaciones)
                .singleElement()
                .satisfies(violacion -> assertThat(violacion.getPropertyPath())
                        .hasToString(CAMPO_TIPO_DOCUMENTO))
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("Tipo de documento es requerido y debe ser CC, CE o PA");
    }

    @ParameterizedTest(name = "numeroDocumento={0} produce \"{1}\"")
    @CsvSource(nullValues = "NULO", value = {
            "NULO,                Numero de documento es requerido y debe tener entre 6 y 15 digitos numericos",
            "'',                  Numero de documento es requerido y debe tener entre 6 y 15 digitos numericos",
            "12345,               Numero de documento debe tener entre 6 y 15 digitos numericos",
            "1234567890123456,    Numero de documento debe tener entre 6 y 15 digitos numericos",
            "1234-56789,          Numero de documento debe tener entre 6 y 15 digitos numericos",
            "ABCDEFGHIJ,          Numero de documento debe tener entre 6 y 15 digitos numericos"
    })
    @DisplayName("Un numero de documento invalido se reporta sobre su propio campo y en espanol")
    void validar_conNumeroDocumentoInvalido_reportaElCampoConSuMensaje(String numeroDocumento,
                                                                       String mensajeEsperado) {
        ConsultaBuroRequest consulta = new ConsultaBuroRequest(TipoDocumento.CC, numeroDocumento);

        Set<ConstraintViolation<ConsultaBuroRequest>> violaciones = validador.validate(consulta);

        assertThat(violaciones)
                .isNotEmpty()
                .allSatisfy(violacion -> assertThat(violacion.getPropertyPath())
                        .hasToString(CAMPO_NUMERO_DOCUMENTO))
                .extracting(ConstraintViolation::getMessage)
                .contains(mensajeEsperado);
    }

    @Test
    @DisplayName("Con los dos campos invalidos se devuelven ambos errores, no solo el primero")
    void validar_conAmbosCamposInvalidos_reportaLosDosCampos() {
        ConsultaBuroRequest consulta = new ConsultaBuroRequest(null, "abc");

        Set<ConstraintViolation<ConsultaBuroRequest>> violaciones = validador.validate(consulta);

        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains(CAMPO_TIPO_DOCUMENTO, CAMPO_NUMERO_DOCUMENTO);
    }
}
