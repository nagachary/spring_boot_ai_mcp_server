package com.naga.controller;

import com.naga.security.JwtUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mcp/auth")
public class AuthTokenGenerationController {
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenGenerationController.class);

    private final String REQUEST_HEADER_X_API_KEY = "X-API-KEY";

    @Value("${mcp.auth.api-key}")
    private String expectedApiKey;

    @Value("${mcp.auth.client.subject}")
    private String clientSubject;

    private final JwtUtility jwtUtility;

    public AuthTokenGenerationController(JwtUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> issueToken(
            @RequestHeader(REQUEST_HEADER_X_API_KEY) String apiKey) {
        logger.info("issueToken method : ");

        if (!expectedApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtUtility.generateToken(clientSubject);
        return ResponseEntity.ok(Map.of("accessToken", token));
    }
}
