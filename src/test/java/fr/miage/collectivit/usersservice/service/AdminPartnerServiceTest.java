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
import fr.miage.collectivit.usersservice.entity.Merchant;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.entity.UserStore;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.repository.UserStoreRepository;
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
    assertThat(partners.getFirst().id()).isEqualTo(1L);
    assertThat(partners.getFirst().existingStoresCount()).isEqualTo(3);
  }

  @Test
  void shouldGetPartnerDetailsWhenFound() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
    when(userStoreRepository.findByUserId(1L))
        .thenReturn(
            List.of(
                UserStore.builder().id(100L).userId(1L).storeId(42L).build(),
                UserStore.builder().id(101L).userId(1L).storeId(84L).build()));

    PartnerDetailResponse details = adminPartnerService.getPartnerDetails("1");

    assertThat(details.id()).isEqualTo(1L);
    assertThat(details.email()).isEqualTo("partner@example.com");
    assertThat(details.storeIds()).containsExactly(42L, 84L);
  }

  @Test
  void shouldThrowWhenPartnerNotFound() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminPartnerService.getPartnerDetails("999"))
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

    when(userRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userStoreRepository.findByUserId(1L)).thenReturn(List.of());

    PartnerDetailResponse updated = adminPartnerService.updatePartner("1", request);

    assertThat(updated.firstName()).isEqualTo("Marc");
    assertThat(updated.lastName()).isEqualTo("Durand");
    assertThat(updated.birthDate()).isEqualTo(LocalDate.of(1982, 3, 10));
    verify(userRepository).save(sampleMerchant);
  }

  @Test
  void shouldAddStoreToPartnerWhenNotAlreadyLinked() {
    AdminAddStoreRequest request = new AdminAddStoreRequest(55L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
    when(userStoreRepository.existsByUserIdAndStoreId(1L, 55L)).thenReturn(false);

    adminPartnerService.addStoreToPartner("1", request);

    verify(userStoreRepository).save(any(UserStore.class));
  }

  @Test
  void shouldNotDuplicateStoreLinkWhenAlreadyExists() {
    AdminAddStoreRequest request = new AdminAddStoreRequest(55L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
    when(userStoreRepository.existsByUserIdAndStoreId(1L, 55L)).thenReturn(true);

    adminPartnerService.addStoreToPartner("1", request);

    verify(userStoreRepository, never()).save(any(UserStore.class));
  }
}

