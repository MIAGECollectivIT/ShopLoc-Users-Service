package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.dto.UserResponse;

public interface LoyaltyCardIdentificationService {

  UserResponse identifyByCardNumber(String cardNumber);
}
