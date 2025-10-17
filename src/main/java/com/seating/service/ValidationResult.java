package com.seating.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a validation operation
 */
public class ValidationResult {
    private final boolean isValid;
    private final List<String> errors;

    public ValidationResult(boolean isValid, List<String> errors) {
        this.isValid = isValid;
        this.errors = errors;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, new ArrayList<>());
    }

    public boolean isValid() {
        return isValid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
