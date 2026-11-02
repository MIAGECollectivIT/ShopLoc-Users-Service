package fr.miage.collectivit.usersservice.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record PartnerResponse(
    Long id,
    UUID publicId,
    String name,
    String email,
    String companyName,
    String role,
    int existingStoresCount) {}
