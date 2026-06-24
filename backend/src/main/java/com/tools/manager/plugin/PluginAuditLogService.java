package com.tools.manager.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 插件审核日志服务
 *
 * 记录和查询插件的上传、审核等操作日志。
 */
@Service
public class PluginAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(PluginAuditLogService.class);

    private final PluginAuditLogRepository auditLogRepository;

    public PluginAuditLogService(PluginAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 记录操作日志
     *
     * @param pluginId      插件 ID
     * @param pluginName    插件名称
     * @param pluginVersion 插件版本
     * @param action        操作行为：SUBMIT / APPROVE / REJECT / DELETE / UPLOAD / UPDATE
     * @param operator      操作者用户名
     * @param operatorIp    操作者 IP
     * @param detail        操作详情
     */
    @Transactional
    public void log(String pluginId, String pluginName, String pluginVersion,
                    String action, String operator, String operatorIp, String detail) {
        try {
            PluginAuditLogEntity entity = new PluginAuditLogEntity();
            entity.setPluginId(pluginId);
            entity.setPluginName(pluginName);
            entity.setPluginVersion(pluginVersion);
            entity.setAction(action);
            entity.setOperator(operator);
            entity.setOperatorIp(operatorIp);
            entity.setDetail(detail);
            auditLogRepository.save(entity);
            log.info("插件审核日志: {} {} {} by {} ({})", action, pluginId, pluginVersion, operator, operatorIp);
        } catch (Exception e) {
            // 日志记录失败不应影响主业务
            log.error("记录插件审核日志失败: {} {}", action, pluginId, e);
        }
    }

    /**
     * 分页查询所有日志（按时间倒序）
     *
     * @param page 页码（从 0 开始）
     * @param size 每页大小
     */
    public Page<PluginAuditLogEntity> getLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findAllByOrderByOperationTimeDesc(pageable);
    }

    /**
     * 根据 pluginId 分页查询日志（按时间倒序）
     *
     * @param pluginId 插件 ID
     * @param page     页码（从 0 开始）
     * @param size     每页大小
     */
    public Page<PluginAuditLogEntity> getLogsByPluginId(String pluginId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByPluginIdOrderByOperationTimeDesc(pluginId, pageable);
    }
}
