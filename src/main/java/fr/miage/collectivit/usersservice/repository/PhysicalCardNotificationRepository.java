package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.CardNotificationStatus;
import fr.miage.collectivit.usersservice.entity.PhysicalCardNotification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhysicalCardNotificationRepository
    extends JpaRepository<PhysicalCardNotification, Long> {

  List<PhysicalCardNotification> findByUserId(Long userId);

  List<PhysicalCardNotification> findByClientCode(String clientCode);

  List<PhysicalCardNotification> findByStatus(CardNotificationStatus status);
}
