package org.criticizer.exceptions.security;

import org.criticizer.exceptions.ApplicationException;

public class OperationNotPermittedException extends ApplicationException {
    public OperationNotPermittedException(String operation) {
        super("This operation is not allowed",
                "Operation not permitted: " + operation);
    }

    public OperationNotPermittedException(String operation, String reason) {
        super("This operation is not allowed: " + reason,
                "Operation not permitted: " + operation + " - " + reason);
    }
}
