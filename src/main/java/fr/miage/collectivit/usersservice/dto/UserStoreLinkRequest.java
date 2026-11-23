package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request payload to link a partner merchant to a store")
public record UserStoreLinkRequest(
    @Schema(
            description = "Public UUID identifier of the merchant partner",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Partner user identifier is mandatory")
        UUID userId,
    @Schema(
            description = "Public UUID identifier of the store to link",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Store identifier is mandatory")
        UUID storeUid) {}
