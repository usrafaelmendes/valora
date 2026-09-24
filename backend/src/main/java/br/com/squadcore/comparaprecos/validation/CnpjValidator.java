package br.com.squadcore.comparaprecos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Obrigatoriedade é responsabilidade do @NotBlank.
        return value == null || value.isBlank() || CnpjUtils.isValido(value);
    }
}
