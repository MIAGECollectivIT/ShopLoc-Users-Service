package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.dto.AdminAddStoreRequest;
import fr.miage.collectivit.usersservice.dto.AdminPartnerUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PartnerDetailResponse;
import fr.miage.collectivit.usersservice.dto.PartnerResponse;
import java.util.List;

public interface AdminPartnerService {

  List<PartnerResponse> getPartners(String query, int limit);

  PartnerDetailResponse getPartnerDetails(String identifier);

  PartnerDetailResponse updatePartner(String identifier, AdminPartnerUpdateRequest request);

  void addStoreToPartner(String identifier, AdminAddStoreRequest request);
}
