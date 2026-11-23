package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.dto.CitizenCardEmailSettingResponse;
import fr.miage.collectivit.usersservice.entity.SystemSetting;
import fr.miage.collectivit.usersservice.repository.SystemSettingRepository;
import fr.miage.collectivit.usersservice.service.SystemSettingService;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LawOfDemeter"})
public class SystemSettingServiceImpl implements SystemSettingService {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  private final SystemSettingRepository settingRepository;
  private final String defaultRecipientEmail;

  public SystemSettingServiceImpl(
      SystemSettingRepository settingRepository,
      @Value("${application.mail.citizen-card-service-email:citoyen-numerique@shop-loc.fr}")
          String defaultRecipientEmail) {
    this.settingRepository = settingRepository;
    this.defaultRecipientEmail = defaultRecipientEmail;
  }

  @Override
  @Transactional(readOnly = true)
  public String getCitizenCardServiceEmail() {
    return settingRepository
        .findById(SETTING_CITIZEN_CARD_SERVICE_EMAIL)
        .map(SystemSetting::getValue)
        .filter(val -> val != null && !val.isBlank())
        .map(String::trim)
        .orElse(defaultRecipientEmail);
  }

  @Override
  @Transactional(readOnly = true)
  public CitizenCardEmailSettingResponse getCitizenCardEmailSetting() {
    return settingRepository
        .findById(SETTING_CITIZEN_CARD_SERVICE_EMAIL)
        .map(
            setting -> {
              boolean customized =
                  setting.getValue() != null
                      && !setting.getValue().isBlank()
                      && !setting.getValue().trim().equalsIgnoreCase(defaultRecipientEmail);
              return new CitizenCardEmailSettingResponse(
                  setting.getValue() != null ? setting.getValue().trim() : defaultRecipientEmail,
                  defaultRecipientEmail,
                  customized,
                  setting.getUpdatedAt(),
                  setting.getUpdatedBy());
            })
        .orElseGet(
            () ->
                new CitizenCardEmailSettingResponse(
                    defaultRecipientEmail, defaultRecipientEmail, false, null, null));
  }

  @Override
  @Transactional
  public CitizenCardEmailSettingResponse updateCitizenCardEmailSetting(
      String email, String updatedBy) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("L'adresse email ne peut pas être vide");
    }
    String cleanedEmail = email.trim().toLowerCase(Locale.ROOT);
    if (!EMAIL_PATTERN.matcher(cleanedEmail).matches()) {
      throw new IllegalArgumentException("Le format de l'adresse email est invalide : " + email);
    }

    SystemSetting setting =
        settingRepository
            .findById(SETTING_CITIZEN_CARD_SERVICE_EMAIL)
            .orElseGet(
                () -> SystemSetting.builder().key(SETTING_CITIZEN_CARD_SERVICE_EMAIL).build());

    setting.setValue(cleanedEmail);
    setting.setUpdatedAt(Instant.now());
    setting.setUpdatedBy(updatedBy != null && !updatedBy.isBlank() ? updatedBy : "ADMIN");

    SystemSetting saved = settingRepository.save(setting);
    if (log.isInfoEnabled()) {
      log.info(
          "Citizen card service email updated to {} by {}", saved.getValue(), saved.getUpdatedBy());
    }

    boolean isCustomized = !cleanedEmail.equalsIgnoreCase(defaultRecipientEmail);
    return new CitizenCardEmailSettingResponse(
        saved.getValue(),
        defaultRecipientEmail,
        isCustomized,
        saved.getUpdatedAt(),
        saved.getUpdatedBy());
  }
}
