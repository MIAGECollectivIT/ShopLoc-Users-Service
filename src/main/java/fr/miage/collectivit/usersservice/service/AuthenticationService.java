package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.dto.AuthResponse;
import fr.miage.collectivit.usersservice.dto.LoginRequest;
import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserResponse;

public interface AuthenticationService {

  UserResponse register(UserRegistrationRequest request);

  AuthResponse login(LoginRequest request);
}
