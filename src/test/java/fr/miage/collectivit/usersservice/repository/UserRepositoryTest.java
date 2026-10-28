package fr.miage.collectivit.usersservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  void shouldSaveAndRetrieveUserWithAllAttributes() {
    User user =
        User.builder()
            .email("client.dupont@example.com")
            .passwordHash("$2a$12$e8r3X4Yk7Z1abcFakeHashForTestingOnly")
            .role(UserRole.CLIENT)
            .firstName("Jean")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1990, 5, 15))
            .clientCode("aB3dE7K")
            .pickupLocation("Mairie Centrale")
            .licensePlates(List.of("AA-123-AA", "BB-456-BB"))
            .build();

    User saved = userRepository.save(user);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getPublicId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    Optional<User> foundByPublicId = userRepository.findByPublicId(saved.getPublicId());
    assertThat(foundByPublicId).isPresent();
    assertThat(foundByPublicId.get().getEmail()).isEqualTo("client.dupont@example.com");
    assertThat(foundByPublicId.get().getFirstName()).isEqualTo("Jean");
    assertThat(foundByPublicId.get().getLastName()).isEqualTo("Dupont");
    assertThat(foundByPublicId.get().getRole()).isEqualTo(UserRole.CLIENT);
    assertThat(foundByPublicId.get().getPickupLocation()).isEqualTo("Mairie Centrale");
    assertThat(foundByPublicId.get().getLicensePlates()).containsExactly("AA-123-AA", "BB-456-BB");
    assertThat(foundByPublicId.get().getClientCode()).isEqualTo("aB3dE7K");
  }

  @Test
  void shouldSaveAndRetrieveUserWithoutPickupLocation() {
    User user =
        User.builder()
            .email("no.pickup@example.com")
            .passwordHash("$2a$12$e8r3X4Yk7Z1abcFakeHashForTestingOnly")
            .role(UserRole.CLIENT)
            .firstName("Chloe")
            .lastName("Giraud")
            .birthDate(LocalDate.of(1996, 9, 12))
            .clientCode("cG96kL1")
            .pickupLocation(null)
            .build();

    User saved = userRepository.save(user);

    Optional<User> foundByPublicId = userRepository.findByPublicId(saved.getPublicId());
    assertThat(foundByPublicId).isPresent();
    assertThat(foundByPublicId.get().getPickupLocation()).isNull();
  }

  @Test
  void shouldFindUserByEmailAndCheckExistence() {
    User user =
        User.builder()
            .email("unique.email@example.com")
            .passwordHash("$2a$12$e8r3X4Yk7Z1abcFakeHashForTestingOnly")
            .firstName("Alice")
            .lastName("Martin")
            .birthDate(LocalDate.of(1985, 3, 20))
            .clientCode("c1D2e3F")
            .pickupLocation("Mairie")
            .licensePlates(new ArrayList<>())
            .build();

    userRepository.save(user);

    assertThat(userRepository.existsByEmail("unique.email@example.com")).isTrue();
    assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();

    Optional<User> found = userRepository.findByEmail("unique.email@example.com");
    assertThat(found).isPresent();
    assertThat(found.get().getFirstName()).isEqualTo("Alice");
  }

  @Test
  void shouldFindUserByClientCodeAndCheckExistence() {
    User user =
        User.builder()
            .email("client.code@example.com")
            .passwordHash("$2a$12$e8r3X4Yk7Z1abcFakeHashForTestingOnly")
            .firstName("Bob")
            .lastName("Durand")
            .birthDate(LocalDate.of(1995, 12, 1))
            .clientCode("xY9z0W1")
            .pickupLocation("Commerce Partenaire B")
            .build();

    userRepository.save(user);

    assertThat(userRepository.existsByClientCode("xY9z0W1")).isTrue();
    assertThat(userRepository.existsByClientCode("0000000")).isFalse();

    Optional<User> found = userRepository.findByClientCode("xY9z0W1");
    assertThat(found).isPresent();
    assertThat(found.get().getLastName()).isEqualTo("Durand");
  }

  @Test
  void shouldEnforceEmailUniquenessConstraint() {
    User user1 =
        User.builder()
            .email("duplicate@example.com")
            .passwordHash("$2a$12$e8r3X4Yk7Z1abcFakeHashForTestingOnly")
            .firstName("First")
            .lastName("User")
            .birthDate(LocalDate.of(2000, 1, 1))
            .clientCode("code001")
            .pickupLocation("Mairie")
            .build();

    userRepository.saveAndFlush(user1);

    User user2 =
        User.builder()
            .email("duplicate@example.com")
            .passwordHash("$2a$12$e8r3X4Yk7Z1abcFakeHashForTestingOnly")
            .firstName("Second")
            .lastName("User")
            .birthDate(LocalDate.of(2000, 2, 2))
            .clientCode("code002")
            .pickupLocation("Mairie")
            .build();

    assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldSupportEmptyLicensePlates() {
    User user =
        User.builder()
            .email("no.plates@example.com")
            .passwordHash("$2a$12$e8r3X4Yk7Z1abcFakeHashForTestingOnly")
            .firstName("Claire")
            .lastName("Lemoine")
            .birthDate(LocalDate.of(1992, 8, 10))
            .clientCode("kL3mN5P")
            .pickupLocation("Mairie")
            .build();

    User saved = userRepository.save(user);

    Optional<User> found = userRepository.findByPublicId(saved.getPublicId());
    assertThat(found).isPresent();
    assertThat(found.get().getLicensePlates()).isEmpty();
  }
}
