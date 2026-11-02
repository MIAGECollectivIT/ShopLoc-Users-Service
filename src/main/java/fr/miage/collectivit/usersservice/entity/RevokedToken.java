package fr.miage.collectivit.usersservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {

  @Id
  @Column(name = "jti", length = 64, nullable = false)
  private String jti;

  @Column(name = "revoked_at", nullable = false)
  private Instant revokedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
}
