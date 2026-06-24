package com.tools.manager.plugin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingPluginRepository extends JpaRepository<PendingPluginEntity, Long> {

    /** 根据 pluginId 查找待审核插件（状态为 PENDING） */
    Optional<PendingPluginEntity> findByPluginIdAndStatus(String pluginId, String status);

    /** 查找所有待审核插件（状态为 PENDING） */
    List<PendingPluginEntity> findByStatusOrderBySubmittedAtDesc(String status);

    /** 统计待审核插件数量 */
    long countByStatus(String status);

    /** 根据 pluginId 删除待审核插件 */
    void deleteByPluginId(String pluginId);
}
