package com.sentra.knowledge.client;

import com.sentra.knowledge.client.dto.D2KGRequest;
import com.sentra.knowledge.client.dto.D2KGResponse;
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
     * 调用Python D2KG接口构建知识图谱
     *
     * @param kbId        知识库ID
     * @param mdContent   Markdown内容
     * @param entityTypes 实体类型配置（可选）
     * @return 文档唯一标识（用于定位GraphML文件）
     */
    public String buildKnowledgeGraph(String kbId, String mdContent, java.util.Map<String, String> entityTypes) {
        String url = pythonBaseUrl + "/sentra/v1/d2kg/build";

        log.info("调用Python D2KG接口，kbId: {}, entityTypes数量: {}", kbId,
                entityTypes != null ? entityTypes.size() : 0);

        try {
            D2KGRequest request = new D2KGRequest();
            request.setContractId(kbId);
            request.setContractText(mdContent);
            request.setEntityTypes(entityTypes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<D2KGRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<D2KGResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    D2KGResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                D2KGResponse body = response.getBody();
                String documentUniqueId = body.getDocumentUniqueId();
                log.info("D2KG调用成功，documentUniqueId: {}, graphNamespace: {}, nodes: {}, edges: {}",
                        documentUniqueId, body.getGraphNamespace(),
                        body.getNodes() != null ? body.getNodes().size() : 0,
                        body.getEdges() != null ? body.getEdges().size() : 0);
                return documentUniqueId;
            } else {
                throw new RuntimeException("D2KG响应失败: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("调用Python D2KG接口失败", e);
            throw new RuntimeException("D2KG调用失败: " + e.getMessage(), e);
        }
    }
}
