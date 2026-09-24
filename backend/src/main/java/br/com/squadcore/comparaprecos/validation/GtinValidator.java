package br.com.squadcore.comparaprecos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class GtinValidator implements ConstraintValidator<Gtin, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // O GTIN é opcional: ausência não é erro.
        return value == null || value.isBlank() || GtinUtils.isValido(value);
    }
}
