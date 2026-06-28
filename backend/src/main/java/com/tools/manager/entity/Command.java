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
@Table(name = "command")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Command {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long moduleId;

    private String name;

    private String command;

    private String description;

    /** 文档内容（Markdown 格式，用于 gitbook 类型模块） */
    @Column(columnDefinition = "TEXT")
    private String content;

    private String category;

    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
