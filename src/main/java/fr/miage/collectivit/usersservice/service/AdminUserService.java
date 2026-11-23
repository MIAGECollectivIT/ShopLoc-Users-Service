package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.dto.AdminUserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PageResponse;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.UserRole;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

  PageResponse<UserResponse> getUsers(
      String query, UserRole role, Boolean locked, Instant from, Instant to, Pageable pageable);

  UserResponse getUserById(String id);

  UserResponse updateUser(String id, AdminUserUpdateRequest request);

  UserResponse unlockUser(String id);
}
