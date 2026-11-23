package fr.miage.collectivit.usersservice.dto;

import java.time.Instant;

public record CitizenCardEmailSettingResponse(
    String email, String defaultEmail, boolean isCustomized, Instant updatedAt, String updatedBy) {}
