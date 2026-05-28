package org.criticizer.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO for creating a new tag. */
public record CreateTagRequest(
        @NotBlank(message = "{tag.name.blank}")
                @Size(min = 1, max = 255, message = "{tag.name.size}")
                String name) {
    public String getName() {
        return name();
    }
}
