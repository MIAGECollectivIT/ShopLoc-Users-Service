package fr.miage.collectivit.usersservice.dto;

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
    return UserResponse.builder()
        .id(user.getPublicId())
        .email(user.getEmail())
        .role(user.getRole())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .birthDate(user.getBirthDate())
        .clientCode(user.getClientCode())
        .pickupLocation(user.getPickupLocation())
        .licensePlates(
            user.getLicensePlates() != null ? List.copyOf(user.getLicensePlates()) : List.of())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
