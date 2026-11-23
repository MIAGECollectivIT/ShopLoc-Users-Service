package fr.miage.collectivit.usersservice.service.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.util.QrCodeService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class PhysicalCardEmailServiceTest {

  @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
  @Mock private JavaMailSender javaMailSender;
  @Mock private QrCodeService qrCodeService;

  private PhysicalCardEmailService physicalCardEmailService;

  @BeforeEach
  void setUp() {
    physicalCardEmailService =
        new PhysicalCardEmailService(
            mailSenderProvider,
            qrCodeService,
            "contact@shop-loc.fr",
            "citoyen-numerique@shop-loc.fr");
  }

  @Test
  @DisplayName("sendPhysicalCardRequestEmail should construct and send MIME message with QR code")
  void shouldSendEmailSuccessfully() throws Exception {
    when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(qrCodeService.generateQrCodePng("C123456")).thenReturn(new byte[] {1, 2, 3});

    physicalCardEmailService.sendPhysicalCardRequestEmail(
        null, "Jean", "Dupont", "jean.dupont@test.fr", "C123456", "Mairie de Lille");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(javaMailSender).send(captor.capture());

    MimeMessage sentMessage = captor.getValue();
    assertThat(sentMessage.getAllRecipients()).hasSize(1);
    assertThat(sentMessage.getAllRecipients()[0].toString())
        .isEqualTo("citoyen-numerique@shop-loc.fr");
    assertThat(sentMessage.getSubject())
        .isEqualTo(
            "[ShopLoc - Citoyen Numérique] Nouvelle demande de carte physique - Jean Dupont (C123456)");
  }

  @Test
  @DisplayName(
      "sendPhysicalCardRequestEmail should skip dispatch when JavaMailSender is unavailable")
  void shouldSkipWhenMailSenderUnavailable() throws Exception {
    when(mailSenderProvider.getIfAvailable()).thenReturn(null);

    physicalCardEmailService.sendPhysicalCardRequestEmail(
        "custom@shop-loc.fr",
        "Jean",
        "Dupont",
        "jean.dupont@test.fr",
        "C123456",
        "Mairie de Lille");

    verify(javaMailSender, never()).send(any(MimeMessage.class));
  }
}
