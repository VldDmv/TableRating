
package org.criticizer.exceptions.validation;


public class WeakPasswordException extends ValidationException {
    public WeakPasswordException() {
        super("Password must be at least 6 characters long",
                "Password validation failed: length < 6");
    }

    public WeakPasswordException(int minLength) {
        super("Password must be at least " + minLength + " characters long",
                "Password validation failed: length < " + minLength);
    }
}