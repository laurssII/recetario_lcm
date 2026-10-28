package es.uvigo.dagss.recetas.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * The date must be between 1900-01-01 and today (both included).
 * {@code null} is considered valid: whether the field is mandatory is decided
 * by {@code @NotNull}.
 */
@Documented
@Constraint(validatedBy = BirthDate.Check.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BirthDate {

    LocalDate EARLIEST = LocalDate.of(1900, 1, 1);

    String message() default ValidationRules.BIRTH_DATE_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Check implements ConstraintValidator<BirthDate, LocalDate> {
        @Override
        public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
            return value == null || (!value.isBefore(EARLIEST) && !value.isAfter(LocalDate.now()));
        }
    }
}
