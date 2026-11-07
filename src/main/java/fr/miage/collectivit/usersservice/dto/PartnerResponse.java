package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "Informations sur un commerçant partenaire")
public record PartnerResponse(
    @Schema(description = "Identifiant interne en base de données", example = "1") Long id,
    @Schema(
            description = "Identifiant public UUID du commerçant partenaire",
            example = "e2b027d1-9447-497d-bb62-5c421711fb85")
        UUID publicId,
    @Schema(description = "Nom complet du partenaire", example = "Jean Boulanger") String name,
    @Schema(description = "Adresse email de contact", example = "jean.boulanger@example.com")
        String email,
    @Schema(
            description = "Nom de la société ou enseigne commerciale",
            example = "Boulangerie du Centre")
        String companyName,
    @Schema(description = "Rôle de l'utilisateur", example = "MERCHANT") String role,
    @Schema(description = "Nombre de commerces actuellement rattachés", example = "2")
        int existingStoresCount) {}
