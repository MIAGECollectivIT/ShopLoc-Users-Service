package fr.miage.collectivit.usersservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.miage.collectivit.usersservice.dto.AdminAddStoreRequest;
import fr.miage.collectivit.usersservice.dto.AdminPartnerUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PartnerDetailResponse;
import fr.miage.collectivit.usersservice.dto.PartnerResponse;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.GlobalExceptionHandler;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.AdminPartnerService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminPartnerControllerTest {

  @Mock private AdminPartnerService adminPartnerService;

  @InjectMocks private AdminPartnerController adminPartnerController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(adminPartnerController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @Test
  void shouldReturn403WhenListingPartnersWithoutAdminRole() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/partners").requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturn403WhenListingPartnersWithNoRole() throws Exception {
    mockMvc.perform(get("/api/v1/admin/partners")).andExpect(status().isForbidden());
  }

  @Test
  void shouldListPartnersWhenAdminRoleProvided() throws Exception {
    PartnerResponse partner =
        PartnerResponse.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .name("Boulangerie Paul")
            .email("paul@example.com")
            .companyName("Boulangerie Paul")
            .role("MERCHANT")
            .existingStoresCount(1)
            .build();

    when(adminPartnerService.getPartners(null, 20)).thenReturn(List.of(partner));

    mockMvc
        .perform(
            get("/api/v1/admin/partners").requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].email").value("paul@example.com"))
        .andExpect(jsonPath("$[0].existingStoresCount").value(1));
  }

  @Test
  void shouldFilterPartnersByQueryWhenAdmin() throws Exception {
    when(adminPartnerService.getPartners("boulangerie", 10)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/v1/admin/partners")
                .param("query", "boulangerie")
                .param("limit", "10")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    verify(adminPartnerService).getPartners("boulangerie", 10);
  }

  @Test
  void shouldReturn403WhenGettingPartnerDetailsWithoutAdminRole() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/partners/1")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "MERCHANT"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldGetPartnerDetailsWhenAdmin() throws Exception {
    UUID publicId = UUID.randomUUID();
    PartnerDetailResponse detail =
        PartnerDetailResponse.builder()
            .id(1L)
            .publicId(publicId)
            .email("paul@example.com")
            .role(UserRole.MERCHANT)
            .firstName("Paul")
            .lastName("Boulanger")
            .birthDate(LocalDate.of(1985, 5, 20))
            .companyName("Boulangerie Paul")
            .storeIds(List.of(10L, 20L))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(adminPartnerService.getPartnerDetails("1")).thenReturn(detail);

    mockMvc
        .perform(
            get("/api/v1/admin/partners/1").requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("paul@example.com"))
        .andExpect(jsonPath("$.firstName").value("Paul"))
        .andExpect(jsonPath("$.lastName").value("Boulanger"))
        .andExpect(jsonPath("$.storeIds[0]").value(10))
        .andExpect(jsonPath("$.storeIds[1]").value(20));
  }

  @Test
  void shouldReturn404WhenPartnerNotFound() throws Exception {
    when(adminPartnerService.getPartnerDetails("999"))
        .thenThrow(new UserNotFoundException("Partner not found"));

    mockMvc
        .perform(
            get("/api/v1/admin/partners/999")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn403WhenUpdatingPartnerWithoutAdminRole() throws Exception {
    AdminPartnerUpdateRequest request =
        AdminPartnerUpdateRequest.builder()
            .firstName("Pierre")
            .lastName("Martin")
            .birthDate(LocalDate.of(1980, 1, 1))
            .build();

    mockMvc
        .perform(
            put("/api/v1/admin/partners/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "MERCHANT"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldUpdatePartnerWhenValidPayloadAndAdmin() throws Exception {
    AdminPartnerUpdateRequest request =
        AdminPartnerUpdateRequest.builder()
            .firstName("Pierre")
            .lastName("Martin")
            .birthDate(LocalDate.of(1980, 1, 1))
            .build();

    PartnerDetailResponse response =
        PartnerDetailResponse.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .email("pierre@example.com")
            .role(UserRole.MERCHANT)
            .firstName("Pierre")
            .lastName("Martin")
            .birthDate(LocalDate.of(1980, 1, 1))
            .companyName("Boutique Martin")
            .storeIds(List.of())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(adminPartnerService.updatePartner(eq("1"), any(AdminPartnerUpdateRequest.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            put("/api/v1/admin/partners/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Pierre"))
        .andExpect(jsonPath("$.lastName").value("Martin"));
  }

  @Test
  void shouldReturn400WhenUpdatePayloadIsInvalid() throws Exception {
    AdminPartnerUpdateRequest invalidRequest =
        AdminPartnerUpdateRequest.builder()
            .firstName("")
            .lastName("Martin")
            .birthDate(LocalDate.now().plusDays(1))
            .build();

    mockMvc
        .perform(
            put("/api/v1/admin/partners/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldAddStoreToPartnerWhenAdmin() throws Exception {
    AdminAddStoreRequest request = new AdminAddStoreRequest(42L);
    doNothing()
        .when(adminPartnerService)
        .addStoreToPartner(eq("1"), any(AdminAddStoreRequest.class));

    mockMvc
        .perform(
            post("/api/v1/admin/partners/1/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isCreated());

    verify(adminPartnerService).addStoreToPartner(eq("1"), any(AdminAddStoreRequest.class));
  }

  @Test
  void shouldReturn400WhenAddingStoreWithNullStoreId() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/partners/1/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeId\": null}")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isBadRequest());
  }
}
