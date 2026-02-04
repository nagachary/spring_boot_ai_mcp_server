package com.naga.github;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class TestToolService {

    @McpTool
    public String echo(String input) {
        return "Echo: " + input;
    }

    @McpTool
    public int add(int a, int b) {
        return a + b;
    }
}
