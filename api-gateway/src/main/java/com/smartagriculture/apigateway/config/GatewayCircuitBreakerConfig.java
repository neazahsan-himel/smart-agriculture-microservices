package com.smartagriculture.apigateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GatewayCircuitBreakerConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallRateThreshold(80)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .build();

        // Must match the global httpclient response-timeout (10s); if omitted,
        // Resilience4j defaults to 1s and cancels the reactive chain too early
        TimeLimiterConfig defaultTimeLimiter = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .build();

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(defaultConfig)
                .timeLimiterConfig(defaultTimeLimiter)
                .build());
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> aiAdvisorCircuitBreakerCustomizer() {
        // Ollama is inherently slow (30-60s) — threshold must exceed the 60s route
        // timeout so normal LLM responses never count as slow calls
        CircuitBreakerConfig aiAdvisorConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallRateThreshold(100)
                .slowCallDurationThreshold(Duration.ofSeconds(65))
                .build();

        // Must be > 60s route timeout so Resilience4j never cancels a valid Ollama response
        TimeLimiterConfig aiAdvisorTimeLimiter = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(65))
                .build();

        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(aiAdvisorConfig)
                .timeLimiterConfig(aiAdvisorTimeLimiter), "ai-advisor-service");
    }
}