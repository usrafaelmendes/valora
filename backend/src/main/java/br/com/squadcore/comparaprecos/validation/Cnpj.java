package br.com.squadcore.comparaprecos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida o CNPJ (com ou sem máscara) pelo formato e pelos dígitos verificadores. Nulo é aceito. */
@Documented
@Constraint(validatedBy = CnpjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cnpj {

    String message() default "O CNPJ informado é inválido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
