package com.naga.github;

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

import java.util.List;

@Service
public class GitHubToolsService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubToolsService.class);

    @Value("${github.owner}")
    private String githubOwner;

    @Value("${github.repo}")
    private String githubRepo;

    private final WebClient webClient;

    @Autowired
    public GitHubToolsService(@Qualifier("GITHUB_WEBCLIENT") WebClient webClient) {
        this.webClient = webClient;
    }

    @Tool(name = "getAllPullRequests", description = "Retrieves all the pull requests based in the pull request state")
    public Mono<List<String>> getAllPullRequests(@ToolParam(description = "state parameter to retrieve the pull requests based on their state", required = false) String state) {
        logger.info("getAllPullRequests");
        Mono<List<String>> objectFlux = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{owner}/{repo}/pulls")
                        .queryParam("state", state)
                        .build(githubOwner, githubRepo))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                Mono.error(new RuntimeException("GitHub API returned error: " + response.statusCode())))
                .bodyToFlux(String.class)
                .collectList();
        return objectFlux;
    }
}
