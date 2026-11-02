package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Discovery", description = "Standard IETF / OAuth2 Discovery Endpoints")
public class JwksDiscoveryController {

  private final JwtTokenService jwtTokenService;

  @GetMapping(value = "/.well-known/jwks.json", produces = "application/json")
  @Operation(
      summary = "Get RFC 7517 JWKS at root domain",
      description = "Standard discovery endpoint returning the RFC 7517 JSON Web Key Set")
  public ResponseEntity<Map<String, Object>> getRootJwks() {
    return ResponseEntity.ok(jwtTokenService.getJwks());
  }
}
