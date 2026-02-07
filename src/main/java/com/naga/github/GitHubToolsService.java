package com.naga.github;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub MCP Tools Service
 * Exposes GitHub operations as MCP tools for external clients
 */
@Service
public class GitHubToolsService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubToolsService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Value("${github.owner}")
    private String githubOwner;

    @Value("${github.repo}")
    private String githubRepo;

    @Value("${github.api.bearer.token}")
    private String gitHubToken;

    private final WebClient webClient;

    @Autowired
    public GitHubToolsService(@Qualifier("GITHUB_WEBCLIENT") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Retrieves all pull requests for the configured repository
     *
     * @param state Filter by pull request state (open, closed, all). Defaults to "open"
     * @return List of pull request details
     */
    @Tool(
            name = "getAllPullRequests",
            description = "Retrieves all pull requests from the GitHub repository. " +
                    "Returns PR number, title, state, author, and creation date for each pull request. " +
                    "State parameter can be: 'open', 'closed', or 'all'. Defaults to 'open' if not specified."
    )
    public Mono<List<Map<String, Object>>> getAllPullRequests(
            @ToolParam(
                    description = "Filter pull requests by state: 'open', 'closed', or 'all'. Defaults to 'open'",
                    required = false
            ) String state) {

        long startTime = System.currentTimeMillis();
        logger.info("GitHub API: Fetching PRs state={}", state);

        // Set default state if not provided
        String prState = (state == null || state.isBlank()) ? "open" : state.toLowerCase();

        try {
            // Make synchronous blocking call for MCP compatibility
            Mono<List<JsonNode>> pullRequestsMono = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{owner}/{repo}/pulls")
                            .queryParam("state", prState)
                            .build(githubOwner, githubRepo))
                    .header("Authorization", "Bearer " + gitHubToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> {
                        logger.error("GitHub API error: {}", response.statusCode());
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    String errorMsg = "GitHub API error " + response.statusCode() + ": " + body;
                                    return reactor.core.publisher.Mono.error(new RuntimeException(errorMsg));
                                });
                    })
                    .bodyToFlux(JsonNode.class)
                    .collectList()
                    .timeout(TIMEOUT);

            return pullRequestsMono.map(pullRequests -> {
                long githubElapsed = System.currentTimeMillis() - startTime;
                if (pullRequests == null || pullRequests.isEmpty()) {
                    logger.info("No pull requests found for state: {}", prState);
                    logger.info("GitHub API: No PRs found, duration={}ms", githubElapsed);
                    return new ArrayList<>();
                }
                List<Map<String, Object>> result = new ArrayList<>();
                for (JsonNode pr : pullRequests) {
                    Map<String, Object> prMap = new HashMap<>();
                    prMap.put("number", pr.get("number").asInt());
                    prMap.put("title", pr.get("title").asText());
                    prMap.put("state", pr.get("state").asText());
                    prMap.put("author", pr.get("user").get("login").asText());
                    prMap.put("created_at", pr.get("created_at").asText());
                    prMap.put("url", pr.get("html_url").asText());
                    result.add(prMap);
                }

                long totalElapsed = System.currentTimeMillis() - startTime;
                logger.info("GitHub API: Successfully Retrieved {} PRs, api={}ms, total={}ms",
                        result.size(), githubElapsed, totalElapsed);
                return result;
            });

        } catch (Exception e) {
            logger.error("Error retrieving pull requests: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve pull requests: " + e.getMessage(), e);
        }
    }
}