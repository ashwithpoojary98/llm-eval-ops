package io.github.ashwithpoojary98.llmops_eval.settings.repository;

import io.github.ashwithpoojary98.llmops_eval.settings.entity.AdminSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminSettingRepository extends JpaRepository<AdminSetting, UUID> {

    Optional<AdminSetting> findByOrganizationIdAndSettingKey(UUID organizationId, String settingKey);

    List<AdminSetting> findByOrganizationId(UUID organizationId);

    @Query("SELECT s FROM AdminSetting s WHERE s.organization.id = :orgId AND s.settingKey LIKE :prefix%")
    List<AdminSetting> findByOrganizationIdAndKeyPrefix(
            @Param("orgId") UUID orgId,
            @Param("prefix") String prefix);

    boolean existsByOrganizationIdAndSettingKey(UUID organizationId, String settingKey);
}
