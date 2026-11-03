package fr.miage.collectivit.usersservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.usersservice.entity.Admin;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTest {

  private JwtTokenService jwtTokenService;

  @BeforeEach
  void setUp() {
    jwtTokenService = new JwtTokenService();
    ReflectionTestUtils.setField(
        jwtTokenService, "secretKeyString", "super-secret-key-that-is-at-least-256-bits-long!");
    ReflectionTestUtils.setField(jwtTokenService, "issuer", "shoploc-auth-service");
    ReflectionTestUtils.setField(jwtTokenService, "expirationMs", 7200000L);
    jwtTokenService.init();
  }

  @Test
  @DisplayName("Génère un token valide avec claims userId, role, email et clientCode pour un Client")
  void shouldGenerateAndValidateTokenSuccessfully() {
    UUID publicId = UUID.randomUUID();
    Client user =
        Client.builder()
            .publicId(publicId)
            .email("alice@shoploc.fr")
            .clientCode("CL12345")
            .firstName("Alice")
            .lastName("Citoyenne")
            .birthDate(LocalDate.of(1985, 4, 12))
            .build();

    String token = jwtTokenService.generateToken(user);
    assertThat(token).isNotBlank();

    Claims claims = jwtTokenService.validateAndExtractClaims(token);
    assertThat(claims.getSubject()).isEqualTo(publicId.toString());
    assertThat(claims.get("userId", String.class)).isEqualTo(publicId.toString());
    assertThat(claims.get("email", String.class)).isEqualTo("alice@shoploc.fr");
    assertThat(claims.get("role", String.class)).isEqualTo("CLIENT");
    assertThat(claims.get("clientCode", String.class)).isEqualTo("CL12345");
  }

  @Test
  @DisplayName("Génère un token pour Admin sans claim clientCode")
  void shouldGenerateTokenForAdminWithoutClientCode() {
    UUID publicId = UUID.randomUUID();
    Admin admin =
        Admin.builder()
            .publicId(publicId)
            .email("admin@shoploc.fr")
            .firstName("Marius")
            .lastName("Vasseur")
            .birthDate(LocalDate.of(1982, 5, 14))
            .build();

    String token = jwtTokenService.generateToken(admin);
    Claims claims = jwtTokenService.validateAndExtractClaims(token);

    assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    assertThat(claims.get("clientCode", String.class)).isNull();
  }

  @Test
  @DisplayName("Rejette un token malformé ou falsifié avec UnauthorizedException")
  void shouldRejectMalformedToken() {
    assertThatThrownBy(() -> jwtTokenService.validateAndExtractClaims("not.a.valid.jwt.token"))
        .isInstanceOf(UnauthorizedException.class);
  }
}
