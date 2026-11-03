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
import fr.miage.collectivit.usersservice.entity.Admin;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.Merchant;
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
  private Client existingUser;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    existingUser =
        Client.builder()
            .id(1L)
            .publicId(userId)
            .email("user@example.com")
            .passwordHash("$2a$12$fakeHash")
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
    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.firstName()).isEqualTo("Sophie");
    assertThat(response.pickupLocation()).isEqualTo("Commerce Partenaire A");
    assertThat(response.licensePlates()).containsExactly("XX-123-YY");
  }

  @Test
  void shouldThrowWhenGetUserProfileNotFound() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userProfileService.getUserProfile(userId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining(userId.toString());
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
  void shouldGetVehicles() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));

    List<VehicleResponse> vehicles = userProfileService.getVehicles(userId);

    assertThat(vehicles).hasSize(1);
    assertThat(vehicles.getFirst().licensePlate()).isEqualTo("XX-123-YY");
  }

  @Test
  void shouldAddVehicleSuccessfully() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(existingUser);

    VehicleRequest request = new VehicleRequest("aa-222-bb");

    VehicleResponse response = userProfileService.addVehicle(userId, request);

    assertThat(response.licensePlate()).isEqualTo("AA-222-BB");
    assertThat(existingUser.getLicensePlates()).contains("AA-222-BB");
    verify(userRepository).save(existingUser);
  }

  @Test
  void shouldThrowWhenAddingDuplicateVehicle() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));

    VehicleRequest request = new VehicleRequest("xx-123-yy");

    assertThatThrownBy(() -> userProfileService.addVehicle(userId, request))
        .isInstanceOf(VehicleAlreadyExistsException.class)
        .hasMessageContaining("XX-123-YY");
  }

  @Test
  void shouldRemoveVehicleSuccessfully() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(existingUser);

    userProfileService.removeVehicle(userId, "XX-123-YY");

    assertThat(existingUser.getLicensePlates()).doesNotContain("XX-123-YY");
    verify(userRepository).save(existingUser);
  }

  @Test
  void shouldThrowWhenRemovingNonExistentVehicle() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));

    assertThatThrownBy(() -> userProfileService.removeVehicle(userId, "ZZ-999-ZZ"))
        .isInstanceOf(VehicleNotFoundException.class)
        .hasMessageContaining("ZZ-999-ZZ");
  }

  @Test
  void shouldGetCurrentUserProfileByUuid() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(existingUser));

    UserResponse response = userProfileService.getCurrentUserProfile(userId.toString(), null);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("user@example.com");
  }

  @Test
  void shouldGetCurrentUserProfileByNumericId() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

    UserResponse response = userProfileService.getCurrentUserProfile("1", null);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("user@example.com");
  }

  @Test
  void shouldGetCurrentUserProfileByEmail() {
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));

    UserResponse response = userProfileService.getCurrentUserProfile(null, "user@example.com");

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("user@example.com");
  }

  @Test
  void shouldGetCurrentUserProfileForMerchantWithoutClientFields() {
    UUID merchantId = UUID.randomUUID();
    Merchant merchant =
        Merchant.builder()
            .id(2L)
            .publicId(merchantId)
            .email("merchant@example.com")
            .firstName("Jean")
            .lastName("Commercant")
            .birthDate(LocalDate.of(1985, 5, 10))
            .build();

    when(userRepository.findByPublicId(merchantId)).thenReturn(Optional.of(merchant));

    UserResponse response = userProfileService.getCurrentUserProfile(merchantId.toString(), null);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(merchantId);
    assertThat(response.email()).isEqualTo("merchant@example.com");
    assertThat(response.role()).isEqualTo(UserRole.MERCHANT);
    assertThat(response.firstName()).isEqualTo("Jean");
    assertThat(response.lastName()).isEqualTo("Commercant");
    assertThat(response.clientCode()).isNull();
    assertThat(response.pickupLocation()).isNull();
    assertThat(response.licensePlates()).isNull();
  }

  @Test
  void shouldGetCurrentUserProfileForAdminWithoutClientFields() {
    UUID adminId = UUID.randomUUID();
    Admin admin =
        Admin.builder()
            .id(3L)
            .publicId(adminId)
            .email("admin@example.com")
            .firstName("Super")
            .lastName("Admin")
            .birthDate(LocalDate.of(1980, 1, 1))
            .build();

    when(userRepository.findByPublicId(adminId)).thenReturn(Optional.of(admin));

    UserResponse response = userProfileService.getCurrentUserProfile(adminId.toString(), null);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(adminId);
    assertThat(response.email()).isEqualTo("admin@example.com");
    assertThat(response.role()).isEqualTo(UserRole.ADMIN);
    assertThat(response.firstName()).isEqualTo("Super");
    assertThat(response.lastName()).isEqualTo("Admin");
    assertThat(response.clientCode()).isNull();
    assertThat(response.pickupLocation()).isNull();
    assertThat(response.licensePlates()).isNull();
  }

  @Test
  void shouldThrowUnauthorizedWhenIdentityHeadersMissing() {
    assertThatThrownBy(() -> userProfileService.getCurrentUserProfile(null, null))
        .isInstanceOf(fr.miage.collectivit.usersservice.exception.UnauthorizedException.class);

    assertThatThrownBy(() -> userProfileService.getCurrentUserProfile("  ", ""))
        .isInstanceOf(fr.miage.collectivit.usersservice.exception.UnauthorizedException.class);
  }

  @Test
  void shouldThrowUserNotFoundWhenIdentityNotMatching() {
    when(userRepository.findByPublicId(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userProfileService.getCurrentUserProfile(userId.toString(), null))
        .isInstanceOf(UserNotFoundException.class);
  }
}
