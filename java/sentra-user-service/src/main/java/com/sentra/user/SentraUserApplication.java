package com.sentra.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.sentra.user.mapper")
public class SentraUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(SentraUserApplication.class, args);
    }
}
