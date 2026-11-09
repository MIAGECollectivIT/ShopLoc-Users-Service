package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.UserStore;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStoreRepository extends JpaRepository<UserStore, Long> {

  List<UserStore> findByUserId(Long userId);

  boolean existsByUserIdAndStoreId(Long userId, Long storeId);

  @Query("SELECT s.userId, COUNT(s) FROM UserStore s WHERE s.userId IN :userIds GROUP BY s.userId")
  List<Object[]> countStoresByUserIds(@Param("userIds") Collection<Long> userIds);
}
