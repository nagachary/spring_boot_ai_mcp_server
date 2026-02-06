package com.naga.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class APIConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(APIConfiguration.class);

    @Value("${github.api.basePath}")
    private String gitHubApiBasePath;

    @Value("${github.api.version}")
    private String apiVersion;

    @Value("${github.api.bearer.token}")
    private String bearerToken;

    @Bean("GITHUB_WEBCLIENT")
    public WebClient webClient() {
        logger.info("Initializing GitHub WebClient");

        return WebClient.builder()
                .baseUrl(gitHubApiBasePath)
                .defaultHeaders(header -> {
                    header.add(HttpHeaders.CONTENT_TYPE, "application/vnd.github+json");
                    header.add(HttpHeaders.ACCEPT, "application/vnd.github+json");
                    header.add("X-GitHub-Api-Version", apiVersion);
                    header.add("Authorization", "Bearer " + bearerToken);
                })
                .build();
    }
}