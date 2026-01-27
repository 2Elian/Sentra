package com.sentra.knowledge.client.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Python知识库构建接口请求DTO (KbPipeline)
 * 对应Python端: class KbPipelineRequest(BaseModel)
 */
@Data
public class KbPipelineRequest {

    /**
     * 文档ID
     * 对应Python端: docID: str
     */
    private String docID;

    /**
     * 知识库ID
     * 对应Python端: kbID: str
     */
    private String kbID;

    /**
     * 文档内容（Markdown格式）
     * 对应Python端: content: str
     */
    private String content;

    /**
     * 文档标题
     * 对应Python端: title: Optional[str] = None
     */
    private String title;

    /**
     * 实体类型配置（必填）
     * 对应Python端: entity_types: List[str]
     * 示例: ["ContractParty", "Amount", "Date", ...]
     */
    private List<String> entityTypes;

    /**
     * 实体类型描述（必填）
     * 对应Python端: entity_types_des: Dict[str, str]
     * 示例: {"ContractParty": "合同主体（甲乙方）", "Amount": "金额", ...}
     */
    private Map<String, String> entityTypesDes;
}
