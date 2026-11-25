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
import fr.miage.collectivit.usersservice.service.UserRoleTransitionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class AdminPartnerServiceImpl implements AdminPartnerService {

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;

  private final UserRepository userRepository;
  private final UserStoreRepository userStoreRepository;
  private final UserRoleTransitionService userRoleTransitionService;

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
  public PartnerDetailResponse getPartnerDetails(UUID id) {
    User merchant = resolveMerchant(id);
    List<UUID> storeUids = fetchStoreUids(merchant.getId());
    return mapToDetailResponse(merchant, storeUids);
  }

  @Override
  @Transactional
  public PartnerDetailResponse updatePartner(UUID id, AdminPartnerUpdateRequest request) {
    User merchant = resolveMerchant(id);

    merchant.setFirstName(request.firstName().trim());
    merchant.setLastName(request.lastName().trim());
    merchant.setBirthDate(request.birthDate());

    User saved = userRepository.save(merchant);
    List<UUID> storeUids = fetchStoreUids(saved.getId());
    return mapToDetailResponse(saved, storeUids);
  }

  @Override
  @Transactional
  @CacheEvict(value = "userProfiles", key = "#id")
  public void addStoreToPartner(UUID id, AdminAddStoreRequest request) {
    User user = resolveUser(id);
    if (user.getRole() == UserRole.CLIENT) {
      userRoleTransitionService.changeUserRole(user.getId(), UserRole.MERCHANT);
      if (log.isInfoEnabled()) {
        log.info("Promoted user {} from CLIENT to MERCHANT on store assignment", id);
      }
    }

    UUID storeUid = request.storeUid();
    if (userStoreRepository.existsByUserIdAndStoreUid(user.getId(), storeUid)) {
      if (log.isDebugEnabled()) {
        log.debug("Store {} is already associated with partner {}", storeUid, user.getPublicId());
      }
      return;
    }

    UserStore link = UserStore.builder().userId(user.getId()).storeUid(storeUid).build();
    userStoreRepository.save(link);
    if (log.isInfoEnabled()) {
      log.info("Linked store {} to partner {}", storeUid, user.getPublicId());
    }
  }

  @Override
  @Transactional
  @CacheEvict(value = "userProfiles", key = "#id")
  public void removeStoreFromPartner(UUID id, UUID storeUid) {
    User user = resolveUser(id);
    userStoreRepository.deleteByUserIdAndStoreUid(user.getId(), storeUid);
    if (log.isInfoEnabled()) {
      log.info("Removed store {} from partner {}", storeUid, user.getPublicId());
    }

    if (user.getRole() == UserRole.MERCHANT && fetchStoreUids(user.getId()).isEmpty()) {
      userRoleTransitionService.changeUserRole(user.getId(), UserRole.CLIENT);
      if (log.isInfoEnabled()) {
        log.info("Demoted user {} from MERCHANT to CLIENT (no stores remaining)", id);
      }
    }
  }

  private User resolveUser(UUID id) {
    if (id == null) {
      throw new UserNotFoundException("Partner merchant identifier must not be empty");
    }
    return userRepository
        .findByPublicId(id)
        .orElseThrow(
            () -> new UserNotFoundException("Partner merchant not found with publicId: " + id));
  }

  private User resolveMerchant(UUID id) {
    User user = resolveUser(id);
    if (user.getRole() != UserRole.MERCHANT) {
      throw new UserNotFoundException("Partner merchant not found with publicId: " + id);
    }
    return user;
  }

  private List<UUID> fetchStoreUids(Long userId) {
    return userStoreRepository.findByUserId(userId).stream()
        .map(UserStore::getStoreUid)
        .filter(Objects::nonNull)
        .toList();
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
        .publicId(u.getPublicId())
        .name(fullName)
        .email(u.getEmail())
        .companyName(companyName)
        .role(u.getRole().name())
        .existingStoresCount(storeCount)
        .build();
  }

  private PartnerDetailResponse mapToDetailResponse(User u, List<UUID> storeUids) {
    String companyName =
        (u.getPickupLocation() != null && !u.getPickupLocation().isBlank())
            ? u.getPickupLocation()
            : "Boutique " + u.getLastName();
    return PartnerDetailResponse.builder()
        .publicId(u.getPublicId())
        .email(u.getEmail())
        .role(u.getRole())
        .firstName(u.getFirstName())
        .lastName(u.getLastName())
        .birthDate(u.getBirthDate())
        .companyName(companyName)
        .storeUids(storeUids)
        .createdAt(u.getCreatedAt())
        .updatedAt(u.getUpdatedAt())
        .build();
  }
}
