package com.sentra.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档状态枚举
 */
@Getter
@AllArgsConstructor
public enum DocumentStatus {
    UPLOADED("已上传"),
    PARSING("解析中"),
    READY("解析完成"),
    KB_BUILDING("知识库构建中"),
    COMPLETED("全部完成"),
    FAILED("失败");

    private final String description;
}
