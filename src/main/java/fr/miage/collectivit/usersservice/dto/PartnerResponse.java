package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "Partner merchant information")
public record PartnerResponse(
    @Schema(description = "Internal database identifier", example = "1") Long id,
    @Schema(
            description = "Public UUID identifier of the partner merchant",
            example = "e2b027d1-9447-497d-bb62-5c421711fb85")
        UUID publicId,
    @Schema(description = "Full partner name", example = "Jean Boulanger") String name,
    @Schema(description = "Contact email address", example = "jean.boulanger@example.com")
        String email,
    @Schema(
            description = "Company name or store commercial brand",
            example = "Boulangerie du Centre")
        String companyName,
    @Schema(description = "User role", example = "MERCHANT") String role,
    @Schema(description = "Number of currently associated stores", example = "2")
        int existingStoresCount) {}
