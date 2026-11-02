package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.UserStore;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStoreRepository extends JpaRepository<UserStore, Long> {

  List<UserStore> findByUserId(Long userId);

  long countByUserId(Long userId);

  Optional<UserStore> findByUserIdAndStoreId(Long userId, Long storeId);

  boolean existsByUserIdAndStoreId(Long userId, Long storeId);

  @Query("SELECT s.userId, COUNT(s) FROM UserStore s WHERE s.userId IN :userIds GROUP BY s.userId")
  List<Object[]> countStoresByUserIds(@Param("userIds") Collection<Long> userIds);
}
