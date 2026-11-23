package fr.miage.collectivit.usersservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.entity.CardNotificationStatus;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.PhysicalCardNotification;
import fr.miage.collectivit.usersservice.repository.PhysicalCardNotificationRepository;
import fr.miage.collectivit.usersservice.service.impl.PhysicalCardNotificationServiceImpl;
import fr.miage.collectivit.usersservice.service.mail.PhysicalCardEmailService;
import jakarta.mail.MessagingException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhysicalCardNotificationServiceTest {

  @Mock private PhysicalCardNotificationRepository notificationRepository;
  @Mock private PhysicalCardEmailService emailService;
  @Mock private SystemSettingService systemSettingService;

  private PhysicalCardNotificationService notificationService;

  @BeforeEach
  void setUp() {
    notificationService =
        new PhysicalCardNotificationServiceImpl(
            notificationRepository, emailService, systemSettingService);
    lenient()
        .when(systemSettingService.getCitizenCardServiceEmail())
        .thenReturn("citoyen-numerique@shop-loc.fr");
  }

  private Client createTestClient() {
    return Client.builder()
        .id(42L)
        .firstName("Alice")
        .lastName("Martin")
        .birthDate(LocalDate.of(1990, 1, 1))
        .email("alice.martin@test.fr")
        .passwordHash("hashed")
        .clientCode("C987654")
        .pickupLocation("Mairie Annexe")
        .build();
  }

  @Test
  @DisplayName("processRegistrationCardRequest should save and send notification with SENT status")
  void shouldProcessCardRequestSuccessfully() throws Exception {
    Client client = createTestClient();

    when(notificationRepository.save(any(PhysicalCardNotification.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PhysicalCardNotification result =
        notificationService.processRegistrationCardRequest(client, "Mairie Annexe");

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(CardNotificationStatus.SENT);
    assertThat(result.getSentAt()).isNotNull();
    assertThat(result.getErrorMessage()).isNull();
    assertThat(result.getClientCode()).isEqualTo("C987654");
    assertThat(result.getPickupLocation()).isEqualTo("Mairie Annexe");

    verify(emailService)
        .sendPhysicalCardRequestEmail(
            eq("citoyen-numerique@shop-loc.fr"),
            eq("Alice"),
            eq("Martin"),
            eq("alice.martin@test.fr"),
            eq("C987654"),
            eq("Mairie Annexe"));
  }

  @Test
  @DisplayName(
      "processRegistrationCardRequest should mark FAILED when email transmission throws exception")
  void shouldMarkFailedWhenEmailFails() throws Exception {
    Client client = createTestClient();

    when(notificationRepository.save(any(PhysicalCardNotification.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    doThrow(new MessagingException("SMTP server connection timeout"))
        .when(emailService)
        .sendPhysicalCardRequestEmail(any(), any(), any(), any(), any(), any());

    PhysicalCardNotification result =
        notificationService.processRegistrationCardRequest(client, "Mairie Annexe");

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(CardNotificationStatus.FAILED);
    assertThat(result.getErrorMessage()).contains("SMTP server connection timeout");
  }

  @Test
  @DisplayName(
      "processRegistrationCardRequest should return null when user is null or pickupLocation is blank")
  void shouldReturnNullWhenNoPickupLocation() throws Exception {
    Client client = createTestClient();
    client.setPickupLocation(null);

    PhysicalCardNotification result =
        notificationService.processRegistrationCardRequest(client, "   ");

    assertThat(result).isNull();
    verify(notificationRepository, never()).save(any());
  }

  @Test
  @DisplayName("getNotificationsForUser should query repository")
  void shouldGetNotificationsForUser() {
    PhysicalCardNotification notif = PhysicalCardNotification.builder().id(1L).userId(42L).build();
    when(notificationRepository.findByUserId(42L)).thenReturn(List.of(notif));

    List<PhysicalCardNotification> list = notificationService.getNotificationsForUser(42L);
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getId()).isEqualTo(1L);
  }
}
