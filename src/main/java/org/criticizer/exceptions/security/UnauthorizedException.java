package org.criticizer.exceptions.security;

import org.criticizer.exceptions.ApplicationException;

public class UnauthorizedException extends ApplicationException {
    public UnauthorizedException() {
        super("You are not authorized to perform this action",
                "Authorization check failed - user not authenticated");
    }

    public UnauthorizedException(String action) {
        super("You are not authorized to perform this action",
                "Authorization check failed for action: " + action);
    }
}
