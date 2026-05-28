package org.criticizer.exceptions;

/**
 * Basic application exception. Contains two messages: - userMessage: a secure message for the user
 * - technicalMessage: a detailed message for logs
 */
public class ApplicationException extends RuntimeException {
    private final String userMessage;

    public ApplicationException(String userMessage, String technicalMessage) {
        super(technicalMessage);
        this.userMessage = userMessage;
    }

    public ApplicationException(String userMessage, String technicalMessage, Throwable cause) {
        super(technicalMessage, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
