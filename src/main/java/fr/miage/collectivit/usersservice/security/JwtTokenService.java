package fr.miage.collectivit.usersservice.security;

import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.exception.UnauthorizedException;
import fr.miage.collectivit.usersservice.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtTokenService {

  private static final int MIN_RSA_KEY_BYTES = 500;
  private static final int MILLIS_IN_SECOND = 1000;

  @Value("${application.security.jwt.private-key:}")
  private String privateKeyString;

  @Value("${application.security.jwt.public-key:}")
  private String publicKeyString;

  @Value("${application.security.jwt.issuer:shoploc-auth-service}")
  private String issuer;

  @Value("${application.security.jwt.expiration-ms:7200000}")
  private long expirationMs;

  @Value("${application.security.jwt.key-id:shoploc-key-1}")
  private String keyId;

  @Autowired(required = false)
  private TokenRevocationService tokenRevocationService;

  private PrivateKey rsaPrivateKey;
  private PublicKey rsaPublicKey;

  @PostConstruct
  public void init() {
    if (privateKeyString == null || privateKeyString.isBlank()) {
      throw new IllegalArgumentException(
          "No RSA private key (application.security.jwt.private-key) configured.");
    }
    String cleanKey = cleanKeyPem(privateKeyString);
    if (!initRsaKey(cleanKey)) {
      throw new IllegalArgumentException(
          "Invalid RSA PKCS#8 private key (RS256 strict mode enabled).");
    }
    if (log.isInfoEnabled()) {
      log.info("JWT Token Service initialized with asymmetric algorithm RS256.");
    }
  }

  private boolean initRsaKey(String cleanKey) {
    try {
      byte[] decoded = Base64.getDecoder().decode(cleanKey);
      if (decoded.length <= MIN_RSA_KEY_BYTES) {
        return false;
      }
      KeyFactory kf = KeyFactory.getInstance("RSA");
      this.rsaPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
      if (rsaPrivateKey instanceof RSAPrivateCrtKey crtKey) {
        RSAPublicKeySpec pubSpec =
            new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
        this.rsaPublicKey = kf.generatePublic(pubSpec);
      } else if (publicKeyString != null && !publicKeyString.isBlank()) {
        byte[] pubBytes = Base64.getDecoder().decode(cleanKeyPem(publicKeyString));
        this.rsaPublicKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
      }
      return this.rsaPrivateKey != null && this.rsaPublicKey != null;
    } catch (GeneralSecurityException | IllegalArgumentException ex) {
      if (log.isErrorEnabled()) {
        log.error("Failed to initialize RSA key for RS256: {}", ex.getMessage());
      }
      return false;
    }
  }

  public String generateToken(User user) {
    long nowMillis = System.currentTimeMillis();
    Date now = new Date(nowMillis);
    Date exp = new Date(nowMillis + expirationMs);
    String roleName = String.valueOf(user.getRole());
    String jti = UUID.randomUUID().toString();

    var builder =
        Jwts.builder()
            .header()
            .keyId(keyId)
            .and()
            .id(jti)
            .subject(user.getPublicId().toString())
            .issuer(issuer)
            .issuedAt(now)
            .expiration(exp)
            .claim("userId", user.getPublicId().toString())
            .claim("email", user.getEmail())
            .claim("role", roleName)
            .claim("firstName", user.getFirstName())
            .claim("lastName", user.getLastName());

    if (user instanceof Client client && client.getClientCode() != null) {
      builder.claim("clientCode", client.getClientCode());
    }

    return builder.signWith(rsaPrivateKey, Jwts.SIG.RS256).compact();
  }

  public Claims validateAndExtractClaims(String token) {
    Claims claims;
    try {
      claims = Jwts.parser().verifyWith(rsaPublicKey).build().parseSignedClaims(token).getPayload();
    } catch (JwtException | IllegalArgumentException ex) {
      throw new UnauthorizedException(
          "Invalid or expired authentication token: " + ex.getMessage(), ex);
    }

    if (tokenRevocationService != null
        && claims.getId() != null
        && tokenRevocationService.isRevoked(claims.getId())) {
      throw new UnauthorizedException("Token has been revoked upon logout.");
    }

    return claims;
  }

  public long getExpirationSeconds() {
    return expirationMs / MILLIS_IN_SECOND;
  }

  public boolean isAsymmetric() {
    return true;
  }

  public String getKeyId() {
    return keyId;
  }

  public java.util.Map<String, Object> getJwks() {
    if (rsaPublicKey instanceof java.security.interfaces.RSAPublicKey rsaPub) {
      String n =
          java.util.Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(rsaPub.getModulus().toByteArray());
      String e =
          java.util.Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(rsaPub.getPublicExponent().toByteArray());
      java.util.Map<String, Object> jwk =
          java.util.Map.of(
              "kty", "RSA", "use", "sig", "alg", "RS256", "kid", keyId, "n", n, "e", e);
      return java.util.Map.of("keys", java.util.List.of(jwk));
    }
    return java.util.Map.of("keys", java.util.List.of());
  }

  public String getPublicKeyBase64() {
    if (rsaPublicKey != null) {
      return Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded());
    }
    return null;
  }

  private String cleanKeyPem(String key) {
    return key.replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s+", "")
        .trim();
  }
}
