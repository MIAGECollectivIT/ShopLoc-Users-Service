package fr.miage.collectivit.usersservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.dto.UserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.VehicleRequest;
import fr.miage.collectivit.usersservice.dto.VehicleResponse;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.exception.VehicleAlreadyExistsException;
import fr.miage.collectivit.usersservice.exception.VehicleNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.service.impl.UserProfileServiceImpl;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserProfileServiceImpl userProfileService;

  private UUID userId;
  private User existingUser;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    existingUser =
        User.builder()
            .id(1L)
            .publicId(userId)
            .email("user@example.com")
            .passwordHash("$2a$12$fakeHash")
            .role(UserRole.CLIENT)
            .firstName("Sophie")
            .lastName("Martin")
            .birthDate(LocalDate.of(1988, 4, 12))
            .clientCode("sM88kL9")
            .pickupLocation("Commerce Partenaire A")
            .licensePlates(new ArrayList<>(List.of("XX-123-YY")))
            .build();
  }

  @Test
  void shouldGetUserProfile() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));

    UserResponse response = userProfileService.getUserProfile(userId);

    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.firstName()).isEqualTo("Sophie");
    assertThat(response.licensePlates()).containsExactly("XX-123-YY");
  }

  @Test
  void shouldThrowWhenUserNotFoundOnGet() {
    UUID unknownId = UUID.randomUUID();
    when(userRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userProfileService.getUserProfile(unknownId))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void shouldUpdateUserProfile() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserUpdateRequest updateRequest =
        UserUpdateRequest.builder()
            .firstName("Sophie-Anne")
            .lastName("Martin-Dupont")
            .birthDate(LocalDate.of(1988, 4, 12))
            .pickupLocation("Mairie Annexe")
            .build();

    UserResponse response = userProfileService.updateUserProfile(userId, updateRequest);

    assertThat(response.firstName()).isEqualTo("Sophie-Anne");
    assertThat(response.lastName()).isEqualTo("Martin-Dupont");
    assertThat(response.pickupLocation()).isEqualTo("Mairie Annexe");
    verify(userRepository).save(existingUser);
  }

  @Test
  void shouldDeleteUserProfile() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));

    userProfileService.deleteUserProfile(userId);

    verify(userRepository).delete(existingUser);
  }

  @Test
  void shouldGetVehiclesForUser() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));

    List<VehicleResponse> vehicles = userProfileService.getVehicles(userId);

    assertThat(vehicles).hasSize(1);
    assertThat(vehicles.get(0).licensePlate()).isEqualTo("XX-123-YY");
  }

  @Test
  void shouldAddVehicleToUser() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(existingUser);

    VehicleResponse response =
        userProfileService.addVehicle(userId, new VehicleRequest("ZZ-999-ZZ"));

    assertThat(response.licensePlate()).isEqualTo("ZZ-999-ZZ");
    assertThat(existingUser.getLicensePlates()).contains("XX-123-YY", "ZZ-999-ZZ");
    verify(userRepository).save(existingUser);
  }

  @Test
  void shouldThrowWhenVehicleAlreadyExists() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));

    assertThatThrownBy(() -> userProfileService.addVehicle(userId, new VehicleRequest("XX-123-YY")))
        .isInstanceOf(VehicleAlreadyExistsException.class);
  }

  @Test
  void shouldRemoveVehicleFromUser() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(existingUser);

    userProfileService.removeVehicle(userId, "XX-123-YY");

    assertThat(existingUser.getLicensePlates()).isEmpty();
    verify(userRepository).save(existingUser);
  }

  @Test
  void shouldThrowWhenRemovingNonExistentVehicle() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));

    assertThatThrownBy(() -> userProfileService.removeVehicle(userId, "UNKNOWN-PLATE"))
        .isInstanceOf(VehicleNotFoundException.class);
  }
}
