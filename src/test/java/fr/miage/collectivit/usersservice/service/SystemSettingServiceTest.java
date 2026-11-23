package fr.miage.collectivit.usersservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.dto.CitizenCardEmailSettingResponse;
import fr.miage.collectivit.usersservice.entity.SystemSetting;
import fr.miage.collectivit.usersservice.repository.SystemSettingRepository;
import fr.miage.collectivit.usersservice.service.impl.SystemSettingServiceImpl;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

  private static final String DEFAULT_EMAIL = "citoyen-numerique@shop-loc.fr";

  @Mock private SystemSettingRepository settingRepository;

  private SystemSettingService systemSettingService;

  @BeforeEach
  void setUp() {
    systemSettingService = new SystemSettingServiceImpl(settingRepository, DEFAULT_EMAIL);
  }

  @Test
  @DisplayName("getCitizenCardServiceEmail - should return default when setting is not in database")
  void shouldReturnDefaultWhenSettingNotFound() {
    when(settingRepository.findById(SystemSettingService.SETTING_CITIZEN_CARD_SERVICE_EMAIL))
        .thenReturn(Optional.empty());

    String email = systemSettingService.getCitizenCardServiceEmail();

    assertThat(email).isEqualTo(DEFAULT_EMAIL);
  }

  @Test
  @DisplayName(
      "getCitizenCardServiceEmail - should return persisted email when present in database")
  void shouldReturnPersistedEmail() {
    SystemSetting setting =
        SystemSetting.builder()
            .key(SystemSettingService.SETTING_CITIZEN_CARD_SERVICE_EMAIL)
            .value("service-cartes@mairie.fr")
            .build();
    when(settingRepository.findById(SystemSettingService.SETTING_CITIZEN_CARD_SERVICE_EMAIL))
        .thenReturn(Optional.of(setting));

    String email = systemSettingService.getCitizenCardServiceEmail();

    assertThat(email).isEqualTo("service-cartes@mairie.fr");
  }

  @Test
  @DisplayName("getCitizenCardEmailSetting - should report customized status accurately")
  void shouldReturnDetailedSettingResponse() {
    Instant now = Instant.now();
    SystemSetting setting =
        SystemSetting.builder()
            .key(SystemSettingService.SETTING_CITIZEN_CARD_SERVICE_EMAIL)
            .value("custom@ville.fr")
            .updatedAt(now)
            .updatedBy("admin@ville.fr")
            .build();
    when(settingRepository.findById(SystemSettingService.SETTING_CITIZEN_CARD_SERVICE_EMAIL))
        .thenReturn(Optional.of(setting));

    CitizenCardEmailSettingResponse response = systemSettingService.getCitizenCardEmailSetting();

    assertThat(response.email()).isEqualTo("custom@ville.fr");
    assertThat(response.defaultEmail()).isEqualTo(DEFAULT_EMAIL);
    assertThat(response.isCustomized()).isTrue();
    assertThat(response.updatedAt()).isEqualTo(now);
    assertThat(response.updatedBy()).isEqualTo("admin@ville.fr");
  }

  @Test
  @DisplayName("updateCitizenCardEmailSetting - should save valid email and return response")
  void shouldUpdateCitizenCardEmailSuccessfully() {
    when(settingRepository.findById(SystemSettingService.SETTING_CITIZEN_CARD_SERVICE_EMAIL))
        .thenReturn(Optional.empty());
    when(settingRepository.save(any(SystemSetting.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CitizenCardEmailSettingResponse response =
        systemSettingService.updateCitizenCardEmailSetting(
            "Service-Citoyen@Ville-Lille.FR", "admin@ville-lille.fr");

    assertThat(response.email()).isEqualTo("service-citoyen@ville-lille.fr");
    assertThat(response.isCustomized()).isTrue();
    assertThat(response.updatedBy()).isEqualTo("admin@ville-lille.fr");
    verify(settingRepository).save(any(SystemSetting.class));
  }

  @Test
  @DisplayName("updateCitizenCardEmailSetting - should throw on blank or invalid email")
  void shouldRejectInvalidEmailFormat() {
    assertThatThrownBy(
            () -> systemSettingService.updateCitizenCardEmailSetting("", "admin@ville.fr"))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(
            () ->
                systemSettingService.updateCitizenCardEmailSetting(
                    "not-an-email", "admin@ville.fr"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
