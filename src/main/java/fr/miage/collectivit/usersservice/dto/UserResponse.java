package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UserResponse(
    UUID id,
    String email,
    UserRole role,
    String firstName,
    String lastName,
    LocalDate birthDate,
    String clientCode,
    String pickupLocation,
    List<String> licensePlates,
    Instant createdAt,
    Instant updatedAt) {
  public static UserResponse fromEntity(User user) {
    String clientCode = null;
    String pickupLocation = null;
    List<String> licensePlates = null;

    if (user instanceof Client client) {
      clientCode = client.getClientCode();
      pickupLocation = client.getPickupLocation();
      licensePlates =
          client.getLicensePlates() != null ? List.copyOf(client.getLicensePlates()) : List.of();
    } else if (user != null && user.getClientCode() != null) {
      clientCode = user.getClientCode();
      pickupLocation = user.getPickupLocation();
      licensePlates =
          user.getLicensePlates() != null ? List.copyOf(user.getLicensePlates()) : List.of();
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
        .licensePlates(licensePlates != null ? licensePlates : List.of())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
