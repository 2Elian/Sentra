package com.sentra.knowledge.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KnowledgeType {
    DOCUMENT("文档库"),
    GRAPH("知识图谱"),
    HYBRID("混合库");

    private final String description;
}
