package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.RevokedToken;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

  boolean existsByJti(String jti);

  @Query("SELECT r.jti FROM RevokedToken r WHERE r.expiresAt > :now")
  List<String> findActiveRevokedJtis(@Param("now") Instant now);

  void deleteByExpiresAtBefore(Instant now);
}
