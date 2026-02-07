# Spring Boot AI MCP Server

**[Java 21](https://www.oracle.com/java/)** | **[Spring Boot 3.4.2](https://spring.io/projects/spring-boot)** | **[Docker](https://www.docker.com/)** | **[MIT License](https://opensource.org/licenses/MIT)**

A production-ready **Model Context Protocol (MCP) Server** built with Spring Boot, featuring reactive architecture, JWT authentication with sliding expiration, and complete Docker containerization for seamless deployment.

## Features

### Core Capabilities
- **MCP Server-Sent Events (SSE)** transport for real-time AI model communication
- **GitHub API Integration** with reactive WebClient for non-blocking pull request management
- **Advanced Security**:
  - Dual-layer authentication (API Key + JWT)
  - Sliding window token refresh for continuous sessions
  - Stateless design enabling horizontal scaling
- **Reactive Architecture**: Built on Spring WebFlux for high-performance, non-blocking operations
- **Production Monitoring**: Actuator endpoints with health checks and metrics

### Docker Deployment
- **Fully Dockerized**: Single-command deployment with Docker Compose
- **Multi-stage Build**: Optimized image size with separated build and runtime environments
- **Environment-based Configuration**: Secure credential management via `.env` files
- **Zero Downtime**: Automatic restart policies and graceful shutdown handling
- **Portable**: Runs consistently across development, staging, and production environments

### Developer Experience
- **Professional Logging**: Structured request/response tracking with performance metrics
- **Error Handling**: Comprehensive error recovery with graceful degradation
- **Hot Reload**: Development mode with instant code changes (non-Docker)
- **Clean Architecture**: Separation of concerns with service, controller, and configuration layers

## Prerequisites

- **Docker & Docker Compose** (recommended)
  - Docker Engine 20.10+
  - Docker Compose 2.0+
- **OR for local development:**
  - Java 21+
  - Maven 3.9+

## Quick Start

### Option 1: Docker Deployment (Recommended)

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/spring_boot_ai_mcp_server.git
cd spring_boot_ai_mcp_server
```

2. **Configure environment variables**
```bash
cp .env.example .env
# Edit .env with your credentials
nano .env
```

3. **Build and run with Docker Compose**
```bash
docker-compose up -d --build
```

4. **Verify deployment**
```bash
docker-compose logs -f
curl http://localhost:8088/actuator/health
```

**That's it!** Your MCP server is running at `http://localhost:8088`

### Option 2: Local Development

1. **Set environment variables**
```bash
export GITHUB_API_BEARER_TOKEN=your_github_token
export MCP_SERVER_SHARED_API_KEY=your_api_key
export MCP_AUTH_JWT_SECRET=your_jwt_secret_min_32_chars
```

2. **Build and run**
```bash
mvn clean install
mvn spring-boot:run
```

## Configuration

### Environment Variables

Create a `.env` file in the project root:

```bash
# GitHub API Configuration
GITHUB_API_BEARER_TOKEN=ghp_your_github_personal_access_token

# MCP Server Authentication
MCP_SERVER_SHARED_API_KEY=your-secure-api-key
MCP_AUTH_JWT_SECRET=your-jwt-secret-at-least-32-characters-long
```

> **️ Security Note**: Never commit `.env` to version control. Use `.env.example` for templates.

### Application Properties

Key configurations in `application.properties`:

```properties
# Server
server.port=8088

# MCP Server
spring.ai.mcp.server.enabled=true
spring.ai.mcp.server.transport=sse

# GitHub Repository
github.owner=your-github-username
github.repo=your-repository-name

# JWT Configuration
mcp.auth.jwt-ttl-seconds=3600
mcp.auth.refresh-window-seconds=300
```

## Authentication Architecture

### Dual-Layer Security Model

This MCP server implements a **defense-in-depth** authentication strategy combining API Key and JWT token authentication:

```
┌─────────────────────────────────────────────────────────────────┐
│                     Authentication Flow                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Initial Authentication (API Key)                            │
│     Client → POST /mcp/auth/token                               │
│              Header: X-API-KEY: shared-secret                   │
│              ↓                                                  │
│     Server validates API key                                    │
│              ↓                                                  │
│     Server generates JWT token                                  │
│              ↓                                                  │
│     Response: { "accessToken": "eyJ..." }                       │
│                                                                 │
│  2. Subsequent Requests (JWT)                                   │
│     Client → POST /mcp/tools/call                               │
│              Header: Authorization: Bearer <jwt-token>          │
│              ↓                                                  │
│     Server validates JWT signature & expiration                 │
│              ↓                                                  │
│     Server processes request                                    │
│              ↓                                                  │
│     Server generates NEW JWT (sliding window)                   │
│              ↓                                                  │
│     Response: Data + Header: X-Refresh-Token: <new-jwt>         │
│                                                                 │
│  3. Token Refresh (Automatic)                                   │
│     Client stores new token from X-Refresh-Token header         │
│     Next request uses the refreshed token                       │
│     Active sessions never expire!                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Why Dual-Layer Authentication?

| Layer         | Purpose                   | Benefits                              |
|---------------|---------------------------|---------------------------------------|
| **API Key**   | Initial authentication    | Simple, revocable, shared across team |
| **JWT Token** | Per-request authorization | Stateless, scalable, auto-expiring    |

### Sliding Window Token Refresh

**Problem**: Fixed-expiration tokens require re-authentication, disrupting user experience.

**Solution**: Sliding window refresh keeps active sessions alive indefinitely.

**How it works:**
1. JWT issued with 1-hour expiration (configurable)
2. Every valid request within the refresh window (last 5 minutes) gets a new token
3. Client automatically uses the new token for subsequent requests
4. Inactive sessions naturally expire after 1 hour
5. Active sessions continue indefinitely without re-authentication

**Configuration:**
```properties
mcp.auth.jwt-ttl-seconds=3600           # Token lifetime: 1 hour
mcp.auth.refresh-window-seconds=300     # Refresh if <5 min remaining
```

### Security Properties

**Stateless**: No session storage, enables horizontal scaling  
**Auto-expiring**: Inactive sessions timeout automatically  
**Revocable**: Change JWT secret to invalidate all tokens  
**Replay-resistant**: Each token has unique issue timestamp  
**Zero-downtime rotation**: API keys can be changed without service restart

## Authentication Flow (Step-by-Step)

### 1. Obtain JWT Token

```bash
curl -X POST http://localhost:8088/mcp/auth/token \
  -H "X-API-KEY: your-api-key"
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2. Use Token for API Requests

```bash
curl http://localhost:8088/mcp/tools/call \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "getAllPullRequests",
    "arguments": {"state": "open"}
  }'
```

**Response:**
```json
HTTP/1.1 200 OK
X-Refresh-Token: refershedToken...  ← New token!

{
  "status": "success",
  "result": [...]
}
```

### 3. Automatic Token Refresh

The server returns a new token in the `X-Refresh-Token` header with each successful request within the refresh window. Active sessions never expire.

### 4. Token Expiration Handling

If a token expires (after 1 hour of inactivity), re-authenticate:

```bash
# Token expired (401 Unauthorized)
curl http://localhost:8088/mcp/tools/call \
  -H "Authorization: Bearer <expired-token>"

# Response:
{
  "error": "JWT token expired",
  "status": 401
}

# Re-authenticate
curl -X POST http://localhost:8088/mcp/auth/token \
  -H "X-API-KEY: your-api-key"
```

## API Endpoints

### Health & Monitoring
```bash
GET /actuator/health          # Health check
GET /actuator/info            # Application info
GET /actuator/metrics         # Performance metrics
```

### Authentication
```bash
POST /mcp/auth/token          # Obtain JWT token
  Header: X-API-KEY: <api-key>
```

### MCP Tools
```bash
POST /mcp/tools/call          # Execute MCP tools
  Header: Authorization: Bearer <jwt-token>
  Body: {
    "name": "getAllPullRequests",
    "arguments": {"state": "open|closed|all"}
  }
```

### SSE Stream
```bash
GET /mcp/sse                  # Server-Sent Events stream
  Header: Authorization: Bearer <jwt-token>
```


**Benefits:**
-  **Smaller Images**: Runtime image ~270MB (vs 500MB+ with build tools)
-  **Faster Deployments**: Optimized layer caching
-  **Security**: No build tools in production image
-  **Reproducibility**: Consistent builds across environments

### Docker Compose Management

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Rebuild after code changes
docker-compose up -d --build

# View running containers
docker-compose ps
```

##  Performance Optimizations

- **Reactive WebFlux**: Non-blocking I/O for high concurrency
- **Connection Pooling**: Efficient HTTP client connection reuse
- **Timeout Configuration**: Prevents resource exhaustion
- **Docker Layer Caching**: 3min → 30sec rebuild times
- **.dockerignore**: 655MB → 5MB build context (100x reduction)

**Measured Performance:**
- Server Processing: 500-800ms
- GitHub API Latency: 400-600ms
- Total Response Time: < 2 seconds (p95)

##  Monitoring & Logging

### Structured Logging

```
10:28:01.911 INFO  ▶ REQUEST: tool=getAllPullRequests, args={state=open}
10:28:02.450 INFO  → GitHub API: Fetching PRs state=open
10:28:02.996 INFO  ← GitHub API: Retrieved 24 PRs, api=546ms, total=546ms
10:28:02.996 INFO  ✓ SUCCESS: tool=getAllPullRequests, items=24, duration=1085ms
```

### Health Checks

```bash
# Application health
curl http://localhost:8088/actuator/health

# Detailed metrics
curl http://localhost:8088/actuator/metrics/http.server.requests
```

##  Deployment Scenarios

### Development
```bash
docker-compose up --build
```

### Staging
```bash
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d
```

### Production
```bash
# With custom environment
docker-compose -f docker-compose.prod.yml up -d

# Or with Kubernetes
kubectl apply -f k8s/deployment.yml
```

##  Security Best Practices

-  Environment-based secrets (never hardcoded)
-  `.env` excluded from version control
-  JWT with sliding expiration windows
-  API key authentication layer
-  HTTPS ready (configure reverse proxy)
-  Security headers enabled
-  Input validation on all endpoints

## Tech Stack

| Technology            | Purpose                            |
|-----------------------|------------------------------------|
| **Spring Boot 3.4.2** | Application framework              |
| **Spring WebFlux**    | Reactive web stack                 |
| **Spring Security**   | Authentication & authorization     |
| **JWT (jjwt 0.11.5)** | Token-based authentication         |
| **Spring AI MCP**     | Model Context Protocol integration |
| **WebClient**         | Non-blocking HTTP client           |
| **Actuator**          | Production monitoring              |
| **Docker**            | Containerization                   |
| **Maven**             | Build automation                   |

##  License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

##  Troubleshooting

### Docker Build Fails

**Issue:** `error: release version 21 not supported`

**Solution:** Ensure Dockerfile uses Java 21:
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
FROM eclipse-temurin:21-jre
```

### Port Already in Use

**Issue:** `Bind for 0.0.0.0:8088 failed: port is already allocated`

**Solution:** Change port in `docker-compose.yml`:
```yaml
ports:
  - "8089:8088"  # Change external port
```

### Environment Variables Not Loading

**Issue:** Application can't find environment variables

**Solution:** Verify `.env` file exists and restart:
```bash
docker-compose down
docker-compose up -d
```


---

**Made with using Spring Boot and Docker**
