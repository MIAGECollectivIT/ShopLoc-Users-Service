package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request payload to associate an existing store with a partner")
public record AdminAddStoreRequest(
    @NotNull(message = "Store identifier is mandatory")
        @Schema(
            description = "Public UUID identifier of the store",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID storeUid) {}
