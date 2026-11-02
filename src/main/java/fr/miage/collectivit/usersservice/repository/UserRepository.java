package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByPublicId(UUID publicId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByClientCode(String clientCode);

  boolean existsByClientCode(String clientCode);

  @Query(
      "SELECT u FROM User u WHERE u.role = :role AND (:query IS NULL OR :query = '' OR"
          + " LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.lastName) LIKE"
          + " LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query,"
          + " '%')))")
  List<User> searchByRoleAndQuery(
      @Param("role") UserRole role, @Param("query") String query, Pageable pageable);
}
