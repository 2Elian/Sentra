package com.sentra.knowledge.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Python D2KG接口响应DTO
 * 返回知识图谱构建结果
 */
@Data
public class D2KGResponse {

    /**
     * 图谱节点列表
     */
    private List<Node> nodes;

    /**
     * 图谱边列表
     */
    private List<Edge> edges;

    /**
     * 图谱命名空间
     */
    private String graphNamespace;

    /**
     * 文档唯一标识（用于定位GraphML文件）
     */
    private String documentUniqueId;

    /**
     * 节点数据结构
     */
    @Data
    public static class Node {
        private String id;
        private String type;
        private String name;
        private String description;
    }

    /**
     * 边数据结构
     */
    @Data
    public static class Edge {
        private String source;
        private String target;
        private String relation;
        private String description;
    }
}
