package com.naga;

import com.naga.github.GitHubToolsService;
import io.modelcontextprotocol.server.McpSyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@ComponentScan(basePackages = {"com.naga.*", "com.spring.*"})
public class SpringBootAiMcpServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(SpringBootAiMcpServerApplication.class);

    public static void main(String[] args) {
        logger.info("SpringBootAiMcpServerApplication");
        SpringApplication.run(SpringBootAiMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider gitHubTools(GitHubToolsService service) {
        return MethodToolCallbackProvider.builder().toolObjects(service).build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        McpSyncServer server = event.getApplicationContext().getBean(McpSyncServer.class);
        logger.info("MCP Server initialized at SSE endpoint: {}", server.getAsyncServer().getServerInfo().name());
    }

}
