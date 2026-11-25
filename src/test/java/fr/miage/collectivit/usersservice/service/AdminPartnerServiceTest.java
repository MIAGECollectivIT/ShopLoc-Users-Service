package fr.miage.collectivit.usersservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.dto.AdminAddStoreRequest;
import fr.miage.collectivit.usersservice.dto.AdminPartnerUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PartnerDetailResponse;
import fr.miage.collectivit.usersservice.dto.PartnerResponse;
import fr.miage.collectivit.usersservice.entity.Admin;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.Merchant;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.entity.UserStore;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.repository.UserStoreRepository;
import fr.miage.collectivit.usersservice.service.UserRoleTransitionService;
import fr.miage.collectivit.usersservice.service.impl.AdminPartnerServiceImpl;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminPartnerServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserStoreRepository userStoreRepository;
  @Mock private UserRoleTransitionService userRoleTransitionService;

  @InjectMocks private AdminPartnerServiceImpl adminPartnerService;

  private Merchant sampleMerchant;
  private UUID publicId;

  @BeforeEach
  void setUp() {
    publicId = UUID.randomUUID();
    sampleMerchant =
        Merchant.builder()
            .id(1L)
            .publicId(publicId)
            .email("partner@example.com")
            .firstName("Jean")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1985, 5, 15))
            .role(UserRole.MERCHANT)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  @Test
  void shouldReturnPartnersList() {
    when(userRepository.searchByRoleAndQuery(eq(UserRole.MERCHANT), eq(""), any(Pageable.class)))
        .thenReturn(List.of(sampleMerchant));
    when(userStoreRepository.countStoresByUserIds(List.of(1L)))
        .thenReturn(List.<Object[]>of(new Object[] {1L, 3L}));

    List<PartnerResponse> partners = adminPartnerService.getPartners(null, 20);

    assertThat(partners).hasSize(1);
    assertThat(partners.getFirst().publicId()).isEqualTo(publicId);
    assertThat(partners.getFirst().existingStoresCount()).isEqualTo(3);
  }

  @Test
  void shouldGetPartnerDetailsWhenFound() {
    UUID storeUid1 = UUID.randomUUID();
    UUID storeUid2 = UUID.randomUUID();
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleMerchant));
    when(userStoreRepository.findByUserId(1L))
        .thenReturn(
            List.of(
                UserStore.builder().id(100L).userId(1L).storeUid(storeUid1).build(),
                UserStore.builder().id(101L).userId(1L).storeUid(storeUid2).build()));

    PartnerDetailResponse details = adminPartnerService.getPartnerDetails(publicId);

    assertThat(details.publicId()).isEqualTo(publicId);
    assertThat(details.email()).isEqualTo("partner@example.com");
    assertThat(details.storeUids()).containsExactly(storeUid1, storeUid2);
  }

  @Test
  void shouldThrowWhenPartnerNotFound() {
    UUID unknownId = UUID.randomUUID();
    when(userRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminPartnerService.getPartnerDetails(unknownId))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void shouldUpdatePartnerAdministrativeDetails() {
    AdminPartnerUpdateRequest request =
        AdminPartnerUpdateRequest.builder()
            .firstName("Marc")
            .lastName("Durand")
            .birthDate(LocalDate.of(1982, 3, 10))
            .build();

    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleMerchant));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userStoreRepository.findByUserId(1L)).thenReturn(List.of());

    PartnerDetailResponse updated = adminPartnerService.updatePartner(publicId, request);

    assertThat(updated.firstName()).isEqualTo("Marc");
    assertThat(updated.lastName()).isEqualTo("Durand");
    assertThat(updated.birthDate()).isEqualTo(LocalDate.of(1982, 3, 10));
    verify(userRepository).save(sampleMerchant);
  }

  @Test
  void shouldAddStoreToPartnerWhenNotAlreadyLinked() {
    UUID storeUid = UUID.randomUUID();
    AdminAddStoreRequest request = new AdminAddStoreRequest(storeUid);
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleMerchant));
    when(userStoreRepository.existsByUserIdAndStoreUid(1L, storeUid)).thenReturn(false);

    adminPartnerService.addStoreToPartner(publicId, request);

    verify(userStoreRepository).save(any(UserStore.class));
  }

  @Test
  void shouldNotDuplicateStoreLinkWhenAlreadyExists() {
    UUID storeUid = UUID.randomUUID();
    AdminAddStoreRequest request = new AdminAddStoreRequest(storeUid);
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleMerchant));
    when(userStoreRepository.existsByUserIdAndStoreUid(1L, storeUid)).thenReturn(true);

    adminPartnerService.addStoreToPartner(publicId, request);

    verify(userStoreRepository, never()).save(any(UserStore.class));
  }

  @Test
  void shouldRemoveStoreFromPartnerAndDemoteToClientWhenNoStoresRemain() {
    UUID storeUid = UUID.randomUUID();
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleMerchant));
    when(userStoreRepository.findByUserId(1L)).thenReturn(List.of());

    adminPartnerService.removeStoreFromPartner(publicId, storeUid);

    verify(userStoreRepository).deleteByUserIdAndStoreUid(1L, storeUid);
    verify(userRoleTransitionService).changeUserRole(1L, UserRole.CLIENT);
  }

  @Test
  void shouldKeepMerchantRoleWhenOtherStoresRemainAfterRemoval() {
    UUID storeUid = UUID.randomUUID();
    UUID remainingStoreUid = UUID.randomUUID();
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(sampleMerchant));
    when(userStoreRepository.findByUserId(1L))
        .thenReturn(List.of(UserStore.builder().id(102L).userId(1L).storeUid(remainingStoreUid).build()));

    adminPartnerService.removeStoreFromPartner(publicId, storeUid);

    verify(userStoreRepository).deleteByUserIdAndStoreUid(1L, storeUid);
    verify(userRoleTransitionService, never()).changeUserRole(any(), any());
  }

  @Test
  void shouldPromoteClientToMerchantWhenAddingStore() {
    UUID clientPublicId = UUID.randomUUID();
    UUID storeUid = UUID.randomUUID();
    Client client =
        Client.builder()
            .id(2L)
            .publicId(clientPublicId)
            .email("client@example.com")
            .firstName("Alice")
            .lastName("Martin")
            .role(UserRole.CLIENT)
            .clientCode("CL12345")
            .build();
    when(userRepository.findByPublicId(clientPublicId)).thenReturn(Optional.of(client));
    when(userStoreRepository.existsByUserIdAndStoreUid(2L, storeUid)).thenReturn(false);

    adminPartnerService.addStoreToPartner(clientPublicId, new AdminAddStoreRequest(storeUid));

    verify(userRoleTransitionService).changeUserRole(2L, UserRole.MERCHANT);
    verify(userStoreRepository).save(any(UserStore.class));
  }

  @Test
  void shouldKeepAdminRoleWhenAddingStoreToAdmin() {
    UUID adminPublicId = UUID.randomUUID();
    UUID storeUid = UUID.randomUUID();
    Admin admin =
        Admin.builder()
            .id(3L)
            .publicId(adminPublicId)
            .email("admin@example.com")
            .firstName("Root")
            .lastName("Admin")
            .role(UserRole.ADMIN)
            .build();
    when(userRepository.findByPublicId(adminPublicId)).thenReturn(Optional.of(admin));
    when(userStoreRepository.existsByUserIdAndStoreUid(3L, storeUid)).thenReturn(false);

    adminPartnerService.addStoreToPartner(adminPublicId, new AdminAddStoreRequest(storeUid));

    verify(userRoleTransitionService, never()).changeUserRole(any(), any());
    verify(userStoreRepository).save(any(UserStore.class));
  }
}
