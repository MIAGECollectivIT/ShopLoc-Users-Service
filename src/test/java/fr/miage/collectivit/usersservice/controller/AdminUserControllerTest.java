package fr.miage.collectivit.usersservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.miage.collectivit.usersservice.dto.AdminUserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PageResponse;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.GlobalExceptionHandler;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.AdminUserService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

  @Mock private AdminUserService adminUserService;

  @InjectMocks private AdminUserController adminUserController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(adminUserController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @Test
  void shouldReturn403WhenListingUsersWithoutAdminRole() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/users").requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturn403WhenListingUsersWithNoRole() throws Exception {
    mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());
  }

  @Test
  void shouldListUsersWhenAdmin() throws Exception {
    UUID publicId = UUID.randomUUID();
    UserResponse user =
        UserResponse.builder()
            .id(publicId)
            .email("alice@example.com")
            .firstName("Alice")
            .lastName("Dupont")
            .role(UserRole.CLIENT)
            .isLocked(false)
            .birthDate(LocalDate.of(1995, 4, 20))
            .clientCode("CL12345")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    PageResponse<UserResponse> pageResponse = PageResponse.of(List.of(user), 0, 20, 1, 1);

    when(adminUserService.getUsers(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(pageResponse);

    mockMvc
        .perform(
            get("/api/v1/admin/users")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN")
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].email").value("alice@example.com"))
        .andExpect(jsonPath("$.content[0].firstName").value("Alice"))
        .andExpect(jsonPath("$.content[0].isLocked").value(false))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void shouldGetUserByIdWhenAdmin() throws Exception {
    UUID publicId = UUID.randomUUID();
    UserResponse user =
        UserResponse.builder()
            .id(publicId)
            .email("bob@example.com")
            .firstName("Bob")
            .lastName("Martin")
            .role(UserRole.MERCHANT)
            .isLocked(false)
            .birthDate(LocalDate.of(1980, 1, 1))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(adminUserService.getUserById(publicId)).thenReturn(user);

    mockMvc
        .perform(
            get("/api/v1/admin/users/" + publicId)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numericId").doesNotExist())
        .andExpect(jsonPath("$.id").value(publicId.toString()))
        .andExpect(jsonPath("$.email").value("bob@example.com"))
        .andExpect(jsonPath("$.role").value("MERCHANT"));
  }

  @Test
  void shouldReturn400WhenUserIdIsNotUuid() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/users/unknown-id")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenUnlockingUserWithNonUuidId() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/users/123/unlock")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn404WhenUserNotFound() throws Exception {
    UUID unknownId = UUID.randomUUID();
    when(adminUserService.getUserById(unknownId))
        .thenThrow(new UserNotFoundException("User not found"));

    mockMvc
        .perform(
            get("/api/v1/admin/users/" + unknownId)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldUpdateUserWhenAdmin() throws Exception {
    UUID publicId = UUID.randomUUID();
    AdminUserUpdateRequest updateRequest =
        AdminUserUpdateRequest.builder()
            .firstName("Alice Modifiée")
            .lastName("Dupont")
            .email("alice.modifiee@example.com")
            .role(UserRole.CLIENT)
            .build();

    UserResponse updatedResponse =
        UserResponse.builder()
            .id(publicId)
            .email("alice.modifiee@example.com")
            .firstName("Alice Modifiée")
            .lastName("Dupont")
            .role(UserRole.CLIENT)
            .build();

    when(adminUserService.updateUser(eq(publicId), any(AdminUserUpdateRequest.class)))
        .thenReturn(updatedResponse);

    mockMvc
        .perform(
            put("/api/v1/admin/users/" + publicId)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Alice Modifiée"))
        .andExpect(jsonPath("$.email").value("alice.modifiee@example.com"));
  }

  @Test
  void shouldUnlockUserWhenAdmin() throws Exception {
    UUID publicId = UUID.randomUUID();
    UserResponse unlocked =
        UserResponse.builder()
            .id(publicId)
            .email("alice@example.com")
            .firstName("Alice")
            .lastName("Dupont")
            .role(UserRole.CLIENT)
            .isLocked(false)
            .build();

    when(adminUserService.unlockUser(publicId)).thenReturn(unlocked);

    mockMvc
        .perform(
            post("/api/v1/admin/users/" + publicId + "/unlock")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isLocked").value(false));
  }
}
