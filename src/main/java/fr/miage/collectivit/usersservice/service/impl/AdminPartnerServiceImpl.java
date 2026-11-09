package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.dto.AdminAddStoreRequest;
import fr.miage.collectivit.usersservice.dto.AdminPartnerUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PartnerDetailResponse;
import fr.miage.collectivit.usersservice.dto.PartnerResponse;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.entity.UserStore;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.repository.UserStoreRepository;
import fr.miage.collectivit.usersservice.service.AdminPartnerService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPartnerServiceImpl implements AdminPartnerService {

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;

  private final UserRepository userRepository;
  private final UserStoreRepository userStoreRepository;

  @Override
  @Transactional(readOnly = true)
  public List<PartnerResponse> getPartners(String query, int limit) {
    int max = clampLimit(limit);
    String q = query != null ? query.trim() : "";

    List<User> merchants =
        userRepository.searchByRoleAndQuery(
            UserRole.MERCHANT, q, PageRequest.of(0, max, Sort.by("id").ascending()));

    if (merchants.isEmpty()) {
      return List.of();
    }

    List<Long> merchantIds = merchants.stream().map(User::getId).toList();
    Map<Long, Integer> storeCounts = fetchStoreCounts(merchantIds);

    return merchants.stream()
        .map(u -> mapToPartnerResponse(u, storeCounts.getOrDefault(u.getId(), 0)))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PartnerDetailResponse getPartnerDetails(String identifier) {
    User merchant = resolveMerchant(identifier);
    List<Long> storeIds = fetchStoreIds(merchant.getId());
    return mapToDetailResponse(merchant, storeIds);
  }

  @Override
  @Transactional
  public PartnerDetailResponse updatePartner(String identifier, AdminPartnerUpdateRequest request) {
    User merchant = resolveMerchant(identifier);

    merchant.setFirstName(request.firstName().trim());
    merchant.setLastName(request.lastName().trim());
    merchant.setBirthDate(request.birthDate());

    User saved = userRepository.save(merchant);
    List<Long> storeIds = fetchStoreIds(saved.getId());
    return mapToDetailResponse(saved, storeIds);
  }

  @Override
  @Transactional
  public void addStoreToPartner(String identifier, AdminAddStoreRequest request) {
    User merchant = resolveMerchant(identifier);
    Long storeId = request.storeId();

    if (userStoreRepository.existsByUserIdAndStoreId(merchant.getId(), storeId)) {
      if (log.isDebugEnabled()) {
        log.debug("Store {} is already associated with partner {}", storeId, merchant.getId());
      }
      return;
    }

    UserStore link = UserStore.builder().userId(merchant.getId()).storeId(storeId).build();
    userStoreRepository.save(link);
    if (log.isInfoEnabled()) {
      log.info("Linked store {} to partner {}", storeId, merchant.getId());
    }
  }

  private User resolveMerchant(String identifier) {
    User user = UserIdentityResolver.resolveUser(userRepository, identifier, identifier);
    if (user.getRole() != UserRole.MERCHANT) {
      throw new UserNotFoundException("Partner merchant not found for identifier: " + identifier);
    }
    return user;
  }

  private List<Long> fetchStoreIds(Long userId) {
    return userStoreRepository.findByUserId(userId).stream().map(UserStore::getStoreId).toList();
  }

  private int clampLimit(int limit) {
    return (limit > 0 && limit <= MAX_LIMIT) ? limit : DEFAULT_LIMIT;
  }

  private Map<Long, Integer> fetchStoreCounts(List<Long> merchantIds) {
    Map<Long, Integer> storeCounts = new HashMap<>();
    for (Object[] row : userStoreRepository.countStoresByUserIds(merchantIds)) {
      if (row != null
          && row.length >= 2
          && row[0] instanceof Number uId
          && row[1] instanceof Number count) {
        storeCounts.put(uId.longValue(), count.intValue());
      }
    }
    return storeCounts;
  }

  private PartnerResponse mapToPartnerResponse(User u, int storeCount) {
    String fullName = (u.getFirstName() + " " + u.getLastName()).trim();
    String companyName =
        (u.getPickupLocation() != null && !u.getPickupLocation().isBlank())
            ? u.getPickupLocation()
            : "Boutique " + u.getLastName();
    return PartnerResponse.builder()
        .id(u.getId())
        .publicId(u.getPublicId())
        .name(fullName)
        .email(u.getEmail())
        .companyName(companyName)
        .role(u.getRole().name())
        .existingStoresCount(storeCount)
        .build();
  }

  private PartnerDetailResponse mapToDetailResponse(User u, List<Long> storeIds) {
    String companyName =
        (u.getPickupLocation() != null && !u.getPickupLocation().isBlank())
            ? u.getPickupLocation()
            : "Boutique " + u.getLastName();
    return PartnerDetailResponse.builder()
        .id(u.getId())
        .publicId(u.getPublicId())
        .email(u.getEmail())
        .role(u.getRole())
        .firstName(u.getFirstName())
        .lastName(u.getLastName())
        .birthDate(u.getBirthDate())
        .companyName(companyName)
        .storeIds(storeIds)
        .createdAt(u.getCreatedAt())
        .updatedAt(u.getUpdatedAt())
        .build();
  }
}
