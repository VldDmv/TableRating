package org.criticizer.dto.genre;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTO for creating a new genre. */
public record CreateGenreRequest(
        @NotBlank(message = "{genre.name.blank}")
                @Size(min = 1, max = 255, message = "{genre.name.size}")
                String name,
        @NotEmpty(message = "{genre.mediatypes.empty}") List<String> mediaTypes) {
    // Compact constructor for defensive copy
    public CreateGenreRequest {
        mediaTypes = mediaTypes != null ? List.copyOf(mediaTypes) : List.of();
    }

    public String getName() {
        return name();
    }

    public List<String> getMediaTypes() {
        return mediaTypes();
    }
}
