package com.sentra.knowledge.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentParseStrategy {
    TEXT("全文索引"),
    CHUNK("切片向量"),
    KG("知识图谱抽取");

    private final String description;
}
