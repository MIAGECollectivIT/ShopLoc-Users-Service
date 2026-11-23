package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.UserStore;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStoreRepository extends JpaRepository<UserStore, Long> {

  List<UserStore> findByUserId(Long userId);

  boolean existsByUserIdAndStoreUid(Long userId, UUID storeUid);

  void deleteByUserIdAndStoreUid(Long userId, UUID storeUid);

  @Query(
      "SELECT s.userId, COUNT(s) FROM UserStore s WHERE s.userId IN :userIds AND s.storeUid IS NOT NULL GROUP BY s.userId")
  List<Object[]> countStoresByUserIds(@Param("userIds") Collection<Long> userIds);
}
