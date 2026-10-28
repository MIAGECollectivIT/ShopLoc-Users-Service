package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(
    name = "Authentication",
    description = "Operations related to user registration and authentication")
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  @PostMapping
  @Operation(
      summary = "Register a new user",
      description =
          "Creates a new user account with hashed credentials and a generated client code")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "User successfully registered"),
    @ApiResponse(responseCode = "400", description = "Invalid registration data"),
    @ApiResponse(responseCode = "409", description = "Email already in use")
  })
  public ResponseEntity<UserResponse> register(
      @Valid @RequestBody UserRegistrationRequest request) {
    UserResponse response = authenticationService.register(request);
    URI location = URI.create("/v1/users/" + response.id());
    return ResponseEntity.created(location).body(response);
  }
}
