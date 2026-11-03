package fr.miage.collectivit.usersservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.dto.AuthResponse;
import fr.miage.collectivit.usersservice.dto.LoginRequest;
import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.AccountLockedException;
import fr.miage.collectivit.usersservice.exception.EmailAlreadyExistsException;
import fr.miage.collectivit.usersservice.exception.UnauthorizedException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.security.JwtTokenService;
import fr.miage.collectivit.usersservice.service.impl.AuthenticationServiceImpl;
import fr.miage.collectivit.usersservice.util.ClientCodeGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
class AuthenticationServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private ClientCodeGenerator clientCodeGenerator;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private JwtTokenService jwtTokenService;

  private AuthenticationService authenticationService;
  private UserRegistrationRequest registrationRequest;

  @BeforeEach
  void setUp() {
    authenticationService =
        new AuthenticationServiceImpl(
            userRepository,
            passwordEncoder,
            clientCodeGenerator,
            transactionTemplate,
            jwtTokenService,
            5,
            15);

    registrationRequest =
        UserRegistrationRequest.builder()
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.bernard@example.com")
            .password("SecurePass99!")
            .pickupLocation("Mairie Centrale")
            .licensePlates(List.of("AA-111-AA"))
            .build();

    org.mockito.Mockito.lenient()
        .when(transactionTemplate.execute(any()))
        .thenAnswer(
            invocation -> {
              TransactionCallback<?> callback = invocation.getArgument(0);
              return callback.doInTransaction(null);
            });
  }

  @Test
  void shouldRegisterNewUserSuccessfully() {
    when(userRepository.existsByEmail("claire.bernard@example.com")).thenReturn(false);
    when(clientCodeGenerator.generateCode()).thenReturn("cOdE123");
    when(userRepository.existsByClientCode("cOdE123")).thenReturn(false);
    when(passwordEncoder.encode("SecurePass99!")).thenReturn("$2a$12$hashedPassword");

    UUID generatedId = UUID.randomUUID();
    Client savedEntity =
        Client.builder()
            .id(1L)
            .publicId(generatedId)
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.bernard@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .clientCode("cOdE123")
            .pickupLocation("Mairie Centrale")
            .licensePlates(List.of("AA-111-AA"))
            .build();

    when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedEntity);

    UserResponse response = authenticationService.register(registrationRequest);

    assertThat(response.id()).isEqualTo(generatedId);
    assertThat(response.email()).isEqualTo("claire.bernard@example.com");
    assertThat(response.clientCode()).isEqualTo("cOdE123");
    assertThat(response.pickupLocation()).isEqualTo("Mairie Centrale");
    assertThat(response.licensePlates()).containsExactly("AA-111-AA");
    verify(userRepository).saveAndFlush(any(User.class));
  }

  @Test
  void shouldRegisterNewUserWithoutPickupLocationSuccessfully() {
    UUID generatedId = UUID.randomUUID();
    UserRegistrationRequest requestWithoutLocation =
        UserRegistrationRequest.builder()
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.demat@example.com")
            .password("SecurePass99!")
            .pickupLocation(null)
            .licensePlates(List.of())
            .build();

    when(userRepository.existsByEmail("claire.demat@example.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
    when(clientCodeGenerator.generateCode()).thenReturn("cOdE123");
    when(userRepository.existsByClientCode("cOdE123")).thenReturn(false);

    Client savedEntity =
        Client.builder()
            .id(1L)
            .publicId(generatedId)
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.demat@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .clientCode("cOdE123")
            .pickupLocation(null)
            .licensePlates(List.of())
            .build();

    when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedEntity);

    UserResponse response = authenticationService.register(requestWithoutLocation);

    assertThat(response.id()).isEqualTo(generatedId);
    assertThat(response.pickupLocation()).isNull();
  }

  @Test
  void shouldNormalizeBlankPickupLocationToNullOnRegistration() {
    UUID generatedId = UUID.randomUUID();
    UserRegistrationRequest requestWithBlankLocation =
        UserRegistrationRequest.builder()
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.blank@example.com")
            .password("SecurePass99!")
            .pickupLocation("   ")
            .build();

    when(userRepository.existsByEmail("claire.blank@example.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
    when(clientCodeGenerator.generateCode()).thenReturn("cOdE123");
    when(userRepository.existsByClientCode("cOdE123")).thenReturn(false);

    Client savedEntity =
        Client.builder()
            .id(1L)
            .publicId(generatedId)
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.blank@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .clientCode("cOdE123")
            .pickupLocation(null)
            .licensePlates(List.of())
            .build();

    when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedEntity);

    UserResponse response = authenticationService.register(requestWithBlankLocation);

    assertThat(response.id()).isEqualTo(generatedId);
    assertThat(response.pickupLocation()).isNull();
  }

  @Test
  void shouldRetryClientCodeGenerationOnCollision() {
    when(userRepository.existsByEmail("claire.bernard@example.com")).thenReturn(false);
    when(passwordEncoder.encode("SecurePass99!")).thenReturn("$2a$12$hashedPassword");
    when(clientCodeGenerator.generateCode()).thenReturn("COLLIDE", "cOdE123");
    when(userRepository.existsByClientCode("COLLIDE")).thenReturn(true);
    when(userRepository.existsByClientCode("cOdE123")).thenReturn(false);

    UUID generatedId = UUID.randomUUID();
    Client savedEntity =
        Client.builder()
            .id(1L)
            .publicId(generatedId)
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.bernard@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .clientCode("cOdE123")
            .pickupLocation("Mairie Centrale")
            .licensePlates(List.of("AA-111-AA"))
            .build();

    when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedEntity);

    UserResponse response = authenticationService.register(registrationRequest);

    assertThat(response.clientCode()).isEqualTo("cOdE123");
  }

  @Test
  void shouldThrowEmailAlreadyExistsExceptionWhenEmailIsTaken() {
    when(userRepository.existsByEmail("claire.bernard@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authenticationService.register(registrationRequest))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessageContaining("claire.bernard@example.com");

    verify(userRepository, never()).saveAndFlush(any(User.class));
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  void shouldLockAccountAfterMaxFailedAttempts() {
    User user =
        User.builder()
            .id(1L)
            .email("claire.bernard@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .failedLoginAttempts(4)
            .build();

    when(userRepository.findByEmail("claire.bernard@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("WrongPassword123!", "$2a$12$hashedPassword")).thenReturn(false);

    LoginRequest req = new LoginRequest("claire.bernard@example.com", "WrongPassword123!");

    assertThatThrownBy(() -> authenticationService.login(req))
        .isInstanceOf(AccountLockedException.class)
        .hasMessageContaining("bloqué");

    assertThat(user.isLocked()).isTrue();
    verify(userRepository).save(user);
  }

  @Test
  void shouldRejectLoginWhenAccountIsAlreadyLocked() {
    User user =
        User.builder()
            .id(1L)
            .email("claire.bernard@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .lockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES))
            .build();

    when(userRepository.findByEmail("claire.bernard@example.com")).thenReturn(Optional.of(user));

    LoginRequest req = new LoginRequest("claire.bernard@example.com", "AnyPassword123!");

    assertThatThrownBy(() -> authenticationService.login(req))
        .isInstanceOf(AccountLockedException.class)
        .hasMessageContaining("bloqué");

    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  void shouldResetLockoutOnSuccessfulLogin() {
    User user =
        User.builder()
            .id(1L)
            .email("claire.bernard@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .failedLoginAttempts(2)
            .build();

    when(userRepository.findByEmail("claire.bernard@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("GoodPassword123!", "$2a$12$hashedPassword")).thenReturn(true);
    when(jwtTokenService.generateToken(user)).thenReturn("jwt-token");
    when(jwtTokenService.getExpirationSeconds()).thenReturn(7200L);

    LoginRequest req = new LoginRequest("claire.bernard@example.com", "GoodPassword123!");

    AuthResponse resp = authenticationService.login(req);

    assertThat(resp.token()).isEqualTo("jwt-token");
    assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
    assertThat(user.getLockedUntil()).isNull();
    verify(userRepository).save(user);
  }

  @Test
  void shouldRejectInvalidCredentials() {
    User user =
        User.builder()
            .id(1L)
            .email("claire.bernard@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .failedLoginAttempts(0)
            .build();

    when(userRepository.findByEmail("claire.bernard@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("WrongPassword123!", "$2a$12$hashedPassword")).thenReturn(false);

    LoginRequest req = new LoginRequest("claire.bernard@example.com", "WrongPassword123!");

    assertThatThrownBy(() -> authenticationService.login(req))
        .isInstanceOf(UnauthorizedException.class);

    assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    verify(userRepository).save(user);
  }
}
