package fr.miage.collectivit.usersservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    // BCrypt with strength 12 provides a high work factor suitable for password hashing
    return new BCryptPasswordEncoder(12);
  }
}
