package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.entity.RevokedToken;
import fr.miage.collectivit.usersservice.repository.RevokedTokenRepository;
import fr.miage.collectivit.usersservice.service.TokenRevocationService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationServiceImpl implements TokenRevocationService {

  private static final String CACHE_REVOKED_JTIS = "revokedJtis";
  private static final String CACHE_REVOKED_LIST = "revokedTokensList";

  private final RevokedTokenRepository revokedTokenRepository;

  @Override
  @Transactional
  @CacheEvict(
      value = {CACHE_REVOKED_JTIS, CACHE_REVOKED_LIST},
      allEntries = true)
  public void revokeToken(String jti, Instant expiresAt) {
    if (jti == null || jti.isBlank()) {
      return;
    }
    Instant expiration = expiresAt != null ? expiresAt : Instant.now().plusSeconds(7200);
    RevokedToken revoked =
        RevokedToken.builder().jti(jti).revokedAt(Instant.now()).expiresAt(expiration).build();
    revokedTokenRepository.save(revoked);
    log.info("Token jti={} successfully revoked until {}", jti, expiration);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(
      value = CACHE_REVOKED_JTIS,
      key = "#jti",
      condition = "#jti != null && !#jti.isBlank()")
  public boolean isRevoked(String jti) {
    if (jti == null || jti.isBlank()) {
      return false;
    }
    return revokedTokenRepository.existsByJti(jti);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = CACHE_REVOKED_LIST, key = "'active'")
  public List<String> getActiveRevokedJtis() {
    return revokedTokenRepository.findActiveRevokedJtis(Instant.now());
  }

  @Override
  @Transactional
  @Scheduled(cron = "${application.security.jwt.revocation-purge-cron:0 0 * * * *}")
  @CacheEvict(
      value = {CACHE_REVOKED_JTIS, CACHE_REVOKED_LIST},
      allEntries = true)
  public void purgeExpiredTokens() {
    Instant now = Instant.now();
    log.debug("Executing purge of revoked tokens expired before {}", now);
    revokedTokenRepository.deleteByExpiresAtBefore(now);
  }
}
