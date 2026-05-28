package org.criticizer.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO for updating an existing tag. */
public record UpdateTagRequest(
        @NotNull(message = "{tag.id.required}") Integer id,
        @NotBlank(message = "{tag.name.blank}")
                @Size(min = 1, max = 255, message = "{tag.name.size}")
                String name) {

    public Integer getId() {
        return id();
    }

    public String getName() {
        return name();
    }

    public UpdateTagRequest withId(Integer newId) {
        return new UpdateTagRequest(newId, this.name);
    }
}
