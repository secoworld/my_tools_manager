package com.tools.manager.plugin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PluginAuditLogRepository extends JpaRepository<PluginAuditLogEntity, Long> {

    /** 分页查询所有日志（按时间倒序） */
    Page<PluginAuditLogEntity> findAllByOrderByOperationTimeDesc(Pageable pageable);

    /** 根据 pluginId 分页查询日志（按时间倒序） */
    Page<PluginAuditLogEntity> findByPluginIdOrderByOperationTimeDesc(String pluginId, Pageable pageable);
}
