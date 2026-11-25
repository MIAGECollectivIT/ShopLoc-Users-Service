package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.service.UserRoleTransitionService;
import fr.miage.collectivit.usersservice.util.ClientCodeGenerator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transitions user roles across the joined-inheritance tables (users, clients, merchants, admins).
 */
@Service
@RequiredArgsConstructor
public class UserRoleTransitionServiceImpl implements UserRoleTransitionService {

  private static final String PARAM_USER_ID = "userId";

  private final EntityManager entityManager;
  private final UserRepository userRepository;
  private final ClientCodeGenerator clientCodeGenerator;

  @Override
  @Transactional
  public void changeUserRole(Long userId, UserRole newRole) {
    entityManager
        .createNativeQuery(
            "UPDATE users SET role = :role, updated_at = CURRENT_TIMESTAMP WHERE id = :userId")
        .setParameter("role", newRole.name())
        .setParameter(PARAM_USER_ID, userId)
        .executeUpdate();

    switch (newRole) {
      case MERCHANT -> {
        if (!existsInTable("merchants", userId)) {
          entityManager
              .createNativeQuery("INSERT INTO merchants (id) VALUES (:userId)")
              .setParameter(PARAM_USER_ID, userId)
              .executeUpdate();
        }
        deleteFromTable("clients", userId);
        deleteFromTable("admins", userId);
      }
      case ADMIN -> {
        if (!existsInTable("admins", userId)) {
          entityManager
              .createNativeQuery("INSERT INTO admins (id) VALUES (:userId)")
              .setParameter(PARAM_USER_ID, userId)
              .executeUpdate();
        }
        deleteFromTable("clients", userId);
        deleteFromTable("merchants", userId);
      }
      case CLIENT -> {
        if (!existsInTable("clients", userId)) {
          String clientCode = generateUniqueClientCode();
          entityManager
              .createNativeQuery(
                  "INSERT INTO clients (id, client_code, license_plates) VALUES (:userId, :clientCode, '{}')")
              .setParameter(PARAM_USER_ID, userId)
              .setParameter("clientCode", clientCode)
              .executeUpdate();
        }
        deleteFromTable("merchants", userId);
        deleteFromTable("admins", userId);
      }
    }

    entityManager.flush();
    entityManager.clear();
  }

  private boolean existsInTable(String tableName, Long userId) {
    Object count =
        entityManager
            .createNativeQuery("SELECT COUNT(*) FROM " + tableName + " WHERE id = :userId")
            .setParameter(PARAM_USER_ID, userId)
            .getSingleResult();
    return count instanceof Number number && number.longValue() > 0;
  }

  private void deleteFromTable(String tableName, Long userId) {
    entityManager
        .createNativeQuery("DELETE FROM " + tableName + " WHERE id = :userId")
        .setParameter(PARAM_USER_ID, userId)
        .executeUpdate();
  }

  private String generateUniqueClientCode() {
    String code;
    do {
      code = clientCodeGenerator.generateCode();
    } while (userRepository.existsByClientCode(code));
    return code;
  }
}
