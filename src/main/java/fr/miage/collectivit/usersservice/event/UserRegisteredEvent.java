package fr.miage.collectivit.usersservice.event;

import fr.miage.collectivit.usersservice.entity.User;

public record UserRegisteredEvent(User user, String pickupLocation) {}
