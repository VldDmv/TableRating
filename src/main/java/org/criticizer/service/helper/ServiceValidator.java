package org.criticizer.service.helper;

import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.exceptions.validation.InvalidScoreException;
import org.criticizer.exceptions.validation.WeakPasswordException;
import org.slf4j.Logger;

/**
 * Centralized validation logic for service layer.
 */
public class ServiceValidator {

    private final Logger log;

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_SEARCH_TERM_LENGTH = 100;

    public ServiceValidator(Logger log) {
        this.log = log;
    }

    /**
     * Validates that score is within acceptable range
     */
    public void validateScore(int score, int userId, String mediaType) {
        if (score < 1 || score > 100) {
            log.warn("Invalid score {} for {} for user {}", score, mediaType, userId);
            throw new InvalidScoreException(score, mediaType);
        }
    }

    /**
     * Validates that a username is not null, empty, or too long
     */
    public String validateUsername(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("Username validation failed: name is empty");
            throw new EmptyNameException("Username");
        }

        String trimmed = name.trim();

        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            log.warn("Username validation failed: too long ({} chars, max {})",
                    trimmed.length(), MAX_USERNAME_LENGTH);
            throw new InvalidInputException(
                    "username",
                    "must not exceed " + MAX_USERNAME_LENGTH + " characters"
            );
        }

        return trimmed;
    }

    /**
     * Validates that password meets length requirements.
     */
    public void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Password validation failed: password is too short or null");
            throw new WeakPasswordException(MIN_PASSWORD_LENGTH);
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            log.warn("Password validation failed: password is too long ({} chars, max {})",
                    password.length(), MAX_PASSWORD_LENGTH);
            throw new InvalidInputException(
                    "password",
                    "must not exceed " + MAX_PASSWORD_LENGTH + " characters"
            );
        }
    }

    /**
     * Validates pagination parameters and returns sanitized values.
     */
    public PaginationParams validatePagination(int page, int pageSize) {
        int sanitizedPage = Math.max(page, 1);
        int sanitizedPageSize = Math.max(Math.min(pageSize, 100), 1);
        int offset = (sanitizedPage - 1) * sanitizedPageSize;

        return new PaginationParams(sanitizedPage, sanitizedPageSize, offset);
    }

    /**
     Trimming whitespace and limiting length.
     */
    public String sanitizeSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return null;
        }

        String trimmed = searchTerm.trim();

        if (trimmed.length() > MAX_SEARCH_TERM_LENGTH) {
            log.warn("Search term too long: {} chars (max {})",
                    trimmed.length(), MAX_SEARCH_TERM_LENGTH);
            throw new InvalidInputException(
                    "search term",
                    "must not exceed " + MAX_SEARCH_TERM_LENGTH + " characters"
            );
        }

        return trimmed;
    }

    /**
     * Validates that a name/title is not null, empty, or too long.
     */
    public String validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("{} validation failed: name is empty", fieldName);
            throw new EmptyNameException(fieldName);
        }

        String trimmed = name.trim();

        if (trimmed.length() > 255) {
            log.warn("{} validation failed: too long ({} chars)",
                    fieldName, trimmed.length());
            throw new InvalidInputException(
                    fieldName.toLowerCase(),
                    "must not exceed 255 characters"
            );
        }

        return trimmed;
    }

    /**
     * Record to hold validated pagination parameters.
     */
    public record PaginationParams(int page, int pageSize, int offset) {
    }
}