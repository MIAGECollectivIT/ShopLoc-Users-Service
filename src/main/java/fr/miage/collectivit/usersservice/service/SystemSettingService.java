package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.dto.CitizenCardEmailSettingResponse;

public interface SystemSettingService {

  String SETTING_CITIZEN_CARD_SERVICE_EMAIL = "citizen_card_service_email";

  String getCitizenCardServiceEmail();

  CitizenCardEmailSettingResponse getCitizenCardEmailSetting();

  CitizenCardEmailSettingResponse updateCitizenCardEmailSetting(String email, String updatedBy);
}
