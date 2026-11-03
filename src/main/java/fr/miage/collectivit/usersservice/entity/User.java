package fr.miage.collectivit.usersservice.entity;

import static fr.miage.collectivit.usersservice.entity.UserRole.CLIENT;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("USER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, updatable = false)
  private UUID publicId;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @ToString.Exclude
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20, insertable = false, updatable = false)
  @Builder.Default
  private UserRole role = CLIENT;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public UserRole getRole() {
    return role != null ? role : CLIENT;
  }

  public String getClientCode() {
    return null;
  }

  public String getPickupLocation() {
    return null;
  }

  public void setPickupLocation(String pickupLocation) {
    // Base implementation is a no-op; overridden in Client
  }

  public List<String> getLicensePlates() {
    return List.of();
  }

  public boolean isLocked() {
    return lockedUntil != null && lockedUntil.isAfter(Instant.now());
  }

  @SuppressWarnings("PMD.NullAssignment")
  public void resetLockout() {
    this.failedLoginAttempts = 0;
    this.lockedUntil = null;
  }

  public void incrementFailedAttempts(int maxAttempts, long lockoutMinutes) {
    this.failedLoginAttempts++;
    if (this.failedLoginAttempts >= maxAttempts) {
      this.lockedUntil = Instant.now().plus(lockoutMinutes, ChronoUnit.MINUTES);
    }
  }

  @PrePersist
  protected void onCreate() {
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }
}
