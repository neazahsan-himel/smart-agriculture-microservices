package com.smartagriculture.aiadvisorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AiAdvisorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAdvisorServiceApplication.class, args);
    }
}