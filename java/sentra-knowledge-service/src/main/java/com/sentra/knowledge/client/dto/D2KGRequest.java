package com.sentra.knowledge.client.dto;

import lombok.Data;

import java.util.Map;

/**
 * Python D2KG接口请求DTO
 * 用于构建知识图谱
 */
@Data
public class D2KGRequest {

    /**
     * 合同ID（对应知识库ID）
     */
    private String contractId;

    /**
     * 合同文本内容（Markdown格式）
     */
    private String contractText;

    /**
     * 实体类型配置
     * 格式：{"ContractParty": "合同主体（甲乙方）", "Amount": "金额", ...}
     */
    private Map<String, String> entityTypes;
}
