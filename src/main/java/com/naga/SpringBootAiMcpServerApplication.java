package com.naga;

import com.naga.github.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

@SpringBootApplication
@ComponentScan(basePackages = {"com.naga.*", "com.spring.*"})
public class SpringBootAiMcpServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(SpringBootAiMcpServerApplication.class);

    public static void main(String[] args) {
        logger.info("SpringBootAiMcpServerApplication");
        SpringApplication.run(SpringBootAiMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider gitHubTools(GitHubService service) {
        return MethodToolCallbackProvider.builder().toolObjects(service).build();
    }

}
