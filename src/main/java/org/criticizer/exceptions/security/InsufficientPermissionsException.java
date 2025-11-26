package org.criticizer.exceptions.security;

import org.criticizer.exceptions.ApplicationException;

public class InsufficientPermissionsException extends ApplicationException {
    public InsufficientPermissionsException() {
        super("You don't have permission to perform this action",
                "Permission check failed - insufficient privileges");
    }

    public InsufficientPermissionsException(String requiredRole) {
        super("You don't have permission to perform this action",
                "Permission check failed - requires role: " + requiredRole);
    }
}
