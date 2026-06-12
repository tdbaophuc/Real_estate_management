package com.javaweb.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        boolean hasLowercase = false;
        boolean hasUppercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isLowerCase(current)) {
                hasLowercase = true;
            } else if (Character.isUpperCase(current)) {
                hasUppercase = true;
            } else if (Character.isDigit(current)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(current)) {
                hasSpecial = true;
            }
        }
        return hasLowercase && hasUppercase && hasDigit && hasSpecial;
    }
}
