package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.config.OpenApiConfig;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.service.LoyaltyCardIdentificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(
    name = "Loyalty Card",
    description = "Operations related to customer loyalty card identification")
public class LoyaltyCardController {

  private final LoyaltyCardIdentificationService loyaltyCardIdentificationService;

  @GetMapping("/loyalty-cards/{cardNumber}")
  @Operation(
      summary = "Identify user by loyalty card number",
      description = "Finds user profile matching the given loyalty card number",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "User profile found"),
    @ApiResponse(responseCode = "404", description = "Card number not found")
  })
  public ResponseEntity<UserResponse> identifyByCardNumber(@PathVariable String cardNumber) {
    return ResponseEntity.ok(loyaltyCardIdentificationService.identifyByCardNumber(cardNumber));
  }
}
