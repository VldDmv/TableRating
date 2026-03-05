package org.criticizer.dto.helper;

/**
 * Response for privacy setting update.
 * Contains confirmation message and new privacy state.
 */
public record PrivacyUpdateResponse(String message, boolean isPublic) {
}