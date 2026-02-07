# GitHub Pull Requests MCP Server

A production-ready Model Context Protocol (MCP) server that provides AI models with secure, authenticated access to GitHub pull request data through a RESTful API.

## Overview

This Spring Boot application implements the MCP specification to expose GitHub repository operations as AI-accessible tools. It features enterprise-grade security with JWT authentication, reactive programming for optimal performance, and comprehensive error handling.

## Key Features

###  **Enterprise Security**
- **JWT Authentication**: Stateless token-based authentication with configurable expiration
- **Token Sliding Window**: Automatic token refresh on activity to maintain session continuity
- **Role-Based Access Control**: Spring Security integration with customizable authorization rules
- **Secure Credential Management**: Environment-based configuration for sensitive data

### **High Performance**
- **Reactive Architecture**: Built on Spring WebFlux for non-blocking, asynchronous operations
- **Sub-second Response Times**: Optimized GitHub API integration (~500ms average)
- **Async Request Processing**: Handles concurrent requests efficiently without thread blocking
- **Connection Pooling**: Optimized HTTP client configuration for GitHub API calls

### **MCP Tool Integration**
- **`getAllPullRequests`**: Retrieve pull requests filtered by state (open, closed, all)
    - Returns structured data: PR number, title, author, creation date, state, and URL
    - Configurable repository targeting via environment variables
    - Comprehensive error handling with detailed logging

### **Production-Ready Features**
- **Structured Logging**: Performance metrics tracking (API latency, request duration)
- **Health Monitoring**: Spring Actuator endpoints for system health checks
- **Timeout Management**: Configurable timeouts to prevent hanging requests
- **Error Recovery**: Graceful degradation with meaningful error messages

## Architecture
```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│  AI Client  │ ──JWT──>│  MCP Server  │ ──API──>│   GitHub    │
│  (Ollama)   │ <─JSON──│ (Spring Boot)│ <─JSON──│     API     │
└─────────────┘         └──────────────┘         └─────────────┘
                              │
                              ├─ JWT Auth Filter
                              ├─ Reactive Controller
                              ├─ GitHub Service Layer
                              └─ Security Configuration
```

## Technical Stack

- **Framework**: Spring Boot 3.x with WebFlux
- **Security**: Spring Security 6.x with JWT (jjwt 0.11.5)
- **HTTP Client**: Spring WebClient (reactive)
- **Build Tool**: Maven
- **Java Version**: 21

## Configuration

### Required Environment Variables
```bash
# JWT Configuration
JWT_SECRET=your-secret-key-here              # Secret for signing JWT tokens
MCP_AUTH_API_KEY=your-api-key               # Optional API key for additional auth

# GitHub Configuration
GITHUB_TOKEN=ghp_your_github_token_here     # GitHub Personal Access Token
GITHUB_OWNER=your-github-username           # Repository owner
GITHUB_REPO=your-repository-name            # Repository name
```

### Application Configuration

**`application.yml`:**
```yaml
server:
  port: 8088

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000  # 1 hour in milliseconds

github:
  api:
    bearer:
      token: ${GITHUB_TOKEN}
  owner: ${GITHUB_OWNER}
  repo: ${GITHUB_REPO}

logging:
  level:
    com.naga: INFO
    org.springframework.security: WARN
```

## API Endpoints

### Authentication
**POST** `/mcp/auth/token`

### MCP Tool Execution
**POST** `/mcp/tools/call`

### MCP Tool List
**POST** `/mcp/tools/list`

## Integration with AI Clients

### Example: Claude Desktop MCP Configuration

**`claude_desktop_config.json`:**
```json
{
  "mcpServers": {
    "github-prs": {
      "url": "http://localhost:8088/mcp/sse",
      "authToken": "your-jwt-token-here"
    }
  }
}
```

## Authentication Architecture

### Dual-Layer Security with Sliding Window Token Refresh

This MCP server implements a **hybrid authentication model** combining shared API key verification with stateless JWT tokens featuring intelligent sliding window expiration. This approach provides enterprise-grade security while maintaining seamless user experience through automatic session extension.

**Key Characteristics:**
- **Shared Secret Authentication**: Initial client verification via API key (`X-API-KEY` header)
- **Stateless JWT Authorization**: Per-request authentication without server-side session storage
- **Sliding Window Expiration**: Automatic token renewal on activity, eliminating manual re-authentication
- **Zero-Downtime Sessions**: Active users experience continuous authentication; inactive sessions expire naturally
- **Horizontal Scalability**: Stateless design enables multi-instance deployment without session synchronization

### Authentication Flow
```
┌─────────────────────────────────────────────────────────────────────┐
│                     AUTHENTICATION LIFECYCLE                        │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────┐                                    ┌──────────┐
    │  Client  │                                    │  Server  │
    └────┬─────┘                                    └────┬─────┘
         │                                               │
         │  ① POST /mcp/auth/token                      │
         │     Header: X-API-KEY: shared_secret          │
         ├──────────────────────────────────────────────>│
         │                                               │
         │                                               │ ② Validate API Key
         │                                               │ ③ Generate JWT
         │                                               │    TTL: 1 hour
         │  ④ 200 OK                                    │
         │     { accessToken: "eyJhbG..." }              │
         │<──────────────────────────────────────────────┤
         │                                               │
    ┌────┴─────┐                                    ┌────┴─────┐
    │  Store   │                                    │          │
    │   JWT    │                                    │          │
    └────┬─────┘                                    └────┬─────┘
         │                                               │
         │  ⑤ POST /mcp/tools/call                      │
         │     Authorization: Bearer eyJhbG...           │
         ├──────────────────────────────────────────────>│
         │                                               │
         │                                               │ ⑥ Validate JWT
         │                                               │ ⑦ Process Request
         │                                               │ ⑧ Generate NEW JWT
         │                                               │    (Sliding Window)
         │  ⑨ 200 OK + Data                             │
         │     Header: X-Refresh-Token: eyJnbW...        │
         │<──────────────────────────────────────────────┤
         │                                               │
    ┌────┴─────┐                                         │
    │  Update  │                                         │
    │   JWT    │  ◄─── Automatic Token Refresh           │
    └────┬─────┘                                         │
         │                                               │
         │  ⑩ Subsequent Requests (Repeat ⑤-⑨)         │
         │     Uses refreshed token                      │
         ├──────────────────────────────────────────────>│
         │                                               │
         ⋮                                               │
         │                                               │
    [Idle > 1 hour]                                      │
         │                                               │
         │  ⑪ POST /mcp/tools/call                      │
         │     Authorization: Bearer [expired]           │
         ├──────────────────────────────────────────────>│
         │                                               │
         │                                               │ ⑫ JWT Expired
         │  ⑬ 403 Forbidden                             │  
         │<──────────────────────────────────────────────┤
         │                                               │
         │  ⑭ Re-authenticate (Return to Step ①)        │
         │                                               │
```

### Sliding Window Mechanism
```
Time ──────────────────────────────────────────────────────────────>

Token A (Initial)
├────────────────────────┤  Expires at T+60min
│                        │
│  ↓ Request at T+30min  │
│                        │
├─ Token B (Refreshed) ──┼────────────────────────┤  Expires at T+90min
│                        │                        │
│                        │  ↓ Request at T+70min  │
│                        │                        │
│                        ├─ Token C (Refreshed) ──┼────────────────────────┤
│                        │                        │                        │
│                        │                        │  ↓ No activity         │
│                        │                        │                        │
│                        │                        │                    [EXPIRED]
│                        │                        │                        ×
│                        │                        │
│   Active Session       │    Active Session      │   Session Terminated
│   (Token renewed)      │    (Token renewed)     │   (Must re-authenticate)

```

### Security Benefits

| Aspect               | Traditional Session        | Sliding Window JWT      |
|----------------------|----------------------------|-------------------------|
| **Session Storage**  | Server-side (memory/DB)    | Stateless (client-side) |
| **Scalability**      | Requires sticky sessions   | Fully horizontal        |
| **Active User UX**   | Manual re-login required   | Seamless auto-renewal   |
| **Inactive Timeout** | Fixed expiration           | Automatic expiration    |
| **Token Compromise** | Valid until manual revoke  | Limited lifetime window |
| **Network Overhead** | Session lookup per request | Zero server-side lookup |

**Result**: Enterprise security with consumer-grade user experience — active users never see authentication prompts, while inactive sessions automatically expire for security.

## Performance Metrics

- **Average Response Time**: 500-800ms (including GitHub API call)
- **GitHub API Latency**: 400-600ms
- **Server Processing Overhead**: 50-100ms
- **Concurrent Request Support**: 100+ simultaneous connections

## Security Best Practices

1. **Never commit secrets** - Use environment variables or secret management systems
2. **Rotate GitHub tokens** regularly and use fine-grained permissions
3. **Use strong JWT secrets** - Minimum 256-bit random key
4. **Enable HTTPS** in production deployments
5. **Implement rate limiting** for public-facing deployments
6. **Monitor authentication failures** for potential security threats

## Error Handling

The server provides comprehensive error responses:

- **401 Unauthorized**: Invalid or expired JWT token
- **403 Forbidden**: Valid token but insufficient permissions
- **404 Not Found**: Unknown tool or endpoint
- **500 Internal Server Error**: GitHub API failures or server errors

All errors include descriptive messages for debugging.

## Logging

Structured logging with performance metrics:
```
10:28:01.911 INFO   REQUEST: tool=getAllPullRequests, args={state=closed}
10:28:02.450 INFO   GitHub API: Fetching PRs state=closed
10:28:02.996 INFO   GitHub API: Retrieved 24 PRs, api=546ms, total=546ms
10:28:02.996 INFO   SUCCESS: tool=getAllPullRequests, items=24, duration=1085ms
```

## Troubleshooting

### Common Issues

**JWT Signature Mismatch**
- Ensure client and server use identical `JWT_SECRET`
- Verify token hasn't been modified in transit

**GitHub API 401 Unauthorized**
- Confirm `GITHUB_TOKEN` is valid and not expired
- Verify token has `repo` scope permissions

**Async Dispatch Errors**
- Ensure `DispatcherType.ASYNC` is permitted in security config
- Check Spring Security filter chain configuration

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Built with️ using Spring Boot and Model Context Protocol**