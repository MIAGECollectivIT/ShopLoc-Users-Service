package fr.miage.collectivit.usersservice.service;

import java.time.Instant;
import java.util.List;

public interface TokenRevocationService {

  void revokeToken(String jti, Instant expiresAt);

  boolean isRevoked(String jti);

  List<String> getActiveRevokedJtis();

  void purgeExpiredTokens();
}
