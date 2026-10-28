package es.uvigo.dagss.recetas.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import es.uvigo.dagss.recetas.dto.AddressDTO;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Class-level constraint for {@link AddressDTO}: the first two digits of the
 * postal code must be the code of the address's province (36xxx -> Pontevedra).
 * It is only checked when both the postal code and the province are
 * individually valid; otherwise those field constraints already report the
 * problem. The violation is attached to the {@code postalCode} property.
 */
@Documented
@Constraint(validatedBy = ConsistentAddress.Check.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsistentAddress {

    String message() default "El código postal no corresponde a la provincia indicada";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Check implements ConstraintValidator<ConsistentAddress, AddressDTO> {
        @Override
        public boolean isValid(AddressDTO address, ConstraintValidatorContext context) {
            if (address == null || address.postalCode() == null || address.province() == null) {
                return true;
            }
            String postalCode = address.postalCode().trim();
            Optional<SpanishProvinces.Province> province = SpanishProvinces.find(address.province());
            if (province.isEmpty() || !postalCode.matches("\\d{5}")) {
                return true;
            }
            if (postalCode.startsWith(province.get().code())) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("El código postal " + postalCode
                            + " no corresponde a la provincia de " + province.get().name()
                            + " (debería empezar por " + province.get().code() + ")")
                    .addPropertyNode("postalCode")
                    .addConstraintViolation();
            return false;
        }
    }
}
