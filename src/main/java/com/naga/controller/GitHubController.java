package com.naga.controller;

import com.naga.github.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/mcp/github/prs")
public class GitHubController {
    private static final Logger logger = LoggerFactory.getLogger(GitHubController.class);

    @Autowired
    private GitHubService gitHubService;

    @GetMapping
    public Mono<List<String>> getAllPRs(@RequestParam(name = "state") String state) {
        logger.info("getAllPRs");
        return gitHubService.getAllPullRequests(state);
    }
}
