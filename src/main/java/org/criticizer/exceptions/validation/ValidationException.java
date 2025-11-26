package org.criticizer.exceptions.validation;

import org.criticizer.exceptions.ApplicationException;

public class ValidationException extends ApplicationException {
    public ValidationException(String userMessage, String technicalMessage) {
        super(userMessage, technicalMessage);
    }
}
