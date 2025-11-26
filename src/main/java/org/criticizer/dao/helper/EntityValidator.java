package org.criticizer.dao.helper;

import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.exceptions.validation.InvalidScoreException;
import org.slf4j.Logger;

/**
 * Centralized validation logic for entities.
 */
public class EntityValidator {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_TEXT_LENGTH = 1000;
    private final Logger log;

    public EntityValidator(Logger log) {
        this.log = log;
    }

    /**
     * Validates and trims a name, ensuring it is not null, empty, or too long.
     */
    public String validateAndTrimName(String name, String entityType, String operationContext) {
        if (name == null) {
            log.warn("{} failed: name is null", operationContext);
            throw new EmptyNameException(entityType);
        }

        final String trimmedName = name.trim();

        if (trimmedName.isEmpty()) {
            log.warn("{} failed: name is empty after trimming", operationContext);
            throw new EmptyNameException(entityType);
        }

        if (trimmedName.length() > MAX_NAME_LENGTH) {
            log.warn("{} failed: name too long ({} chars, max {})",
                    operationContext, trimmedName.length(), MAX_NAME_LENGTH);
            throw new InvalidInputException(
                    entityType.toLowerCase() + " name",
                    "must not exceed " + MAX_NAME_LENGTH + " characters (got " + trimmedName.length() + ")"
            );
        }

        return trimmedName;
    }

    /**
     * Validates that a score is within acceptable range.
     */
    public void validateScore(int score, int userId, String entityType) {
        if (score < 1 || score > 100) {
            log.warn("Invalid score {} for {} for user {}", score, entityType, userId);
            throw new InvalidScoreException(score, entityType);
        }
    }
}