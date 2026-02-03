package com.naga.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GitHubService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    @Value("${github.owner}")
    private String githubOwner;

    @Value("${github.repo}")
    private String githubRepo;

    private final WebClient webClient;

    @Autowired
    public GitHubService(@Qualifier("GITHUB_WEBCLIENT") WebClient webClient) {
        this.webClient = webClient;
    }

    @Tool(name = "getAllPullRequests", description = "Retrieves all the pull requests based in the pull request state")
    public Mono<List<String>> getAllPullRequests(@ToolParam(description = "state parameter to retrieve the pull requests based on their state", required = false) String state) {
        logger.info("getAllPullRequests");
        Mono<List<String>> objectFlux = webClient.get().uri(githubOwner + "/" + githubRepo + "/pulls?state=" + state).retrieve().bodyToFlux(String.class).collectList();
        return objectFlux;
    }
}
