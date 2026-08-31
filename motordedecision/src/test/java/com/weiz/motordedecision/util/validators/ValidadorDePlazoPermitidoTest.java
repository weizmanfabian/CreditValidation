package com.weiz.motordedecision.util.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija el conjunto de plazos que el enunciado publica (linea 23): 12, 24, 36 y
 * 48, y ninguno mas.
 *
 * Los casos invalidos no son numeros al azar: 6 y 18 son plazos comerciales
 * habituales que este producto no ofrece, 60 esta por encima del mayor, y 0 y
 * -12 son los que delatarian un validador que solo mira el maximo.
 */
@DisplayName("Validador del plazo permitido")
class ValidadorDePlazoPermitidoTest {

    private final ValidadorDePlazoPermitido validador = new ValidadorDePlazoPermitido();

    @ParameterizedTest(name = "plazo {0} meses es valido")
    @ValueSource(ints = {12, 24, 36, 48})
    @DisplayName("Los cuatro plazos del enunciado se aceptan")
    void isValid_conPlazoDelEnunciado_devuelveVerdadero(int plazoMeses) {
        boolean esValido = validador.isValid(plazoMeses, null);

        assertThat(esValido).isTrue();
    }

    @ParameterizedTest(name = "plazo {0} meses se rechaza")
    @ValueSource(ints = {0, -12, 6, 11, 13, 18, 30, 47, 49, 60})
    @DisplayName("Cualquier plazo fuera del conjunto se rechaza")
    void isValid_conPlazoFueraDelConjunto_devuelveFalso(int plazoMeses) {
        boolean esValido = validador.isValid(plazoMeses, null);

        assertThat(esValido).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("El plazo ausente lo da por bueno: de exigirlo se ocupa @NotNull")
    void isValid_conPlazoNulo_devuelveVerdadero(Integer plazoMeses) {
        boolean esValido = validador.isValid(plazoMeses, null);

        assertThat(esValido).isTrue();
    }

    @Test
    @DisplayName("El conjunto permitido son exactamente cuatro plazos")
    void plazosPermitidos_siempre_contieneSoloLosCuatroDelEnunciado() {
        assertThat(ValidadorDePlazoPermitido.PLAZOS_PERMITIDOS)
                .containsExactlyInAnyOrder(12, 24, 36, 48);
    }

    @Test
    @DisplayName("El mensaje por omision nombra el campo en palabras y enumera los cuatro plazos")
    void message_porOmision_nombraElCampoYLosPlazos() throws NoSuchMethodException {
        String mensaje = (String) PlazoPermitido.class.getDeclaredMethod("message").getDefaultValue();

        assertThat(mensaje)
                .isEqualTo("Plazo en meses debe ser 12, 24, 36 o 48");
    }
}
