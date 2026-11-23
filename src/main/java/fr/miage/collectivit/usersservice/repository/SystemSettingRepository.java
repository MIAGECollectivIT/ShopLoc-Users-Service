package fr.miage.collectivit.usersservice.repository;

import fr.miage.collectivit.usersservice.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {}
