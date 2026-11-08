package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload to associate an existing store with a partner")
public record AdminAddStoreRequest(
    @NotNull(message = "Store identifier is mandatory")
        @Schema(description = "Store identifier", example = "42")
        Long storeId) {}
