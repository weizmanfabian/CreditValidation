package com.weiz.motordedecision.util.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restringe un plazo en meses al conjunto que publica el enunciado: 12, 24, 36
 * o 48.
 *
 * Existe como anotacion propia, y no como un {@code @Pattern} o una lista
 * suelta repetida en cada DTO, porque el conjunto es uno solo: quien lo
 * conozca es {@link ValidadorDePlazoPermitido} y nadie mas.
 *
 * Un valor nulo lo da por bueno: si el plazo es obligatorio lo dice
 * {@code @NotNull}, y duplicar esa regla aqui produciria dos errores para un
 * unico campo vacio.
 */
@Documented
@Constraint(validatedBy = ValidadorDePlazoPermitido.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlazoPermitido {

    /**
     * Mensaje en espanol que nombra el dato en palabras y enumera lo que se
     * espera de el ({@code docs/error-handling.md} §5).
     */
    String message() default "Plazo en meses debe ser 12, 24, 36 o 48";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
