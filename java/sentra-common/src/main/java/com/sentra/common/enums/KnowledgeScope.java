package com.sentra.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库范围枚举
 */
@Getter
@AllArgsConstructor
public enum KnowledgeScope {
    PRIVATE("私有"),
    TENANT("租户内共享"),
    PUBLIC("公开");

    private final String description;
}
