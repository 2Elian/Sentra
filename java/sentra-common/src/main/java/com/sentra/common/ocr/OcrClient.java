package com.sentra.common.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * OCR客户端
 * 调用MinerU API进行文档解析
 * 可被多个服务复用（knowledge-service、agent-service等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrClient {

    private final OcrProperties ocrProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用OCR API解析PDF文件
     *
     * @param pdfFile  PDF文件
     * @param outputDir OCR输出目录
     * @return OCR响应结果
     * @throws IOException 调用失败时抛出
     */
    public OcrResponse parsePdf(File pdfFile, String outputDir) throws IOException {
        OcrResponse response = new OcrResponse();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(ocrProperties.getApiUrl());

            // 构建multipart请求
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addPart("files", new FileBody(pdfFile, ContentType.APPLICATION_OCTET_STREAM));
            builder.addTextBody("output_dir", outputDir, ContentType.TEXT_PLAIN);
            builder.addTextBody("backend", ocrProperties.getBackend(), ContentType.TEXT_PLAIN);
            builder.addTextBody("return_middle_json", "true", ContentType.TEXT_PLAIN);
            builder.addTextBody("return_model_output", "true", ContentType.TEXT_PLAIN);
            builder.addTextBody("return_content_list", "true", ContentType.TEXT_PLAIN);
            builder.addTextBody("formula_enable", "true", ContentType.TEXT_PLAIN);
            builder.addTextBody("table_enable", "true", ContentType.TEXT_PLAIN);
            builder.addTextBody("return_images", "true", ContentType.TEXT_PLAIN);

            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);

            log.info("向OCR服务发送请求: {}, 文件: {}", ocrProperties.getApiUrl(), pdfFile.getName());

            // 执行请求
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                int statusCode = httpResponse.getCode();
                HttpEntity responseEntity = httpResponse.getEntity();
                String result = responseEntity != null ? EntityUtils.toString(responseEntity) : "";

                log.debug("OCR响应状态码: {}, 响应长度: {}", statusCode, result.length());

                if (statusCode == 200) {
                    // 解析响应
                    JsonNode rootNode = objectMapper.readTree(result);
                    JsonNode resultsNode = rootNode.path("results");

                    if (resultsNode.isObject() && resultsNode.size() > 0) {
                        String firstKey = resultsNode.fieldNames().next();
                        JsonNode fileResultNode = resultsNode.get(firstKey);

                        if (fileResultNode.has("md_content")) {
                            String mdContent = fileResultNode.get("md_content").asText();
                            response.setMdContent(mdContent);
                            response.setSuccess(true);
                            log.info("OCR解析成功，Markdown内容长度: {}", mdContent.length());
                        } else {
                            response.setSuccess(false);
                            response.setErrorMessage("响应中未找到md_content字段");
                            log.error("OCR响应格式错误: 未找到md_content字段");
                        }
                    } else {
                        response.setSuccess(false);
                        response.setErrorMessage("响应中未找到results字段");
                        log.error("OCR响应格式错误: 未找到results字段");
                    }

                    // 保存原始响应
                    response.setRawResponse(objectMapper.convertValue(rootNode, Map.class));
                } else {
                    response.setSuccess(false);
                    response.setErrorMessage("OCR API调用失败: HTTP " + statusCode);
                    log.error("OCR API调用失败: code={}, response={}", statusCode, result);
                    throw new IOException("OCR API调用失败: " + statusCode + ", " + result);
                }
            }

        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("调用OCR API异常: " + e.getMessage());
            log.error("调用OCR API失败", e);
            throw new IOException("调用OCR API失败", e);
        }

        return response;
    }

    /**
     * 调用OCR API解析PDF文件（使用默认输出目录）
     *
     * @param pdfFile PDF文件
     * @return OCR响应结果
     * @throws IOException 调用失败时抛出
     */
    public OcrResponse parsePdf(File pdfFile) throws IOException {
        return parsePdf(pdfFile, System.getProperty("java.io.tmpdir"));
    }
}
