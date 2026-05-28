package org.criticizer.exceptions.validation;

public class InvalidScoreException extends ValidationException {
    public InvalidScoreException(int score) {
        super(
                "Rating must be between 1 and 100",
                "Invalid score value: " + score + " (must be 1-100)");
    }

    public InvalidScoreException(int score, String mediaType) {
        super(
                "Rating must be between 1 and 100",
                "Invalid score " + score + " for " + mediaType + " (must be 1-100)");
    }
}
