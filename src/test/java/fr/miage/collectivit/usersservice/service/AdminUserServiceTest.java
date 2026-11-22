package fr.miage.collectivit.usersservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.dto.AdminUserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PageResponse;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.Merchant;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.service.impl.AdminUserServiceImpl;
import fr.miage.collectivit.usersservice.util.ClientCodeGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private EntityManager entityManager;
  @Mock private ClientCodeGenerator clientCodeGenerator;
  @Mock private Query queryMock;

  @InjectMocks private AdminUserServiceImpl adminUserService;

  private Client sampleUser;
  private UUID publicId;

  @BeforeEach
  void setUp() {
    publicId = UUID.randomUUID();
    sampleUser =
        Client.builder()
            .id(1L)
            .publicId(publicId)
            .email("alice@example.com")
            .firstName("Alice")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1995, 4, 20))
            .clientCode("CL12345")
            .role(UserRole.CLIENT)
            .failedLoginAttempts(0)
            .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
            .updatedAt(Instant.now())
            .build();
  }

  @Test
  void shouldReturnPaginatedUsersWithFilters() {
    Page<User> page = new PageImpl<>(List.of(sampleUser), PageRequest.of(0, 20), 1);
    when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    PageResponse<UserResponse> response =
        adminUserService.getUsers(
            "alice", UserRole.CLIENT, false, null, null, PageRequest.of(0, 20));

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().getFirst().email()).isEqualTo("alice@example.com");
    assertThat(response.totalElements()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);
  }

  @Test
  void shouldGetUserByIdSuccessfully() {
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleUser));

    UserResponse response = adminUserService.getUserById(publicId.toString());

    assertThat(response.id()).isEqualTo(publicId);
    assertThat(response.email()).isEqualTo("alice@example.com");
  }

  @Test
  void shouldThrowWhenUserNotFound() {
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminUserService.getUserById(publicId.toString()))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void shouldUpdateUserDetailsSuccessfully() {
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AdminUserUpdateRequest updateRequest =
        AdminUserUpdateRequest.builder()
            .firstName("Alice Modifiée")
            .lastName("Durand")
            .email("alice.new@example.com")
            .build();

    UserResponse response = adminUserService.updateUser(publicId.toString(), updateRequest);

    assertThat(response.firstName()).isEqualTo("Alice Modifiée");
    assertThat(response.lastName()).isEqualTo("Durand");
    assertThat(response.email()).isEqualTo("alice.new@example.com");
    verify(userRepository).save(sampleUser);
  }

  @Test
  void shouldUpdateUserRoleToMerchantSuccessfully() {
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    when(entityManager.createNativeQuery(anyString())).thenReturn(queryMock);
    when(queryMock.setParameter(anyString(), any())).thenReturn(queryMock);

    Merchant merchantUser =
        Merchant.builder()
            .id(1L)
            .publicId(publicId)
            .email("alice@example.com")
            .firstName("Alice")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1995, 4, 20))
            .role(UserRole.MERCHANT)
            .failedLoginAttempts(0)
            .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
            .updatedAt(Instant.now())
            .build();

    // After role change and cache clear, findUserOrThrow re-fetches
    when(userRepository.findByPublicId(publicId))
        .thenReturn(Optional.of(sampleUser))
        .thenReturn(Optional.of(merchantUser));

    AdminUserUpdateRequest updateRequest =
        AdminUserUpdateRequest.builder()
            .firstName("Alice")
            .lastName("Dupont")
            .email("alice@example.com")
            .role(UserRole.MERCHANT)
            .build();

    UserResponse response = adminUserService.updateUser(publicId.toString(), updateRequest);

    assertThat(response.role()).isEqualTo(UserRole.MERCHANT);
    verify(entityManager).flush();
    verify(entityManager).clear();
  }

  @Test
  void shouldUnlockUser() {
    sampleUser.setLockedUntil(Instant.now().plus(1, ChronoUnit.HOURS));
    sampleUser.setFailedLoginAttempts(5);

    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = adminUserService.unlockUser(publicId.toString());

    assertThat(sampleUser.getLockedUntil()).isNull();
    assertThat(sampleUser.getFailedLoginAttempts()).isZero();
    assertThat(response.isLocked()).isFalse();
    verify(userRepository).save(sampleUser);
  }
}
