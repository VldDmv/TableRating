package org.criticizer.dto.genre;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTO for updating an existing genre. */
public record UpdateGenreRequest(
        @NotNull(message = "{genre.id.required}") Integer id,
        @NotBlank(message = "{genre.name.blank}")
                @Size(min = 1, max = 255, message = "{genre.name.size}")
                String name,
        @NotEmpty(message = "{genre.mediatypes.empty}") List<String> mediaTypes) {
    // Compact constructor for defensive copy
    public UpdateGenreRequest {
        mediaTypes = mediaTypes != null ? List.copyOf(mediaTypes) : List.of();
    }

    public Integer getId() {
        return id();
    }

    public String getName() {
        return name();
    }

    public List<String> getMediaTypes() {
        return mediaTypes();
    }

    public UpdateGenreRequest withId(Integer newId) {
        return new UpdateGenreRequest(newId, this.name, this.mediaTypes);
    }
}
