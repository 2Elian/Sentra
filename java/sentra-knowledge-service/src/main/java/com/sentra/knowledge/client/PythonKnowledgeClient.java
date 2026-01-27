package com.sentra.knowledge.client;

import com.sentra.knowledge.client.dto.KbPipelineRequest;
import com.sentra.knowledge.client.dto.KbPipelineResponse;
import com.sentra.knowledge.client.dto.MdParseRequest;
import com.sentra.knowledge.client.dto.MdParseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Python知识服务客户端
 * 调用Python FastAPI接口进行文档处理
 */
@Slf4j
@Component
public class PythonKnowledgeClient {

    private final RestTemplate restTemplate;

    @Value("${sentra.python.base-url:http://localhost:8000}")
    private String pythonBaseUrl;

    public PythonKnowledgeClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 调用Python mdParse接口
     * 对OCR解析后的Markdown内容进行章节重构
     *
     * @param documentId  文档ID
     * @param kbId        知识库ID
     * @param mdContent   OCR解析后的Markdown内容
     * @return 重构后的Markdown内容
     */
    public String parseMarkdown(String documentId, String kbId, String mdContent) {
        String url = pythonBaseUrl + "/sentra/v1/knowledge/mdParse";

        log.info("调用Python mdParse接口，documentId: {}, kbId: {}", documentId, kbId);

        try {
            MdParseRequest request = new MdParseRequest();
            request.setDocumentId(documentId);
            request.setKbId(kbId);
            request.setMdContent(mdContent);

            MdParseResponse response = restTemplate.postForObject(url, request, MdParseResponse.class);

            if (response != null && response.getNewMdContent() != null) {
                log.info("mdParse调用成功，重构后内容长度: {}", response.getNewMdContent().length());
                return response.getNewMdContent();
            } else {
                throw new RuntimeException("mdParse响应为空");
            }

        } catch (Exception e) {
            log.error("调用Python mdParse接口失败", e);
            throw new RuntimeException("mdParse调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用Python知识库构建接口 (KbPipeline)
     *
     * @param docID          文档ID
     * @param kbID           知识库ID
     * @param content        Markdown内容
     * @param title          文档标题
     * @param entityTypes    实体类型列表 List[str]
     * @param entityTypesDes 实体类型描述 Dict[str, str]
     * @return 知识库构建响应
     */
    public KbPipelineResponse buildKnowledgeBase(
            String docID,
            String kbID,
            String content,
            String title,
            List<String> entityTypes,
            Map<String, String> entityTypesDes
    ) {
        String url = pythonBaseUrl + "/sentra/v1/knowledge/build";

        log.info("调用Python知识库构建接口 (KbPipeline)，docID: {}, kbID: {}, entityTypes数量: {}",
                docID, kbID, entityTypes != null ? entityTypes.size() : 0);

        try {
            KbPipelineRequest request = new KbPipelineRequest();
            request.setDocID(docID);
            request.setKbID(kbID);
            request.setContent(content);
            request.setTitle(title);
            request.setEntityTypes(entityTypes);
            request.setEntityTypesDes(entityTypesDes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<KbPipelineRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<KbPipelineResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    KbPipelineResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KbPipelineResponse body = response.getBody();
                log.info("KbPipeline调用成功，status: {}, totalChunks: {}, totalEntities: {}, totalEdges: {}, totalQac: {}",
                        body.getStatus(),
                        body.getTotalChunks(),
                        body.getTotalEntities(),
                        body.getTotalEdges(),
                        body.getTotalQac());
                return body;
            } else {
                throw new RuntimeException("KbPipeline响应失败: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("调用Python知识库构建接口失败 (KbPipeline)", e);
            throw new RuntimeException("KbPipeline调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用Python删除接口，删除知识库中的文档数据
     *
     * @param kbId            知识库ID
     * @param documentUniqueId 文档唯一标识
     * @return 是否删除成功
     */
    public boolean deleteDocumentFromKnowledgeBase(String kbId, String documentUniqueId) {
        String url = pythonBaseUrl + "/sentra/v1/knowledge/delete";

        log.info("调用Python删除接口，kbId: {}, documentUniqueId: {}", kbId, documentUniqueId);

        try {
            // 构建请求体
            java.util.Map<String, String> request = new java.util.HashMap<>();
            request.put("kbId", kbId);
            request.put("documentUniqueId", documentUniqueId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<java.util.Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Python删除接口调用成功，kbId: {}, documentUniqueId: {}", kbId, documentUniqueId);
                return true;
            } else {
                log.warn("Python删除接口返回非成功状态，statusCode: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("调用Python删除接口失败，kbId: {}, documentUniqueId: {}", kbId, documentUniqueId, e);
            // 不抛出异常，允许继续清理其他数据
            return false;
        }
    }

    /**
     * 调用Python删除接口，删除整个知识库的数据
     *
     * @param kbId 知识库ID
     * @return 是否删除成功
     */
    public boolean deleteKnowledgeBase(String kbId) {
        String url = pythonBaseUrl + "/sentra/v1/knowledge/delete/kb";

        log.info("调用Python知识库删除接口，kbId: {}", kbId);

        try {
            // 构建请求体
            java.util.Map<String, String> request = new java.util.HashMap<>();
            request.put("kbId", kbId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<java.util.Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Python知识库删除接口调用成功，kbId: {}", kbId);
                return true;
            } else {
                log.warn("Python知识库删除接口返回非成功状态，statusCode: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("调用Python知识库删除接口失败，kbId: {}", kbId, e);
            // 不抛出异常，允许继续清理其他数据
            return false;
        }
    }
}
