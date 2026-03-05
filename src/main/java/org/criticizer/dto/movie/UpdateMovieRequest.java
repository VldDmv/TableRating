package org.criticizer.dto.movie;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import org.criticizer.controller.helper.UpdateMediaRequest;

import java.util.List;

public record UpdateMovieRequest(
        @NotBlank(message = "{media.name.blank}")
        @Size(min = 1, max = 255, message = "{media.name.size}")
        String name,
        @Size(max = 500, message = "Cover URL too long")
        String coverUrl,
        @NotNull(message = "{media.score.required}")
        @Min(value = 1, message = "{media.score.min}")
        @Max(value = 100, message = "{media.score.max}")
        Integer score,

        @JsonProperty("genreIds")
        List<Integer> categoryIds
) implements UpdateMediaRequest {
}