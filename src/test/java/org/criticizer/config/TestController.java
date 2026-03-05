package org.criticizer.config;

import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ItemInUseException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.exceptions.security.InsufficientPermissionsException;
import org.criticizer.exceptions.security.UnauthorizedException;
import org.criticizer.exceptions.validation.ValidationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/not-found")
    public void throwNotFound() {
        throw new ResourceNotFoundException("Game", "TestGame");
    }

    @GetMapping("/already-exists")
    public void throwAlreadyExists() {
        throw new ItemAlreadyExistsException("Game", "TestGame");
    }

    @GetMapping("/in-use")
    public void throwInUse() {
        throw new ItemInUseException("tag", "it is used by games");
    }

    @GetMapping("/validation-error")
    public void throwValidation() {
        throw new ValidationException("Invalid input", "Invalid");
    }

    @GetMapping("/unauthorized")
    public void throwUnauthorized() {
        throw new UnauthorizedException("Not authenticated");
    }

    @GetMapping("/forbidden")
    public void throwForbidden() {
        throw new InsufficientPermissionsException("ADMIN");
    }

    @GetMapping("/server-error")
    public void throwServerError() {
        throw new RuntimeException("Unexpected error");
    }

    @PostMapping("/validate")
    public void validateDto(
            @org.springframework.validation.annotation.Validated @RequestBody TestDto dto) {
    }

    public static class TestDto {
        @jakarta.validation.constraints.NotBlank(message = "Name is required")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}