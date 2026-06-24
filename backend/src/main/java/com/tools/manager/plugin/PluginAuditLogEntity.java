package com.tools.manager.plugin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 插件审核日志实体
 *
 * 记录插件的上传、审核通过、审核拒绝、删除等操作日志。
 */
@Entity
@Table(name = "plugin_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作时间 */
    @Column(name = "operation_time", nullable = false, updatable = false)
    private LocalDateTime operationTime;

    /** 操作者 IP 地址 */
    @Column(name = "operator_ip", length = 100)
    private String operatorIp;

    /** 插件 ID */
    @Column(name = "plugin_id", nullable = false, length = 100)
    private String pluginId;

    /** 插件名称 */
    @Column(name = "plugin_name", length = 100)
    private String pluginName;

    /** 插件版本 */
    @Column(name = "plugin_version", length = 20)
    private String pluginVersion;

    /** 操作行为：SUBMIT / APPROVE / REJECT / DELETE / UPLOAD / UPDATE */
    @Column(name = "action", nullable = false, length = 20)
    private String action;

    /** 操作者用户名 */
    @Column(name = "operator", length = 100)
    private String operator;

    /** 操作详情/备注 */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @PrePersist
    public void prePersist() {
        if (this.operationTime == null) {
            this.operationTime = LocalDateTime.now();
        }
    }
}
