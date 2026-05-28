package org.criticizer.exceptions.data;

import org.criticizer.exceptions.ApplicationException;

public class DatabaseException extends ApplicationException {
    public DatabaseException(String operation, Throwable cause) {
        super(
                "An error occurred while processing your request",
                "Database error during operation: " + operation,
                cause);
    }
}
