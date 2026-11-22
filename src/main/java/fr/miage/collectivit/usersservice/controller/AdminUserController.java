package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.config.OpenApiConfig;
import fr.miage.collectivit.usersservice.dto.AdminUserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PageResponse;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.ForbiddenException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/admin/users", "/api/v1/admin/users"})
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Administrative operations on all user accounts")
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals"})
public class AdminUserController {

  private static final String ROLE_ADMIN = "ADMIN";

  private final AdminUserService adminUserService;

  @GetMapping
  @Operation(
      summary = "List all users with filters and pagination",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  public ResponseEntity<PageResponse<UserResponse>> getUsers(
      @RequestParam(value = "query", required = false) String query,
      @RequestParam(value = "role", required = false) UserRole role,
      @RequestParam(value = "locked", required = false) Boolean locked,
      @RequestParam(value = "from", required = false) Instant from,
      @RequestParam(value = "to", required = false) Instant to,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      HttpServletRequest request) {
    checkAdminRole(request);
    Pageable pageable =
        PageRequest.of(
            Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("createdAt").descending());
    return ResponseEntity.ok(adminUserService.getUsers(query, role, locked, from, to, pageable));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get user details by ID",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  public ResponseEntity<UserResponse> getUserById(
      @PathVariable("id") String id, HttpServletRequest request) {
    checkAdminRole(request);
    return ResponseEntity.ok(adminUserService.getUserById(id));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update user details",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  public ResponseEntity<UserResponse> updateUser(
      @PathVariable("id") String id,
      @Valid @RequestBody AdminUserUpdateRequest requestBody,
      HttpServletRequest request) {
    checkAdminRole(request);
    return ResponseEntity.ok(adminUserService.updateUser(id, requestBody));
  }

  @PostMapping("/{id}/unlock")
  @Operation(
      summary = "Unlock temporarily locked user account",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  public ResponseEntity<UserResponse> unlockUser(
      @PathVariable("id") String id, HttpServletRequest request) {
    checkAdminRole(request);
    return ResponseEntity.ok(adminUserService.unlockUser(id));
  }

  private void checkAdminRole(HttpServletRequest request) {
    if (request == null) {
      throw new ForbiddenException("Access restricted to administrators");
    }
    String role = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);
    if (!ROLE_ADMIN.equalsIgnoreCase(role)) {
      throw new ForbiddenException("Access restricted to administrators");
    }
  }
}
