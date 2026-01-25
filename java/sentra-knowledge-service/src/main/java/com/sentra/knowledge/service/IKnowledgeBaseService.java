package com.sentra.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sentra.knowledge.dto.KnowledgeBaseCreateRequest;
import com.sentra.knowledge.dto.KnowledgeBaseResponse;
import com.sentra.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.sentra.knowledge.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface IKnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 创建知识库
     * 包含校验、kb_id生成和数据库初始化
     *
     * @param request 创建请求
     * @param tenantId 租户ID
     * @return 知识库响应
     */
    KnowledgeBaseResponse createKnowledgeBase(KnowledgeBaseCreateRequest request, String tenantId);

    /**
     * 更新知识库
     *
     * @param kbId    知识库ID
     * @param request 更新请求
     * @return 是否成功
     */
    boolean updateKnowledgeBase(String kbId, KnowledgeBaseUpdateRequest request);

    /**
     * 删除知识库
     * 同时删除相关数据库存储
     *
     * @param kbId 知识库ID
     * @return 是否成功
     */
    boolean deleteKnowledgeBase(String kbId);

    /**
     * 获取知识库详情
     *
     * @param kbId 知识库ID
     * @return 知识库响应
     */
    KnowledgeBaseResponse getKnowledgeBaseByKbId(String kbId);

    /**
     * 获取租户下的所有知识库
     *
     * @param tenantId 租户ID
     * @return 知识库列表
     */
    List<KnowledgeBaseResponse> listByTenantId(String tenantId);

    /**
     * 获取用户的知识库列表
     *
     * @param tenantId    租户ID
     * @param ownerUserId 所有者用户ID
     * @return 知识库列表
     */
    List<KnowledgeBaseResponse> listByOwner(String tenantId, String ownerUserId);
}
