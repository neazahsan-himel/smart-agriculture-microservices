package com.smartagriculture.aiadvisorservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        // qwen3:8b is a "thinking" model — on CPU-only inference a single reply can take
        // several minutes once the prompt includes farm/crop/weather/memory/conversation-history
        // context, and grows further as a conversation gets longer.
        factory.setReadTimeout(Duration.ofSeconds(600));
        return new RestTemplate(factory);
    }
}