package org.criticizer.exceptions.security;

import org.criticizer.exceptions.ApplicationException;

public class InvalidCredentialsException extends ApplicationException {
    public InvalidCredentialsException() {
        super("Invalid username or password",
                "Authentication failed - credentials do not match");
    }

    public InvalidCredentialsException(String username) {
        super("Invalid username or password",
                "Authentication failed for user: " + username);
    }
}
