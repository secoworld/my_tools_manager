package com.tools.manager.plugin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待审核插件实体
 *
 * 非 admin 用户提交的插件存储在此表中，审核通过后会被移动到 custom_plugin 表。
 * 相同 pluginId 的待审核插件会被覆盖（只保留最新提交）。
 */
@Entity
@Table(name = "pending_plugin")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingPluginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plugin_id", nullable = false, length = 100)
    private String pluginId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String icon;

    @Column(length = 50)
    private String category;

    @Column(length = 100)
    private String author;

    @Column(name = "entry_file", nullable = false, length = 255)
    private String entryFile;

    @Column(name = "need_backend", nullable = false)
    private Boolean needBackend;

    /** 提交者用户名（非 admin 用户） */
    @Column(name = "submitter", length = 100)
    private String submitter;

    /** 提交时间 */
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    /** 更新时间 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 审核状态：PENDING / APPROVED / REJECTED */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 审核备注（拒绝原因等） */
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    /** ZIP 文件在磁盘上的存储路径（相对于 plugins pending 目录） */
    @Column(name = "zip_path", nullable = false, length = 500)
    private String zipPath;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.submittedAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "PENDING";
        }
        if (this.needBackend == null) {
            this.needBackend = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
