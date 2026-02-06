package com.naga.controller;

import com.naga.github.GitHubToolsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;

/**
 * Manual MCP SSE endpoint controller
 * Exposes MCP tools via Server-Sent Events
 */
@RestController
@RequestMapping("/mcp")
public class McpSseController {

    private static final Logger logger = LoggerFactory.getLogger(McpSseController.class);
    private final GitHubToolsService gitHubToolsService;

    @Autowired
    public McpSseController(GitHubToolsService gitHubToolsService) {
        this.gitHubToolsService = gitHubToolsService;
        logger.info("MCP SSE Controller initialized");
    }

    /**
     * SSE endpoint for MCP protocol
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sseEndpoint() {
        logger.info("SSE client connected to /mcp/sse");

        return Flux.interval(Duration.ofSeconds(30))
                .map(sequence -> "event: heartbeat\ndata: {\"timestamp\": \"" +
                        new Date() + "\"}\n\n")
                .doOnCancel(() -> logger.info("SSE client disconnected"));
    }

    /**
     * List all available MCP tools
     */
    @PostMapping("/tools/list")
    public Map<String, Object> listTools() {
        logger.info("Listing MCP tools");

        List<Map<String, Object>> tools = new ArrayList<>();

        Map<String, Object> tool1 = new HashMap<>();
        tool1.put("name", "getAllPullRequests");
        tool1.put("description", "Retrieves all pull requests from the GitHub repository. " +
                "Returns PR number, title, state, author, and creation date for each pull request. " +
                "State parameter can be: 'open', 'closed', or 'all'. Defaults to 'open' if not specified.");
        tool1.put("parameters", Map.of(
                "state", Map.of(
                        "type", "string",
                        "description", "Filter pull requests by state: 'open', 'closed', or 'all'",
                        "required", false,
                        "default", "open"
                )
        ));
        tools.add(tool1);

        Map<String, Object> response = new HashMap<>();
        response.put("tools", tools);
        response.put("count", tools.size());

        return response;
    }

    /**
     * Execute a specific MCP tool
     */
    @PostMapping("/tools/call")
    public Map<String, Object> callTool(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.getOrDefault("arguments", new HashMap<>());

        logger.info("Calling tool: {} with arguments: {}", toolName, arguments);

        Map<String, Object> response = new HashMap<>();

        try {
            // Route to the appropriate tool
            if ("getAllPullRequests".equals(toolName)) {
                String state = (String) arguments.getOrDefault("state", "open");
                List<Map<String, Object>> result = gitHubToolsService.getAllPullRequests(state);

                response.put("status", "success");
                response.put("result", result);
                logger.info("Tool executed successfully, returned {} items", result.size());
            } else {
                throw new IllegalArgumentException("Unknown tool: " + toolName);
            }

        } catch (Exception e) {
            logger.error("Error calling tool {}: {}", toolName, e.getMessage(), e);
            response.put("status", "error");
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * Get MCP server info
     */
    @GetMapping("/info")
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "spring_boot_ai_mcp_server");
        info.put("version", "1.0.0");
        info.put("protocol", "SSE");
        info.put("endpoint", "/mcp/sse");
        info.put("toolCount", 1);
        info.put("tools", List.of("getAllPullRequests"));
        info.put("status", "running");

        return info;
    }
}