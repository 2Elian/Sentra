package com.sentra.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 存储配置属性 --> 后面有需要读yml配置的就参考这个
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sentra.storage")
public class StorageProperties {

    /**
     * 图存储路径
     */
    private String graphPath;

    /**
     * SFTP远程配置
     */
    private SftpConfig sftp = new SftpConfig();

    @Data
    public static class SftpConfig {
        private String host;
        private int port;
        private String username;
        private String password;
        private String uploadPath;
        private String ocrOutputPath;
    }
}
