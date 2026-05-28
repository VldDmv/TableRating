package org.criticizer.exceptions.validation;

public class MissingParameterException extends ValidationException {
    public MissingParameterException(String parameterName) {
        super("Required information is missing", "Missing required parameter: " + parameterName);
    }
}
