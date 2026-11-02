package fr.miage.collectivit.usersservice.dto;

import lombok.Builder;

@Builder
public record AuthResponse(String token, String tokenType, long expiresIn, UserResponse user) {}
