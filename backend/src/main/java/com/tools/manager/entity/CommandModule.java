package com.tools.manager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "command_module")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private String icon;

    private Integer sortOrder;

    /** 模块格式：command（命令库，默认）或 gitbook（文档库） */
    @Column(nullable = false)
    private String format = "command";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
