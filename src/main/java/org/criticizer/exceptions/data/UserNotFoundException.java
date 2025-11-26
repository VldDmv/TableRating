package org.criticizer.exceptions.data;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String username) {
        super("User", username);
    }

    public UserNotFoundException(int userId) {
        super("User", "ID: " + userId);
    }
}
