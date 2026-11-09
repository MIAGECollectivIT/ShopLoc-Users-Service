package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request payload to link a partner merchant to a store")
public record UserStoreLinkRequest(
    @Schema(
            description = "Internal database identifier of the merchant partner",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Partner user identifier is mandatory")
        @Positive(message = "Partner user identifier must be positive")
        Long userId,
    @Schema(
            description = "Internal identifier of the store to link",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Store identifier is mandatory")
        @Positive(message = "Store identifier must be positive")
        Long storeId) {}
