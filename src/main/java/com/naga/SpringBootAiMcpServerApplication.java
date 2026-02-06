package com.naga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class SpringBootAiMcpServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(SpringBootAiMcpServerApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Spring Boot AI MCP Server Application");
        SpringApplication.run(SpringBootAiMcpServerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("✓ Application started successfully");
        logger.info("✓ MCP Tools available via SSE at: http://localhost:8088/mcp/sse");
        logger.info("✓ REST API available at: http://localhost:8088/api/github/prs");
        logger.info("✓ Actuator health: http://localhost:8088/actuator/health");
    }
}