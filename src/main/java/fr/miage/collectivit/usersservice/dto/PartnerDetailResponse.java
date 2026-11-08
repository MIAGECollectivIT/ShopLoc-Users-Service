package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "Detailed administrative representation of a partner merchant")
public record PartnerDetailResponse(
    @Schema(description = "Internal database identifier", example = "1") Long id,
    @Schema(
            description = "Public UUID identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID publicId,
    @Schema(description = "Email address", example = "partner@example.com") String email,
    @Schema(description = "User role", example = "MERCHANT") UserRole role,
    @Schema(description = "First name", example = "Jean") String firstName,
    @Schema(description = "Last name", example = "Dupont") String lastName,
    @Schema(description = "Birth date", example = "1985-05-15") LocalDate birthDate,
    @Schema(description = "Company or merchant store label", example = "Boulangerie Dupont")
        String companyName,
    @Schema(description = "List of associated store identifiers", example = "[1, 2]")
        List<Long> storeIds,
    @Schema(description = "Creation timestamp", example = "2026-11-01T10:00:00Z") Instant createdAt,
    @Schema(description = "Last update timestamp", example = "2026-11-02T12:00:00Z")
        Instant updatedAt) {}
