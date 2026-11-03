package fr.miage.collectivit.usersservice.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "merchants")
@DiscriminatorValue("MERCHANT")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class Merchant extends User {

  @Override
  public UserRole getRole() {
    return UserRole.MERCHANT;
  }
}
