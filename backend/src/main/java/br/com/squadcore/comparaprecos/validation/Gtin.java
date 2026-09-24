package br.com.squadcore.comparaprecos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida o GTIN/EAN pelo formato e pelo dígito verificador. Nulo ou em branco é aceito. */
@Documented
@Constraint(validatedBy = GtinValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Gtin {

    String message() default "O GTIN/EAN informado é inválido (use 8, 12, 13 ou 14 dígitos).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
