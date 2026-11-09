package fr.miage.collectivit.usersservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller exposing users service status and health information with OpenAPI documentation. */
@RestController
@RequestMapping({"/v1/users", "/api/v1/users"})
@Tag(name = "Users Status", description = "Operations related to users service status")
public class UsersStatusController {

  @GetMapping("/status")
  @Operation(
      summary = "Check service status",
      description = "Returns the current running status of the users service",
      security = {})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is active and operational",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "StatusResponseExample",
                            value = "{\"service\": \"users-service\", \"status\": \"UP\"}")))
      })
  public ResponseEntity<Map<String, String>> getStatus() {
    return ResponseEntity.ok(Map.of("service", "users-service", "status", "UP"));
  }
}
