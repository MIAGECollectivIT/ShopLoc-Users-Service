package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

/**
 * Masked customer profile returned upon successful loyalty card identification (GDPR compliant).
 */
@Builder
@Schema(
    description =
        "Customer profile representation with masked PII for in-store cashier verification")
public record CustomerIdentificationResponse(
    @Schema(
            description = "Public UUID identifier of the customer account",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID publicId,
    @Schema(description = "Unique 7-character customer loyalty code", example = "k7P9xQ2")
        String clientCode,
    @Schema(description = "Customer first name", example = "Luc") String firstName,
    @Schema(
            description = "Masked customer last name for cashier greeting and validation",
            example = "D***t")
        String lastNameMasked,
    @Schema(
            description = "Masked customer email address for privacy",
            example = "l***t@example.com")
        String emailMasked,
    @Schema(
            description =
                "Confirmation flag indicating the customer account is valid and recognized",
            example = "true")
        boolean recognized,
    @Schema(description = "Year the customer joined the loyalty program", example = "2026")
        Integer memberSinceYear) {}
