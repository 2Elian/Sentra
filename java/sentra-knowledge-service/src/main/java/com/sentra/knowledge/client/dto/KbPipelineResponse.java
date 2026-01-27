package com.sentra.knowledge.client.dto;

import lombok.Data;

/**
 * Python知识库构建接口响应DTO (KbPipeline)
 * 对应Python端: class KbPipelineReponse(BaseModel)
 */
@Data
public class KbPipelineResponse {

    /**
     * 处理状态
     * 对应Python端: status: StatusType
     */
    private String status;

    /**
     * 总切块数量
     * 对应Python端: total_chunks: int
     */
    private Integer totalChunks;

    /**
     * 总实体数量
     * 对应Python端: total_entities: int
     */
    private Integer totalEntities;

    /**
     * 总边（关系）数量
     * 对应Python端: total_edges: int
     */
    private Integer totalEdges;

    /**
     * 总问答对数量
     * 对应Python端: total_qac: int
     */
    private Integer totalQac;
}
