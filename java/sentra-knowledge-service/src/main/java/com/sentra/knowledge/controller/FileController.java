package com.sentra.knowledge.controller;

import com.sentra.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件访问Controller
 * 用于提供本地文件访问
 */
@RestController
@RequestMapping("/v1/file")
@RequiredArgsConstructor
public class FileController {

    /**
     * 获取文件内容
     * @param path 文件路径 (URL参数)
     * @return 文件内容
     */
    @GetMapping
    public ResponseEntity<Resource> getFile(@RequestParam("path") String path) {
        try {
            // 将URL编码的路径解码,并将正斜杠转换为反斜杠(Windows路径)
            String filePath = java.net.URLDecoder.decode(path, "UTF-8");
            filePath = filePath.replace("/", "\\");

            Path normalizedPath = Paths.get(filePath).normalize();
            File file = normalizedPath.toFile();

            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            // 根据文件扩展名设置Content-Type
            String contentType = MediaType.APPLICATION_JSON_VALUE;
            if (filePath.endsWith(".graphml")) {
                contentType = "application/xml"; // GraphML是XML格式
            } else if (filePath.endsWith(".json")) {
                contentType = MediaType.APPLICATION_JSON_VALUE;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
