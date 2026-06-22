package com.tools.manager.plugin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "custom_plugin")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plugin_id", unique = true, nullable = false, length = 100)
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

    @Column(nullable = false)
    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PluginVisibility visibility;

    @Column(name = "need_backend", nullable = false)
    private Boolean needBackend;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.uploadedAt = now;
        this.updatedAt = now;
        if (this.enabled == null) {
            this.enabled = true;
        }
        if (this.visibility == null) {
            this.visibility = PluginVisibility.ALL;
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
