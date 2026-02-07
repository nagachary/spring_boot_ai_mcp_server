package com.naga.controller;

import com.naga.github.GitHubToolsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github/prs")
public class GitHubController {
    private static final Logger logger = LoggerFactory.getLogger(GitHubController.class);

    private final GitHubToolsService gitHubService;

    @Autowired
    public GitHubController(GitHubToolsService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping
    public Mono<List<Map<String, Object>>> getAllPRs(
            @RequestParam(name = "state", required = false, defaultValue = "open") String state) {

        logger.info("REST API: getAllPRs called with state={}", state);

        return gitHubService.getAllPullRequests(state)
                .doOnSuccess(prs -> logger.info("REST API: Retrieved {} pull requests", prs.size()))
                .doOnError(error -> logger.error("REST API: Error - {}", error.getMessage()));
    }
}