package com.sentra.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 存储配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sentra.storage")
public class StorageProperties {

    /**
     * Neo4j本地图存储路径
     */
    private String graphPath = "G:\\项目成果打包\\基于图结构的文档问答助手\\logs\\sentra\\graph";

    /**
     * SFTP远程配置
     */
    private SftpConfig sftp = new SftpConfig();

    @Data
    public static class SftpConfig {
        private String host = "172.16.107.15";
        private int port = 22;
        private String username = "202312150002";
        private String password = "20001017aA@";
        private String uploadPath = "/data/lzm/sentra/data/files";
        private String ocrOutputPath = "/data/lzm/sentra/data/ocr_result";
    }
}
