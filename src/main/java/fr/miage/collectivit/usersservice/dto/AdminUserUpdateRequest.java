package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Administrative user modification payload")
public record AdminUserUpdateRequest(
    @NotBlank(message = "First name is mandatory")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        @Schema(description = "User first name", example = "Alice")
        String firstName,
    @NotBlank(message = "Last name is mandatory")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        @Schema(description = "User last name", example = "Dupont")
        String lastName,
    @NotBlank(message = "Email is mandatory")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        @Schema(description = "User email address", example = "alice.dupont@example.com")
        String email,
    @Schema(description = "User role", example = "CLIENT") UserRole role) {}
