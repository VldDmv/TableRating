package org.criticizer.exceptions.validation;

import org.criticizer.exceptions.ApplicationException;

public class PasswordMismatchException extends ApplicationException {
    public PasswordMismatchException() {
        super(
                "Passwords do not match",
                "Registration failed - password and confirmation do not match");
    }
}
