package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.validation.MinAge;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;

@Builder
@Schema(description = "Payload to update partner administrative profile")
public record AdminPartnerUpdateRequest(
    @NotBlank(message = "First name is mandatory")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        @Schema(description = "First name", example = "Jean")
        String firstName,
    @NotBlank(message = "Last name is mandatory")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        @Schema(description = "Last name", example = "Dupont")
        String lastName,
    @NotNull(message = "Birth date is mandatory")
        @Past(message = "Birth date must be in the past")
        @MinAge(value = 18, message = "User must be at least 18 years old")
        @Schema(description = "Birth date", example = "1985-05-15")
        LocalDate birthDate) {}
