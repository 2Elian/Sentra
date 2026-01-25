package com.sentra.knowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties  // 启用配置属性
@MapperScan("com.sentra.knowledge.mapper")
@ComponentScan(basePackages = {
    "com.sentra.knowledge",
    "com.sentra.common"  // 扫描sentra-common模块的组件
})
public class SentraKnowledgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SentraKnowledgeApplication.class, args);
    }
}
