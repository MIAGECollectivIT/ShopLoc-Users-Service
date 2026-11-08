package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.config.OpenApiConfig;
import fr.miage.collectivit.usersservice.dto.AdminAddStoreRequest;
import fr.miage.collectivit.usersservice.dto.AdminPartnerUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PartnerDetailResponse;
import fr.miage.collectivit.usersservice.dto.PartnerResponse;
import fr.miage.collectivit.usersservice.exception.ForbiddenException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.AdminPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping({"/v1/admin/partners", "/api/v1/admin/partners"})
@RequiredArgsConstructor
@Tag(
    name = "Admin Partners",
    description = "Administrative operations on partner merchant accounts")
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals"})
public class AdminPartnerController {

  private static final String ROLE_ADMIN = "ADMIN";
  private static final String OK_STATUS = "200";
  private static final String CREATED_STATUS = "201";
  private static final String BAD_REQUEST_STATUS = "400";
  private static final String UNAUTHORIZED_STATUS = "401";
  private static final String FORBIDDEN_STATUS = "403";
  private static final String NOT_FOUND_STATUS = "404";

  private final AdminPartnerService adminPartnerService;

  @GetMapping
  @Operation(
      summary = "List partner merchants",
      description = "Lists partner merchant accounts with optional filtering by query or limit",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "List of partners matching criteria",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = PartnerResponse.class)))),
    @ApiResponse(
        responseCode = UNAUTHORIZED_STATUS,
        description = "Unauthorized - Missing or invalid authentication token",
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = "Forbidden - Operation restricted to administrators",
        content = @Content)
  })
  public ResponseEntity<List<PartnerResponse>> getPartners(
      @Parameter(description = "Filter by merchant name or email", example = "boulangerie")
          @RequestParam(value = "query", required = false)
          String query,
      @Parameter(description = "Maximum number of records to return", example = "20")
          @RequestParam(value = "limit", required = false, defaultValue = "20")
          int limit,
      HttpServletRequest request) {
    checkAdminRole(request);
    return ResponseEntity.ok(adminPartnerService.getPartners(query, limit));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get partner detailed profile",
      description = "Retrieves full details of a partner merchant including associated store IDs",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Partner details retrieved successfully",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PartnerDetailResponse.class))),
    @ApiResponse(
        responseCode = UNAUTHORIZED_STATUS,
        description = "Unauthorized - Missing or invalid authentication token",
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = "Forbidden - Operation restricted to administrators",
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = "Partner merchant not found",
        content = @Content)
  })
  public ResponseEntity<PartnerDetailResponse> getPartnerById(
      @Parameter(
              description = "Partner internal ID, public UUID, or email address",
              example = "1",
              required = true)
          @PathVariable("id")
          String id,
      HttpServletRequest request) {
    checkAdminRole(request);
    return ResponseEntity.ok(adminPartnerService.getPartnerDetails(id));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update partner profile",
      description = "Updates administrative identity information for an existing partner merchant",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Partner updated successfully",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PartnerDetailResponse.class))),
    @ApiResponse(
        responseCode = BAD_REQUEST_STATUS,
        description = "Invalid update payload",
        content = @Content),
    @ApiResponse(
        responseCode = UNAUTHORIZED_STATUS,
        description = "Unauthorized - Missing or invalid authentication token",
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = "Forbidden - Operation restricted to administrators",
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = "Partner merchant not found",
        content = @Content)
  })
  public ResponseEntity<PartnerDetailResponse> updatePartner(
      @Parameter(
              description = "Partner internal ID, public UUID, or email address",
              example = "1",
              required = true)
          @PathVariable("id")
          String id,
      @Valid @RequestBody AdminPartnerUpdateRequest requestBody,
      HttpServletRequest request) {
    checkAdminRole(request);
    return ResponseEntity.ok(adminPartnerService.updatePartner(id, requestBody));
  }

  @PostMapping("/{id}/stores")
  @Operation(
      summary = "Add store to partner",
      description = "Associates an existing store with a partner merchant",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = CREATED_STATUS,
        description = "Store associated successfully with partner",
        content = @Content),
    @ApiResponse(
        responseCode = BAD_REQUEST_STATUS,
        description = "Invalid payload or store identifier",
        content = @Content),
    @ApiResponse(
        responseCode = UNAUTHORIZED_STATUS,
        description = "Unauthorized - Missing or invalid authentication token",
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = "Forbidden - Operation restricted to administrators",
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = "Partner merchant not found",
        content = @Content)
  })
  public ResponseEntity<Void> addStoreToPartner(
      @Parameter(
              description = "Partner internal ID, public UUID, or email address",
              example = "1",
              required = true)
          @PathVariable("id")
          String id,
      @Valid @RequestBody AdminAddStoreRequest requestBody,
      HttpServletRequest request) {
    checkAdminRole(request);
    adminPartnerService.addStoreToPartner(id, requestBody);
    return ResponseEntity.status(HttpStatus.CREATED).build();
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
