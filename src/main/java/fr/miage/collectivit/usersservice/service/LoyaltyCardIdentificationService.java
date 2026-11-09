package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.dto.CustomerIdentificationResponse;

/** Service for identifying customer accounts from 7-character loyalty card numbers. */
public interface LoyaltyCardIdentificationService {

  CustomerIdentificationResponse identifyByCardNumber(String cardNumber);
}
