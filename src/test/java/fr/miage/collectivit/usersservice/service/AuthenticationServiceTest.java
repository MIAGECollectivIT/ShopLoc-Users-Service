package fr.miage.collectivit.usersservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.EmailAlreadyExistsException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.service.impl.AuthenticationServiceImpl;
import fr.miage.collectivit.usersservice.util.ClientCodeGenerator;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private ClientCodeGenerator clientCodeGenerator;

  @InjectMocks private AuthenticationServiceImpl authenticationService;

  private UserRegistrationRequest registrationRequest;

  @BeforeEach
  void setUp() {
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
  }

  @Test
  void shouldRegisterNewUserSuccessfully() {
    when(userRepository.existsByEmail("claire.bernard@example.com")).thenReturn(false);
    when(clientCodeGenerator.generateCode()).thenReturn("cOdE123");
    when(userRepository.existsByClientCode("cOdE123")).thenReturn(false);
    when(passwordEncoder.encode("SecurePass99!")).thenReturn("$2a$12$hashedPassword");

    UUID generatedId = UUID.randomUUID();
    User savedEntity =
        User.builder()
            .id(1L)
            .publicId(generatedId)
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.bernard@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .role(UserRole.CLIENT)
            .clientCode("cOdE123")
            .pickupLocation("Mairie Centrale")
            .licensePlates(List.of("AA-111-AA"))
            .build();

    when(userRepository.save(any(User.class))).thenReturn(savedEntity);

    UserResponse response = authenticationService.register(registrationRequest);

    assertThat(response.id()).isEqualTo(generatedId);
    assertThat(response.email()).isEqualTo("claire.bernard@example.com");
    assertThat(response.clientCode()).isEqualTo("cOdE123");
    assertThat(response.pickupLocation()).isEqualTo("Mairie Centrale");
    assertThat(response.licensePlates()).containsExactly("AA-111-AA");
    verify(userRepository).save(any(User.class));
  }

  // Physical card pickup location is optional as the customer can choose not to request a physical
  // card
  @Test
  void shouldRegisterNewUserWithoutPickupLocationSuccessfully() {
    UserRegistrationRequest noPickupRequest =
        UserRegistrationRequest.builder()
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.bernard@example.com")
            .password("SecurePass99!")
            .pickupLocation(null)
            .build();

    when(userRepository.existsByEmail("claire.bernard@example.com")).thenReturn(false);
    when(clientCodeGenerator.generateCode()).thenReturn("cOdE123");
    when(userRepository.existsByClientCode("cOdE123")).thenReturn(false);
    when(passwordEncoder.encode("SecurePass99!")).thenReturn("$2a$12$hashedPassword");

    UUID generatedId = UUID.randomUUID();
    User savedEntity =
        User.builder()
            .id(1L)
            .publicId(generatedId)
            .firstName("Claire")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1993, 7, 24))
            .email("claire.bernard@example.com")
            .passwordHash("$2a$12$hashedPassword")
            .role(UserRole.CLIENT)
            .clientCode("cOdE123")
            .pickupLocation(null)
            .licensePlates(List.of())
            .build();

    when(userRepository.save(any(User.class))).thenReturn(savedEntity);

    UserResponse response = authenticationService.register(noPickupRequest);

    assertThat(response.id()).isEqualTo(generatedId);
    assertThat(response.pickupLocation()).isNull();
    verify(userRepository).save(any(User.class));
  }

  @Test
  void shouldThrowEmailAlreadyExistsWhenEmailTaken() {
    when(userRepository.existsByEmail("claire.bernard@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authenticationService.register(registrationRequest))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessageContaining("claire.bernard@example.com");

    verify(userRepository, never()).save(any(User.class));
    verify(passwordEncoder, never()).encode(anyString());
  }
}
