package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.ToString;

@Builder
@Schema(description = "User authentication request")
public record LoginRequest(
    @Schema(
            description = "User account email address",
            example = "alice.dupont@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is mandatory")
        @Email(message = "Invalid email address format")
        String email,
    @Schema(
            description = "Account cleartext password",
            example = "SecurePass123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is mandatory")
        @ToString.Exclude
        String password) {}
