package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.entity.UserRole;

/**
 * Handles role transitions across the joined inheritance user tables (users, clients, merchants,
 * admins).
 */
public interface UserRoleTransitionService {

  void changeUserRole(Long userId, UserRole newRole);
}
