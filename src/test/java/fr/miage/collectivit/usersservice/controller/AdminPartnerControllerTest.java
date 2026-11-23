package fr.miage.collectivit.usersservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    UUID publicId = UUID.randomUUID();
    PartnerResponse partner =
        PartnerResponse.builder()
            .publicId(publicId)
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
        .andExpect(jsonPath("$[0].id").doesNotExist())
        .andExpect(jsonPath("$[0].publicId").value(publicId.toString()))
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
            get("/api/v1/admin/partners/" + UUID.randomUUID())
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "MERCHANT"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldGetPartnerDetailsWhenAdmin() throws Exception {
    UUID publicId = UUID.randomUUID();
    UUID storeUid1 = UUID.randomUUID();
    UUID storeUid2 = UUID.randomUUID();
    PartnerDetailResponse detail =
        PartnerDetailResponse.builder()
            .publicId(publicId)
            .email("paul@example.com")
            .role(UserRole.MERCHANT)
            .firstName("Paul")
            .lastName("Boulanger")
            .birthDate(LocalDate.of(1985, 5, 20))
            .companyName("Boulangerie Paul")
            .storeUids(List.of(storeUid1, storeUid2))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(adminPartnerService.getPartnerDetails(publicId)).thenReturn(detail);

    mockMvc
        .perform(
            get("/api/v1/admin/partners/" + publicId)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.publicId").value(publicId.toString()))
        .andExpect(jsonPath("$.email").value("paul@example.com"))
        .andExpect(jsonPath("$.firstName").value("Paul"))
        .andExpect(jsonPath("$.lastName").value("Boulanger"))
        .andExpect(jsonPath("$.storeUids[0]").value(storeUid1.toString()))
        .andExpect(jsonPath("$.storeUids[1]").value(storeUid2.toString()));
  }

  @Test
  void shouldReturn400WhenGettingPartnerDetailsWithNonUuidId() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/partners/1").requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn404WhenPartnerNotFound() throws Exception {
    UUID unknownId = UUID.randomUUID();
    when(adminPartnerService.getPartnerDetails(unknownId))
        .thenThrow(new UserNotFoundException("Partner not found"));

    mockMvc
        .perform(
            get("/api/v1/admin/partners/" + unknownId)
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
            put("/api/v1/admin/partners/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "MERCHANT"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldUpdatePartnerWhenValidPayloadAndAdmin() throws Exception {
    UUID publicId = UUID.randomUUID();
    AdminPartnerUpdateRequest request =
        AdminPartnerUpdateRequest.builder()
            .firstName("Pierre")
            .lastName("Martin")
            .birthDate(LocalDate.of(1980, 1, 1))
            .build();

    PartnerDetailResponse response =
        PartnerDetailResponse.builder()
            .publicId(publicId)
            .email("pierre@example.com")
            .role(UserRole.MERCHANT)
            .firstName("Pierre")
            .lastName("Martin")
            .birthDate(LocalDate.of(1980, 1, 1))
            .companyName("Boutique Martin")
            .storeUids(List.of())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(adminPartnerService.updatePartner(eq(publicId), any(AdminPartnerUpdateRequest.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            put("/api/v1/admin/partners/" + publicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.publicId").value(publicId.toString()))
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
            put("/api/v1/admin/partners/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldAddStoreToPartnerWhenAdmin() throws Exception {
    UUID partnerId = UUID.randomUUID();
    UUID storeUid = UUID.randomUUID();
    AdminAddStoreRequest request = new AdminAddStoreRequest(storeUid);
    doNothing()
        .when(adminPartnerService)
        .addStoreToPartner(eq(partnerId), any(AdminAddStoreRequest.class));

    mockMvc
        .perform(
            post("/api/v1/admin/partners/" + partnerId + "/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isCreated());

    verify(adminPartnerService).addStoreToPartner(eq(partnerId), any(AdminAddStoreRequest.class));
  }

  @Test
  void shouldReturn400WhenAddingStoreWithNullStoreUid() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/partners/" + UUID.randomUUID() + "/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeUid\": null}")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRemoveStoreFromPartnerWhenAdmin() throws Exception {
    UUID partnerId = UUID.randomUUID();
    UUID storeUid = UUID.randomUUID();
    doNothing().when(adminPartnerService).removeStoreFromPartner(partnerId, storeUid);

    mockMvc
        .perform(
            delete("/api/v1/admin/partners/" + partnerId + "/stores/" + storeUid)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isNoContent());

    verify(adminPartnerService).removeStoreFromPartner(partnerId, storeUid);
  }

  @Test
  void shouldReturn400WhenRemovingStoreWithNonUuidStoreUid() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/admin/partners/" + UUID.randomUUID() + "/stores/42")
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn403WhenRemovingStoreWithoutAdminRole() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/admin/partners/" + UUID.randomUUID() + "/stores/" + UUID.randomUUID())
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "MERCHANT"))
        .andExpect(status().isForbidden());
  }
}
