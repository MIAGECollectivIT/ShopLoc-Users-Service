package fr.miage.collectivit.usersservice.service.mail;

import fr.miage.collectivit.usersservice.util.QrCodeService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LawOfDemeter", "PMD.UseObjectForClearerAPI"})
public class PhysicalCardEmailService {

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final QrCodeService qrCodeService;
  private final String fromEmail;
  private final String defaultRecipientEmail;

  public PhysicalCardEmailService(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      QrCodeService qrCodeService,
      @Value("${spring.mail.from:contact@shop-loc.fr}") String fromEmail,
      @Value("${application.mail.citizen-card-service-email:citoyen-numerique@shop-loc.fr}")
          String defaultRecipientEmail) {
    this.mailSenderProvider = mailSenderProvider;
    this.qrCodeService = qrCodeService;
    this.fromEmail = fromEmail;
    this.defaultRecipientEmail = defaultRecipientEmail;
  }

  public String getDefaultRecipientEmail() {
    return defaultRecipientEmail;
  }

  /**
   * Sends the physical card creation notification email to the Citoyen Numérique service.
   *
   * @param recipientEmail the email address of the Citoyen Numérique service
   * @param firstName user's first name
   * @param lastName user's last name
   * @param userEmail user's registered email
   * @param clientCode unique loyalty/card client code (e.g. C123456)
   * @param pickupLocation chosen physical pickup location (e.g. Mairie centrale, Commerce
   *     partenaire)
   * @throws MessagingException if SMTP transmission fails
   */
  public void sendPhysicalCardRequestEmail(
      String recipientEmail,
      String firstName,
      String lastName,
      String userEmail,
      String clientCode,
      String pickupLocation)
      throws MessagingException {

    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      log.warn(
          "JavaMailSender is not configured. Skipping email dispatch for client code {}",
          clientCode);
      return;
    }

    String effectiveRecipient =
        (recipientEmail != null && !recipientEmail.isBlank())
            ? recipientEmail.trim()
            : defaultRecipientEmail;

    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper =
        new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

    helper.setFrom(fromEmail);
    helper.setTo(effectiveRecipient);
    helper.setSubject(
        String.format(
            "[ShopLoc - Citoyen Numérique] Nouvelle demande de carte physique - %s %s (%s)",
            firstName, lastName, clientCode));

    byte[] qrCodeBytes = qrCodeService.generateQrCodePng(clientCode);

    String htmlContent =
        buildHtmlTemplate(firstName, lastName, userEmail, clientCode, pickupLocation);
    String textContent =
        buildTextContent(firstName, lastName, userEmail, clientCode, pickupLocation);

    helper.setText(textContent, htmlContent);
    helper.addInline("qrcodeImage", new ByteArrayResource(qrCodeBytes), "image/png");

    mailSender.send(mimeMessage);
    log.info(
        "Physical card request email dispatched successfully to {} for citizen {} {} ({})",
        effectiveRecipient,
        firstName,
        lastName,
        clientCode);
  }

  private String buildHtmlTemplate(
      String firstName,
      String lastName,
      String userEmail,
      String clientCode,
      String pickupLocation) {
    return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8fafc; color: #1e293b; margin: 0; padding: 24px; }
            .card { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
            .header { background: linear-gradient(135deg, #0ea5e9, #0284c7); padding: 24px; color: #ffffff; text-align: center; }
            .header h1 { margin: 0; font-size: 20px; font-weight: 700; letter-spacing: -0.025em; }
            .header p { margin: 6px 0 0; opacity: 0.9; font-size: 13px; }
            .content { padding: 24px; }
            .intro { font-size: 14px; line-height: 1.6; margin-bottom: 20px; }
            .badge { display: inline-block; padding: 4px 10px; background: #e0f2fe; color: #0369a1; border-radius: 9999px; font-size: 12px; font-weight: 600; }
            .details-box { background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; padding: 16px; margin: 20px 0; }
            .detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #cbd5e1; font-size: 13px; }
            .detail-row:last-child { border-bottom: none; }
            .detail-label { color: #64748b; font-weight: 500; }
            .detail-value { font-weight: 600; color: #0f172a; text-align: right; }
            .pickup-box { background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 8px; padding: 14px; margin: 20px 0; text-align: center; }
            .pickup-box .title { font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: #047857; font-weight: 700; }
            .pickup-box .location { font-size: 16px; color: #065f46; font-weight: 700; margin-top: 4px; }
            .qr-section { text-align: center; margin: 24px 0; padding: 16px; background: #f1f5f9; border-radius: 8px; }
            .qr-section img { width: 180px; height: 180px; border-radius: 8px; background: #ffffff; padding: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
            .qr-caption { font-size: 12px; color: #64748b; margin-top: 8px; font-family: monospace; font-weight: bold; }
            .footer { padding: 16px 24px; background: #f8fafc; border-top: 1px solid #e2e8f0; text-align: center; font-size: 11px; color: #94a3b8; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="header">
              <h1>ShopLoc — Service Citoyen Numérique</h1>
              <p>Demande d'impression et d'attribution de carte physique</p>
            </div>
            <div class="content">
              <p class="intro">
                Bonjour,<br><br>
                Un nouvel utilisateur s'est inscrit sur la plateforme <strong>ShopLoc</strong> et a formulé une demande pour obtenir sa <strong>carte physique d'adhérent</strong>.
              </p>

              <div class="pickup-box">
                <div class="title">Point de retrait sélectionné</div>
                <div class="location">%s</div>
              </div>

              <div class="details-box">
                <div class="detail-row">
                  <span class="detail-label">Citoyen adhérent</span>
                  <span class="detail-value">%s %s</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">Adresse email</span>
                  <span class="detail-value">%s</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">Code d'adhérent (Identifiant carte)</span>
                  <span class="detail-value" style="font-family: monospace; color: #0284c7;">%s</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">Statut initial</span>
                  <span class="detail-value"><span class="badge">À produire</span></span>
                </div>
              </div>

              <div class="qr-section">
                <p style="margin: 0 0 10px; font-size: 12px; font-weight: 600; color: #475569;">QR Code d'identification adhérent pour impression :</p>
                <img src="cid:qrcodeImage" alt="QR Code Adhérent">
                <div class="qr-caption">%s</div>
              </div>

              <p style="font-size: 12px; color: #64748b; line-height: 1.5;">
                Merci de procéder à l'encodage et à l'acheminement de la carte vers le point de retrait désigné.
              </p>
            </div>
            <div class="footer">
              Cet email automatique est envoyé par le système ShopLoc lors de la finalisation d'inscription citoyenne.
            </div>
          </div>
        </body>
        </html>
        """
        .formatted(pickupLocation, firstName, lastName, userEmail, clientCode, clientCode);
  }

  private String buildTextContent(
      String firstName,
      String lastName,
      String userEmail,
      String clientCode,
      String pickupLocation) {
    return """
        [ShopLoc — Service Citoyen Numérique]
        Demande de création de carte physique d'adhérent

        Nouvelle inscription enregistrée :
        - Nom & Prénom : %s %s
        - Email : %s
        - Code d'adhérent : %s
        - Lieu de retrait : %s

        Merci de préparer la carte physique et de l'acheminer au point de retrait sélectionné.
        """
        .formatted(firstName, lastName, userEmail, clientCode, pickupLocation);
  }
}
