package es.uvigo.dagss.recetas.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * The text must be a Spanish DNI, NIE (both with a correct control letter) or
 * a CIF (format only). {@code null} and blank values are considered valid:
 * whether the field is mandatory is decided by {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = TaxId.Check.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TaxId {

    String message() default ValidationRules.TAX_ID_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Check implements ConstraintValidator<TaxId, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.isBlank() || SpanishIds.isValidTaxId(value);
        }
    }
}
