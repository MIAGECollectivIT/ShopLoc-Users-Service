package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

  Optional<Client> findByClientCode(String clientCode);
}
