package fr.miage.collectivit.usersservice.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.usersservice.dto.CitizenCardEmailSettingResponse;
import fr.miage.collectivit.usersservice.dto.CitizenCardEmailSettingUpdateRequest;
import fr.miage.collectivit.usersservice.exception.GlobalExceptionHandler;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.SystemSettingService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminSettingControllerTest {

  @Mock private SystemSettingService systemSettingService;

  @InjectMocks private AdminSettingController adminSettingController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(adminSettingController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("GET /v1/admin/settings/citizen-card-email - Should return setting for ADMIN")
  void shouldReturnCitizenCardEmailSettingForAdmin() throws Exception {
    CitizenCardEmailSettingResponse response =
        new CitizenCardEmailSettingResponse(
            "citoyen@ville.fr",
            "citoyen-numerique@shop-loc.fr",
            true,
            Instant.now(),
            "admin@ville.fr");
    when(systemSettingService.getCitizenCardEmailSetting()).thenReturn(response);

    mockMvc
        .perform(
            get("/v1/admin/settings/citizen-card-email")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("citoyen@ville.fr"))
        .andExpect(jsonPath("$.defaultEmail").value("citoyen-numerique@shop-loc.fr"))
        .andExpect(jsonPath("$.isCustomized").value(true));
  }

  @Test
  @DisplayName("GET /v1/admin/settings/citizen-card-email - Should return 403 for non-ADMIN")
  void shouldRejectNonAdminAccess() throws Exception {
    mockMvc
        .perform(
            get("/v1/admin/settings/citizen-card-email")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PUT /v1/admin/settings/citizen-card-email - Should update email setting for ADMIN")
  void shouldUpdateCitizenCardEmailSettingForAdmin() throws Exception {
    CitizenCardEmailSettingUpdateRequest request =
        new CitizenCardEmailSettingUpdateRequest("nouveau-service@ville.fr");
    CitizenCardEmailSettingResponse response =
        new CitizenCardEmailSettingResponse(
            "nouveau-service@ville.fr",
            "citoyen-numerique@shop-loc.fr",
            true,
            Instant.now(),
            "admin@ville.fr");
    when(systemSettingService.updateCitizenCardEmailSetting(
            eq("nouveau-service@ville.fr"), eq("admin@ville.fr")))
        .thenReturn(response);

    mockMvc
        .perform(
            put("/v1/admin/settings/citizen-card-email")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_EMAIL, "admin@ville.fr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("nouveau-service@ville.fr"))
        .andExpect(jsonPath("$.isCustomized").value(true));

    verify(systemSettingService)
        .updateCitizenCardEmailSetting(eq("nouveau-service@ville.fr"), eq("admin@ville.fr"));
  }

  @Test
  @DisplayName("PUT /v1/admin/settings/citizen-card-email - Should validate email format")
  void shouldValidateEmailFormatOnUpdate() throws Exception {
    CitizenCardEmailSettingUpdateRequest request =
        new CitizenCardEmailSettingUpdateRequest("invalid-email-format");

    mockMvc
        .perform(
            put("/v1/admin/settings/citizen-card-email")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_EMAIL, "admin@ville.fr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
