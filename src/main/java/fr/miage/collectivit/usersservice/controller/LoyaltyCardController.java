package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.config.OpenApiConfig;
import fr.miage.collectivit.usersservice.dto.CustomerIdentificationResponse;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.ForbiddenException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.LoyaltyCardIdentificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller exposing customer loyalty card identification operations for merchants. */
@RestController
@RequestMapping({"/v1/users", "/api/v1/users"})
@RequiredArgsConstructor
@Tag(
    name = "Loyalty Card",
    description = "Operations related to customer loyalty card identification")
public class LoyaltyCardController {

  private static final String FORBIDDEN_MESSAGE =
      "Access restricted to partner merchants and administrators";
  private static final Pattern CLIENT_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{7}$");

  private final LoyaltyCardIdentificationService loyaltyCardIdentificationService;

  @GetMapping("/loyalty-cards/{cardNumber}")
  @Operation(
      summary = "Identify customer by loyalty card number or client code",
      description =
          "Finds customer profile matching the given 7-character loyalty code, returning masked sensitive data. Accessible to partner merchants and administrators.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Customer loyalty account found and recognized",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerIdentificationResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid client code format (must be 7 alphanumeric characters)",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid authentication token",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Forbidden - Operation restricted to partner merchants and administrators",
        content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Customer loyalty code not found",
        content = @Content)
  })
  public ResponseEntity<CustomerIdentificationResponse> identifyByCardNumber(
      @Parameter(
              description = "7-character alphanumeric loyalty client code",
              example = "k7P9xQ2",
              required = true)
          @PathVariable
          String cardNumber,
      HttpServletRequest request) {
    checkMerchantOrAdminRole(request);

    if (cardNumber == null || !CLIENT_CODE_PATTERN.matcher(cardNumber.trim()).matches()) {
      throw new IllegalArgumentException(
          "Client card code must be exactly 7 alphanumeric characters");
    }

    return ResponseEntity.ok(
        loyaltyCardIdentificationService.identifyByCardNumber(cardNumber.trim()));
  }

  private void checkMerchantOrAdminRole(HttpServletRequest request) {
    if (request == null) {
      throw new ForbiddenException(FORBIDDEN_MESSAGE);
    }
    String role = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);
    if (!UserRole.MERCHANT.name().equalsIgnoreCase(role)
        && !UserRole.ADMIN.name().equalsIgnoreCase(role)) {
      throw new ForbiddenException(FORBIDDEN_MESSAGE);
    }
  }
}
