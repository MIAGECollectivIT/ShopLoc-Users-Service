package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.entity.PhysicalCardNotification;
import fr.miage.collectivit.usersservice.entity.User;
import java.util.List;

public interface PhysicalCardNotificationService {

  PhysicalCardNotification processRegistrationCardRequest(User user, String pickupLocation);

  List<PhysicalCardNotification> getNotificationsForUser(Long userId);
}
