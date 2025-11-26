package org.criticizer.exceptions.validation;


public class EmptyNameException extends ValidationException {
    public EmptyNameException(String fieldName) {
        super(fieldName + " cannot be empty",
                "Validation failed: " + fieldName + " is null or empty");
    }
}