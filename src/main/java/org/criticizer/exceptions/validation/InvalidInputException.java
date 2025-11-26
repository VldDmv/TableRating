package org.criticizer.exceptions.validation;

public class InvalidInputException extends ValidationException {
    public InvalidInputException(String fieldName) {
        super("Invalid " + fieldName,
                "Validation failed for field: " + fieldName);
    }

    public InvalidInputException(String fieldName, String reason) {
        super("Invalid " + fieldName + ": " + reason,
                "Validation failed for field '" + fieldName + "' - " + reason);
    }
}
