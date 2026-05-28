package org.criticizer.exceptions.data;

import org.criticizer.exceptions.ApplicationException;

public class UserAlreadyExistsException extends ApplicationException {
    public UserAlreadyExistsException(String username) {
        super(
                "Username already exists. Please choose another one.",
                "Registration failed - username '" + username + "' already exists in database");
    }
}
