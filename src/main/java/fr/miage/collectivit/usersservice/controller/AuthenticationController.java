package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.config.OpenApiConfig;
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1", "/api/v1"})
@RequiredArgsConstructor
@Tag(
    name = "Authentication",
    description = "Operations related to user registration, login, and token verification")
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals"})
public class AuthenticationController {

  private static final String OK_STATUS = "200";

  private final AuthenticationService authenticationService;
  private final JwtTokenService jwtTokenService;
  private final TokenRevocationService tokenRevocationService;

  @Value("${application.security.registration-enabled:false}")
  private boolean registrationEnabled;

  @PostMapping("/users")
  @Operation(
      summary = "Register a new user",
      description =
          "Creates a new user account with hashed credentials and a generated client code",
      security = {})
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "User successfully registered",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid registration data",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Public registration is disabled",
        content = @Content),
    @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
  })
  public ResponseEntity<UserResponse> register(
      @Valid
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Registration payload with personal info and credentials",
              required = true)
          @RequestBody
          UserRegistrationRequest request) {
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
          "Verifies user credentials and returns a signed Bearer JWT token with user role and profile details",
      security = {})
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Authentication successful",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid login payload", content = @Content),
    @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
  })
  public ResponseEntity<AuthResponse> login(
      @Valid
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "User login credentials",
              required = true)
          @RequestBody
          LoginRequest request) {
    AuthResponse response = authenticationService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/users/logout")
  @Operation(
      summary = "Logout user and revoke JWT token",
      description = "Revokes the presented JWT token and invalidates the session",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Logged out successfully",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        name = "LogoutSuccessExample",
                        value =
                            "{\"message\": \"Déconnexion réussie. Le jeton a été révoqué.\"}"))),
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
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
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Get RFC 7517 JWKS",
      description = "Returns standard JSON Web Key Set for automated token verification",
      security = {})
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "RFC 7517 JWKS key set returned successfully",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        name = "JwksExample",
                        value =
                            "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"shoploc-key-1\",\"alg\":\"RS256\",\"n\":\"u1v2w3...\",\"e\":\"AQAB\"}]}")))
  })
  public ResponseEntity<Map<String, Object>> getJwks() {
    return ResponseEntity.ok(jwtTokenService.getJwks());
  }

  @GetMapping("/auth/revoked-tokens")
  @Operation(
      summary = "Get list of active revoked token JTIs",
      description =
          "Returns the active revoked token JTIs to synchronize consumer microservice caches",
      security = {})
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "List of revoked JTIs",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array =
                    @ArraySchema(
                        schema = @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"))))
  })
  public ResponseEntity<java.util.List<String>> getRevokedTokens() {
    return ResponseEntity.ok(tokenRevocationService.getActiveRevokedJtis());
  }

  @GetMapping("/auth/check-revocation")
  @Operation(
      summary = "Check if a token JTI is revoked",
      description = "Queries the revocation state of a specific token JTI",
      security = {})
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Revocation state of the JTI",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        name = "RevocationCheckExample",
                        value =
                            "{\"jti\": \"a1b2c3d4-e5f6-7890-abcd-ef1234567890\", \"revoked\": false}")))
  })
  public ResponseEntity<Map<String, Object>> checkRevocation(
      @Parameter(
              description = "Unique JWT Token identifier (JTI)",
              example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              required = true)
          @RequestParam("jti")
          String jti) {
    boolean revoked = tokenRevocationService.isRevoked(jti);
    return ResponseEntity.ok(Map.of("jti", jti, "revoked", revoked));
  }

  @GetMapping("/auth/public-key")
  @Operation(
      summary = "Get JWT Public Key",
      description = "Returns the RSA Public Key (X.509 SPKI Base64) used to verify JWT tokens",
      security = {})
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Public key returned successfully",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        name = "PublicKeyExample",
                        value =
                            "{\"algorithm\": \"RS256\", \"publicKey\": \"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\"}")))
  })
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
          "Validates incoming Bearer JWT token and returns user identity headers (for Traefik ForwardAuth)",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Token is valid",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        name = "VerifySuccessExample",
                        value =
                            "{\"status\": \"VALID\", \"userId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"role\": \"CLIENT\"}"))),
    @ApiResponse(
        responseCode = "401",
        description = "Missing, expired, or invalid token",
        content = @Content)
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
