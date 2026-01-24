package com.sentra.knowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.sentra.knowledge.mapper")
public class SentraKnowledgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SentraKnowledgeApplication.class, args);
    }
}
