package org.criticizer.dto.admin;

import jakarta.validation.constraints.NotNull;
import org.criticizer.entity.Role;

/** Request DTO for changing user role */
public record ChangeRoleRequest(@NotNull(message = "Role is required") Role role) {}
