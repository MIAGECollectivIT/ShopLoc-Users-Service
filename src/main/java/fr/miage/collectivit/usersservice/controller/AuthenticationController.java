package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.dto.AuthResponse;
import fr.miage.collectivit.usersservice.dto.LoginRequest;
import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.exception.ForbiddenException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.security.JwtTokenService;
import fr.miage.collectivit.usersservice.service.AuthenticationService;
import fr.miage.collectivit.usersservice.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(
    name = "Authentication",
    description = "Operations related to user registration, login, and token verification")
public class AuthenticationController {

  private final AuthenticationService authenticationService;
  private final JwtTokenService jwtTokenService;
  private final TokenRevocationService tokenRevocationService;

  @Value("${application.security.registration-enabled:false}")
  private boolean registrationEnabled;

  @PostMapping("/users")
  @Operation(
      summary = "Register a new user",
      description =
          "Creates a new user account with hashed credentials and a generated client code")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "User successfully registered"),
    @ApiResponse(responseCode = "400", description = "Invalid registration data"),
    @ApiResponse(responseCode = "403", description = "Public registration is disabled"),
    @ApiResponse(responseCode = "409", description = "Email already in use")
  })
  public ResponseEntity<UserResponse> register(
      @Valid @RequestBody UserRegistrationRequest request) {
    if (!registrationEnabled) {
      throw new ForbiddenException("Public registration is disabled.");
    }
    UserResponse response = authenticationService.register(request);
    URI location = URI.create("/v1/users/" + response.id());
    return ResponseEntity.created(location).body(response);
  }

  @PostMapping("/users/login")
  @Operation(
      summary = "Authenticate user and issue JWT token",
      description =
          "Verifies user credentials and returns a signed Bearer JWT token with user role and profile details")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Authentication successful"),
    @ApiResponse(responseCode = "400", description = "Invalid login payload"),
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
  })
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authenticationService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/users/logout")
  @Operation(
      summary = "Logout user and revoke JWT token",
      description = "Revokes the presented JWT token and invalidates the session")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Logged out successfully"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
  })
  public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
    Claims claims = (Claims) request.getAttribute(JwtAuthInterceptor.ATTR_CLAIMS);
    if (claims != null && claims.getId() != null) {
      tokenRevocationService.revokeToken(
          claims.getId(),
          claims.getExpiration() != null ? claims.getExpiration().toInstant() : null);
    }
    return ResponseEntity.ok(Map.of("message", "Déconnexion réussie. Le jeton a été révoqué."));
  }

  @GetMapping(
      value = {"/.well-known/jwks.json", "/auth/jwks.json"},
      produces = "application/json")
  @Operation(
      summary = "Get RFC 7517 JWKS",
      description = "Returns standard JSON Web Key Set for automated token verification")
  public ResponseEntity<java.util.Map<String, Object>> getJwks() {
    return ResponseEntity.ok(jwtTokenService.getJwks());
  }

  @GetMapping("/auth/revoked-tokens")
  @Operation(
      summary = "Get list of active revoked token JTIs",
      description =
          "Returns the active revoked token JTIs to synchronize consumer microservice caches")
  public ResponseEntity<java.util.List<String>> getRevokedTokens() {
    return ResponseEntity.ok(tokenRevocationService.getActiveRevokedJtis());
  }

  @GetMapping("/auth/check-revocation")
  @Operation(
      summary = "Check if a token JTI is revoked",
      description = "Queries the revocation state of a specific token JTI")
  public ResponseEntity<java.util.Map<String, Object>> checkRevocation(
      @org.springframework.web.bind.annotation.RequestParam("jti") String jti) {
    boolean revoked = tokenRevocationService.isRevoked(jti);
    return ResponseEntity.ok(java.util.Map.of("jti", jti, "revoked", revoked));
  }

  @GetMapping("/auth/public-key")
  @Operation(
      summary = "Get JWT Public Key",
      description = "Returns the RSA Public Key (X.509 SPKI Base64) used to verify JWT tokens")
  public ResponseEntity<Map<String, String>> getPublicKey() {
    String pubKey = jwtTokenService.getPublicKeyBase64();
    return ResponseEntity.ok(
        Map.of(
            "algorithm", jwtTokenService.isAsymmetric() ? "RS256" : "HS256",
            "publicKey", pubKey != null ? pubKey : ""));
  }

  @GetMapping("/auth/verify")
  @Operation(
      summary = "Verify JWT token",
      description =
          "Validates incoming Bearer JWT token and returns user identity headers (for Traefik ForwardAuth)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Token is valid"),
    @ApiResponse(responseCode = "401", description = "Missing, expired, or invalid token")
  })
  public ResponseEntity<Map<String, String>> verify(HttpServletRequest request) {
    String userId = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ID);
    String email = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_EMAIL);
    String role = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    HttpHeaders headers = new HttpHeaders();
    if (userId != null) {
      headers.set("X-User-Id", userId);
    }
    if (email != null) {
      headers.set("X-User-Email", email);
    }
    if (role != null) {
      headers.set("X-User-Role", role);
    }

    return ResponseEntity.ok()
        .headers(headers)
        .body(
            Map.of(
                "status", "VALID", "userId", String.valueOf(userId), "role", String.valueOf(role)));
  }
}
