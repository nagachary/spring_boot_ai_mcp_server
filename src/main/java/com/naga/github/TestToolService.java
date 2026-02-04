package com.naga.github;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class TestToolService {

    @McpTool(name="echo", description = "This will echo the input prompt")
    public String echo(String input) {
        return "Echo: " + input;
    }

    @McpTool(name="add", description = "This will add two input int values, please prompt two int values to perform addition")
    public int add(int a, int b) {
        return a + b;
    }
}
