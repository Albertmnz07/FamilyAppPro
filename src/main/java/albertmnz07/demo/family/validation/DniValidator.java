package albertmnz07.demo.family.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;

public class DniValidator implements ConstraintValidator<ValidDni, String> {

    private static final String CHECK_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("\\d{8}[A-Z]")) {
            return false;
        }
        int number = Integer.parseInt(normalized.substring(0, 8));
        return normalized.charAt(8) == CHECK_LETTERS.charAt(number % 23);
    }
}
