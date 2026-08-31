package com.weiz.motordedecision.util.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

/**
 * Comprueba que el plazo recibido este en el conjunto de {@link PlazoPermitido}.
 *
 * El conjunto vive aqui, en el codigo, y no en {@code application.yml} a pesar
 * de la regla de {@code docs/conventions.md} §9: no es un umbral que el area de
 * riesgo ajuste, es el catalogo publicado del formulario —la misma naturaleza
 * que {@code TipoDocumento}— y lo repiten la restriccion
 * {@code ck_solicitud_plazo} del esquema y el formulario del frontend (D-050).
 *
 * Un plazo nulo se da por valido: quien exige su presencia es {@code @NotNull}.
 */
public class ValidadorDePlazoPermitido implements ConstraintValidator<PlazoPermitido, Integer> {

    /** Los cuatro plazos del enunciado (linea 23). */
    public static final Set<Integer> PLAZOS_PERMITIDOS = Set.of(12, 24, 36, 48);

    @Override
    public boolean isValid(Integer plazoMeses, ConstraintValidatorContext contexto) {
        return plazoMeses == null || PLAZOS_PERMITIDOS.contains(plazoMeses);
    }
}
