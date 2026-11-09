package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Successful authentication response with JWT token")
public record AuthResponse(
    @Schema(
            description = "Signed JWT access token",
            example =
                "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFsaWNlIER1cG9udCJ9...")
        String token,
    @Schema(description = "Authorization token type", example = "Bearer") String tokenType,
    @Schema(description = "Token validity duration in milliseconds", example = "7200000")
        long expiresIn,
    @Schema(description = "Authenticated user profile") UserResponse user) {}
