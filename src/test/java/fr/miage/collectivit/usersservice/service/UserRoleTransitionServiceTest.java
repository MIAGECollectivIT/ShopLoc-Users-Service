package fr.miage.collectivit.usersservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.service.impl.UserRoleTransitionServiceImpl;
import fr.miage.collectivit.usersservice.util.ClientCodeGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRoleTransitionServiceTest {

  @Mock private EntityManager entityManager;
  @Mock private UserRepository userRepository;
  @Mock private ClientCodeGenerator clientCodeGenerator;
  @Mock private Query queryMock;

  @InjectMocks private UserRoleTransitionServiceImpl userRoleTransitionService;

  @BeforeEach
  void setUp() {
    when(entityManager.createNativeQuery(anyString())).thenReturn(queryMock);
    when(queryMock.setParameter(anyString(), any())).thenReturn(queryMock);
    when(queryMock.getSingleResult()).thenReturn(0L);
  }

  @Test
  void shouldTransitionUserToMerchant() {
    userRoleTransitionService.changeUserRole(1L, UserRole.MERCHANT);

    verify(queryMock).setParameter("role", "MERCHANT");
    verify(queryMock, atLeastOnce()).setParameter("userId", 1L);
    verify(entityManager).flush();
    verify(entityManager).clear();
  }

  @Test
  void shouldTransitionUserToClientWithUniqueCode() {
    when(clientCodeGenerator.generateCode()).thenReturn("CL99999");
    when(userRepository.existsByClientCode("CL99999")).thenReturn(false);

    userRoleTransitionService.changeUserRole(2L, UserRole.CLIENT);

    verify(queryMock).setParameter("role", "CLIENT");
    verify(queryMock).setParameter(eq("clientCode"), eq("CL99999"));
    verify(entityManager).flush();
    verify(entityManager).clear();
  }
}
