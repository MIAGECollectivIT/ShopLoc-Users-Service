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
@Schema(description = "Detailed representation of a ShopLoc user profile")
public record UserResponse(
    @Schema(
            description = "Public UUID identifier of the user",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,
    @Schema(description = "User email address", example = "alice.dupont@example.com") String email,
    @Schema(description = "User role on the platform", example = "CLIENT") UserRole role,
    @Schema(description = "First name", example = "Alice") String firstName,
    @Schema(description = "Last name", example = "Dupont") String lastName,
    @Schema(description = "ISO-formatted birth date", example = "1995-04-20") LocalDate birthDate,
    @Schema(
            description = "Unique 7-character client code (present for clients)",
            example = "CL12345")
        String clientCode,
    @Schema(
            description = "Pickup location selected for the physical loyalty card",
            example = "Mairie de Lille")
        String pickupLocation,
    @ArraySchema(
            schema =
                @Schema(description = "Registered vehicle license plate", example = "AA-123-BB"))
        List<String> licensePlates,
    @Schema(description = "Account creation timestamp", example = "2026-11-01T10:15:30Z")
        Instant createdAt,
    @Schema(description = "Profile last update timestamp", example = "2026-11-07T12:00:00Z")
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
