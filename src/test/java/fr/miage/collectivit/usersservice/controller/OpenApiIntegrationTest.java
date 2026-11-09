package fr.miage.collectivit.usersservice.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class OpenApiIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  @DisplayName("Swagger UI static page is accessible on /swagger-ui/index.html")
  void shouldServeSwaggerUiIndexHtml() throws Exception {
    mockMvc
        .perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("swagger-ui")));
  }

  @Test
  @DisplayName("Swagger UI and OpenAPI adapt seamlessly when requested on custom port (e.g., 8081)")
  void shouldSupportCustomPortSuchAs8081() throws Exception {
    mockMvc
        .perform(
            get("/v3/api-docs").header("Host", "localhost:8081").header("X-Forwarded-Port", "8081"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.servers[0].url").value("/"))
        .andExpect(jsonPath("$.servers[1].url").value("/api"));

    mockMvc
        .perform(
            get("/swagger-ui/index.html")
                .header("Host", "localhost:8081")
                .header("X-Forwarded-Port", "8081"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("swagger-ui")));
  }

  @Test
  @DisplayName("OpenAPI JSON specification is valid, non-duplicated, and properly configured")
  void shouldGenerateValidOpenApiSpecification() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        // Verify servers configuration
        .andExpect(jsonPath("$.servers[0].url").value("/"))
        .andExpect(jsonPath("$.servers[0].description").value("Direct / Local (Default)"))
        .andExpect(jsonPath("$.servers[1].url").value("/api"))
        .andExpect(jsonPath("$.servers[1].description").value("Traefik API Gateway (WAN)"))
        // Verify canonical non-duplicated paths
        .andExpect(jsonPath("$.paths['/v1/users/login']").exists())
        .andExpect(jsonPath("$.paths['/v1/users/status']").exists())
        .andExpect(jsonPath("$.paths['/v1/users/me']").exists())
        .andExpect(jsonPath("$.paths['/v1/users/{userId}']").exists())
        .andExpect(jsonPath("$.paths['/v1/users/loyalty-cards/{cardNumber}']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/users/me']").doesNotExist())
        // Verify DTO schema examples
        .andExpect(
            jsonPath("$.components.schemas.LoginRequest.properties.email.example")
                .value("alice.dupont@example.com"))
        .andExpect(
            jsonPath("$.components.schemas.LoginRequest.properties.password.example")
                .value("SecurePass123!"))
        .andExpect(
            jsonPath("$.components.schemas.VehicleRequest.properties.licensePlate.example")
                .value("AA-123-BB"))
        .andExpect(
            jsonPath("$.components.schemas.UserResponse.properties.clientCode.example")
                .value("CL12345"))
        .andExpect(
            jsonPath("$.components.schemas.UserResponse.properties.pickupLocation.example")
                .value("Mairie de Lille"))
        // Verify public operations do not have mandatory security
        .andExpect(jsonPath("$.paths['/v1/users/status'].get.security").doesNotExist());
  }

  @Test
  @DisplayName("Public endpoints are accessible both via direct /v1 and gateway /api/v1 paths")
  void shouldSupportBothDirectAndGatewayPrefixForPublicEndpoints() throws Exception {
    mockMvc
        .perform(get("/v1/users/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));

    mockMvc
        .perform(get("/api/v1/users/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));

    mockMvc.perform(get("/v1/auth/public-key")).andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/auth/public-key")).andExpect(status().isOk());

    mockMvc.perform(get("/.well-known/jwks.json")).andExpect(status().isOk());

    mockMvc.perform(get("/api/.well-known/jwks.json")).andExpect(status().isOk());
  }
}
