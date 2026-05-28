package org.criticizer.dto.book;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.List;
import org.criticizer.controller.helper.CreateMediaRequest;

public record CreateBookRequest(
        @NotBlank(message = "{media.name.blank}")
                @Size(min = 1, max = 255, message = "{media.name.size}")
                String name,
        @Size(max = 500, message = "Cover URL too long") String coverUrl,
        @NotNull(message = "{media.score.required}")
                @Min(value = 1, message = "{media.score.min}")
                @Max(value = 100, message = "{media.score.max}")
                Integer score,
        @JsonProperty("genreIds") List<Integer> categoryIds)
        implements CreateMediaRequest {}
