package fr.miage.collectivit.usersservice.security;

import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.exception.UnauthorizedException;
import fr.miage.collectivit.usersservice.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
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
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtTokenService {

  private static final int MIN_KEY_BYTES = 32;
  private static final int MIN_RSA_KEY_BYTES = 500;
  private static final int MILLIS_IN_SECOND = 1000;

  @Value("${application.security.jwt.private-key:${application.security.jwt.secret-key:}}")
  private String privateKeyString;

  @Value("${application.security.jwt.secret-key:}")
  private String secretKeyString;

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
  private SecretKey hmacSecretKey;
  private boolean asymmetric;

  @PostConstruct
  public void init() {
    resolveKeyConfiguration();
    String cleanKey = cleanKeyPem(privateKeyString);

    if (initRsaKey(cleanKey)) {
      this.asymmetric = true;
      log.info("JWT Token Service initialisé avec algorithme asymétrique RS256.");
    } else {
      initHmacKey(cleanKey);
      this.asymmetric = false;
      log.info("JWT Token Service initialisé avec algorithme symétrique HS256.");
    }
  }

  private void resolveKeyConfiguration() {
    if ((privateKeyString == null || privateKeyString.isBlank())
        && (secretKeyString != null && !secretKeyString.isBlank())) {
      privateKeyString = secretKeyString;
    }
    if (privateKeyString == null || privateKeyString.isBlank()) {
      throw new IllegalArgumentException(
          "Aucune clé de signature JWT (application.security.jwt.private-key ou secret-key) n'a été configurée.");
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
      return true;
    } catch (GeneralSecurityException | IllegalArgumentException ex) {
      if (log.isDebugEnabled()) {
        log.debug(
            "La clé n'est pas une clé RSA PKCS#8 valide, essai en tant que clé HMAC symétrique: {}",
            ex.getMessage());
      }
      return false;
    }
  }

  private void initHmacKey(String cleanKey) {
    byte[] hmacBytes;
    try {
      hmacBytes = Base64.getDecoder().decode(cleanKey);
      if (hmacBytes.length < MIN_KEY_BYTES) {
        hmacBytes = cleanKey.getBytes(StandardCharsets.UTF_8);
      }
    } catch (IllegalArgumentException notBase64) {
      hmacBytes = cleanKey.getBytes(StandardCharsets.UTF_8);
    }
    this.hmacSecretKey = Keys.hmacShaKeyFor(hmacBytes);
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
            .claim("dbId", user.getId())
            .claim("email", user.getEmail())
            .claim("role", roleName)
            .claim("clientCode", user.getClientCode())
            .claim("firstName", user.getFirstName())
            .claim("lastName", user.getLastName());

    if (asymmetric && rsaPrivateKey != null) {
      return builder.signWith(rsaPrivateKey, Jwts.SIG.RS256).compact();
    } else {
      return builder.signWith(hmacSecretKey).compact();
    }
  }

  public Claims validateAndExtractClaims(String token) {
    try {
      Claims claims;
      if (asymmetric && rsaPublicKey != null) {
        claims =
            Jwts.parser().verifyWith(rsaPublicKey).build().parseSignedClaims(token).getPayload();
      } else {
        claims =
            Jwts.parser().verifyWith(hmacSecretKey).build().parseSignedClaims(token).getPayload();
      }

      if (tokenRevocationService != null
          && claims.getId() != null
          && tokenRevocationService.isRevoked(claims.getId())) {
        throw new UnauthorizedException("Jeton révoqué suite à une déconnexion.");
      }

      return claims;
    } catch (JwtException | IllegalArgumentException ex) {
      throw new UnauthorizedException(
          "Jeton d'authentification invalide ou expiré : " + ex.getMessage(), ex);
    }
  }

  public long getExpirationSeconds() {
    return expirationMs / MILLIS_IN_SECOND;
  }

  public boolean isAsymmetric() {
    return asymmetric;
  }

  public String getKeyId() {
    return keyId;
  }

  public java.util.Map<String, Object> getJwks() {
    if (asymmetric && rsaPublicKey instanceof java.security.interfaces.RSAPublicKey rsaPub) {
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
