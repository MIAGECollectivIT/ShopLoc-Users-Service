package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.config.OpenApiConfig;
import fr.miage.collectivit.usersservice.dto.CitizenCardEmailSettingResponse;
import fr.miage.collectivit.usersservice.dto.CitizenCardEmailSettingUpdateRequest;
import fr.miage.collectivit.usersservice.exception.ForbiddenException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/admin/settings", "/api/v1/admin/settings"})
@RequiredArgsConstructor
@Tag(name = "Admin Settings", description = "Global platform configuration management")
public class AdminSettingController {

  private static final String ROLE_ADMIN = "ADMIN";

  private final SystemSettingService systemSettingService;

  @GetMapping("/citizen-card-email")
  @Operation(
      summary = "Get current citizen card service email setting",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  public ResponseEntity<CitizenCardEmailSettingResponse> getCitizenCardEmailSetting(
      HttpServletRequest request) {
    checkAdminRole(request);
    return ResponseEntity.ok(systemSettingService.getCitizenCardEmailSetting());
  }

  @PutMapping("/citizen-card-email")
  @Operation(
      summary = "Update citizen card service email setting",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  public ResponseEntity<CitizenCardEmailSettingResponse> updateCitizenCardEmailSetting(
      @Valid @RequestBody CitizenCardEmailSettingUpdateRequest requestBody,
      HttpServletRequest request) {
    checkAdminRole(request);
    String adminEmail = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_EMAIL);
    return ResponseEntity.ok(
        systemSettingService.updateCitizenCardEmailSetting(requestBody.email(), adminEmail));
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
