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
 * The text must be the name of one of the 52 Spanish provinces (see
 * {@link SpanishProvinces}; case, accents and co-official spellings are
 * accepted). {@code null} and blank values are considered valid: whether the
 * field is mandatory is decided by {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = SpanishProvince.Check.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpanishProvince {

    String message() default ValidationRules.PROVINCE_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Check implements ConstraintValidator<SpanishProvince, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.isBlank() || SpanishProvinces.find(value).isPresent();
        }
    }
}
