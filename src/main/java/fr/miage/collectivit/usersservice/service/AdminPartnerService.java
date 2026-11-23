package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.dto.AdminAddStoreRequest;
import fr.miage.collectivit.usersservice.dto.AdminPartnerUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PartnerDetailResponse;
import fr.miage.collectivit.usersservice.dto.PartnerResponse;
import java.util.List;
import java.util.UUID;

public interface AdminPartnerService {

  List<PartnerResponse> getPartners(String query, int limit);

  PartnerDetailResponse getPartnerDetails(UUID id);

  PartnerDetailResponse updatePartner(UUID id, AdminPartnerUpdateRequest request);

  void addStoreToPartner(UUID id, AdminAddStoreRequest request);

  void removeStoreFromPartner(UUID id, UUID storeUid);
}
