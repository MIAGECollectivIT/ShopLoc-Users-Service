package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "Représentation détaillée d'un profil utilisateur ShopLoc")
public record UserResponse(
    @Schema(
            description = "Identifiant UUID public de l'utilisateur",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,
    @Schema(description = "Adresse email de l'utilisateur", example = "alice.dupont@example.com")
        String email,
    @Schema(description = "Rôle de l'utilisateur dans la plateforme", example = "CLIENT")
        UserRole role,
    @Schema(description = "Prénom", example = "Alice") String firstName,
    @Schema(description = "Nom de famille", example = "Dupont") String lastName,
    @Schema(description = "Date de naissance au format ISO", example = "1995-04-20")
        LocalDate birthDate,
    @Schema(
            description = "Code client unique à 7 caractères (présent pour les clients)",
            example = "CL12345")
        String clientCode,
    @Schema(
            description = "Lieu de retrait choisi pour la carte de fidélité physique",
            example = "Mairie de Lille")
        String pickupLocation,
    @ArraySchema(
            schema =
                @Schema(
                    description = "Plaque d'immatriculation enregistrée",
                    example = "AA-123-BB"))
        List<String> licensePlates,
    @Schema(description = "Horodatage de création du compte", example = "2026-11-01T10:15:30Z")
        Instant createdAt,
    @Schema(
            description = "Horodatage de dernière mise à jour du profil",
            example = "2026-11-07T12:00:00Z")
        Instant updatedAt) {
  public static UserResponse fromEntity(User user) {
    if (user == null) {
      return null;
    }
    String clientCode = null;
    String pickupLocation = null;
    List<String> licensePlates = null;

    if (user instanceof Client client) {
      clientCode = client.getClientCode();
      pickupLocation = client.getPickupLocation();
      licensePlates =
          client.getLicensePlates() != null ? List.copyOf(client.getLicensePlates()) : List.of();
    }

    return UserResponse.builder()
        .id(user.getPublicId())
        .email(user.getEmail())
        .role(user.getRole())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .birthDate(user.getBirthDate())
        .clientCode(clientCode)
        .pickupLocation(pickupLocation)
        .licensePlates(licensePlates)
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
