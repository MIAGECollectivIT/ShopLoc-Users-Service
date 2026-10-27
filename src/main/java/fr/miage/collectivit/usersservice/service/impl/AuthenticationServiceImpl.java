package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.EmailAlreadyExistsException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.service.AuthenticationService;
import fr.miage.collectivit.usersservice.util.ClientCodeGenerator;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ClientCodeGenerator clientCodeGenerator;

  @Override
  @Transactional
  public UserResponse register(UserRegistrationRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyExistsException(request.email());
    }

    String clientCode = generateUniqueClientCode();
    String hashedPassword = passwordEncoder.encode(request.password());

    User user =
        User.builder()
            .firstName(request.firstName())
            .lastName(request.lastName())
            .birthDate(request.birthDate())
            .email(request.email())
            .passwordHash(hashedPassword)
            .role(UserRole.CLIENT)
            .clientCode(clientCode)
            .pickupLocation(request.pickupLocation())
            .licensePlates(
                request.licensePlates() != null
                    ? new ArrayList<>(request.licensePlates())
                    : new ArrayList<>())
            .build();

    User savedUser = userRepository.save(user);
    return UserResponse.fromEntity(savedUser);
  }

  private String generateUniqueClientCode() {
    String code;
    do {
      code = clientCodeGenerator.generateCode();
    } while (userRepository.existsByClientCode(code));
    return code;
  }
}
