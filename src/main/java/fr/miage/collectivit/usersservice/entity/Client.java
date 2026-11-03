package fr.miage.collectivit.usersservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "clients")
@DiscriminatorValue("CLIENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class Client extends User {

  @Column(name = "client_code", nullable = false, unique = true, length = 7)
  private String clientCode;

  // Physical card pickup location is optional as the customer can choose not to request a physical card
  @Column(name = "pickup_location", length = 150)
  private String pickupLocation;

  // Uses SQL ARRAY mapping supported natively in PostgreSQL and modern dialects
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "license_plates")
  @Builder.Default
  private List<String> licensePlates = new ArrayList<>();

  @Override
  public UserRole getRole() {
    return UserRole.CLIENT;
  }
}
