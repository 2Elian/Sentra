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
    READY("就绪"),
    FAILED("解析失败");

    private final String description;
}
