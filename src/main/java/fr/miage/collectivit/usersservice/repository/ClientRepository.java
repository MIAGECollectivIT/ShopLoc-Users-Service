package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.Client;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

  Optional<Client> findByPublicId(UUID publicId);

  Optional<Client> findByEmail(String email);

  Optional<Client> findByClientCode(String clientCode);

  boolean existsByClientCode(String clientCode);
}
