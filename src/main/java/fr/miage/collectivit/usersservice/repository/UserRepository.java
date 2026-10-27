package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByPublicId(UUID publicId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByClientCode(String clientCode);

  boolean existsByClientCode(String clientCode);
}
