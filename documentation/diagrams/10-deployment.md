# Deployment Architecture

> Development and production deployment topologies for the Automated Fuel Quota Management System.

---

## Development Topology

> Two separate processes communicate over localhost. Hot Module Replacement (HMR)
> is active in the frontend; API requests are proxied to the backend.

```mermaid
graph TD
    Dev["👨‍💻 Developer Machine"]

    subgraph FE["Frontend Process — Vite Dev Server :5173"]
        Vite["Vite 5\nnpm run dev\nHMR enabled"]
        ViteProxy["Vite Proxy\n/api/* → localhost:8080"]
    end

    subgraph BE["Backend Process — Spring Boot :8080"]
        SB["Spring Boot 4.0.5\nmvn spring-boot:run\n(serves built static files too)"]
        SC["SecurityConfig\nCORS: allow localhost:5173"]
    end

    subgraph Data["Local Data Services"]
        PG[("PostgreSQL 15+\nlocalhost:5432\nautomated_fuel_quota DB")]
    end

    Dev -->|"http://localhost:5173"| Vite
    Vite -->|"browser loads"| ViteProxy
    ViteProxy -->|"proxy /api/*"| SB
    SB --> PG

    style FE fill:#e3f2fd,stroke:#1565c0
    style BE fill:#e8f5e9,stroke:#2e7d32
    style Data fill:#fff3e0,stroke:#e65100
```

### Development Commands

```bash
# Start backend (API only — no frontend hot reload)
mvn spring-boot:run

# Start frontend (hot reload — proxies API to :8080)
cd frontend && npm run dev

# Both together in separate terminals
# Frontend: http://localhost:5173
# API:       http://localhost:8080/api
```

---

## Production Build Pipeline

> The Maven build orchestrates the full-stack production build via `frontend-maven-plugin`.

```mermaid
flowchart LR
    Dev["mvn clean package"] --> FMP["frontend-maven-plugin\ninstalls Node.js if needed"]
    FMP --> NpmInstall["npm install\n(installs dependencies)"]
    NpmInstall --> ViteBuild["npm run build\n(Vite production build\nto frontend/dist/)"]
    ViteBuild --> CopyStatic["npm run copy-to-static\n(copies dist → src/main/resources/static/)"]
    CopyStatic --> MvnCompile["Maven compile + test"]
    MvnCompile --> MvnPackage["Maven package\n(Spring Boot JAR)"]
    MvnPackage --> JAR["target/automated-fuel-quota-*.jar\n(Self-contained JAR with embedded frontend)"]

    style JAR fill:#2e7d32,color:#fff
```

---

## Production Runtime Topology

> Single JAR serves both the API and the React SPA as static files.
> All traffic enters through port 8080.

```mermaid
graph TD
    Internet["🌐 Internet / Corporate Network"]

    subgraph LB["Load Balancer / Reverse Proxy (e.g. Nginx)"]
        Nginx["Nginx / AWS ALB\nTLS Termination\nHTTPS → HTTP"]
    end

    subgraph App["Application Server (JVM)"]
        SpringJAR["Spring Boot JAR\nPort 8080\nEmbedded Tomcat"]

        subgraph SpringInternal["Spring Boot — Internal"]
            StaticHandler["SpaController\n/** → index.html\n(React SPA)"]
            APIHandler["REST Controllers\n/api/**"]
            Security["Spring Security\nJWT Filter + CORS"]
            Scheduler["@Scheduled\nQuota Reset Cron"]
        end
    end

    subgraph DataTier["Data Tier"]
        PG[("PostgreSQL 15+\nPrimary Database\nPort 5432")]
    end

    subgraph Monitoring["Monitoring"]
        Actuator["Spring Actuator\n/actuator/health\n/actuator/metrics"]
    end

    Internet --> Nginx
    Nginx -->|"HTTPS → HTTP :8080"| SpringJAR
    SpringJAR --> StaticHandler
    SpringJAR --> APIHandler
    APIHandler --> Security
    SpringJAR --> Scheduler
    APIHandler --> PG
    Scheduler --> PG
    SpringJAR --> Actuator

    style App fill:#1b5e20,color:#fff
    style DataTier fill:#37474f,color:#fff
    style LB fill:#1565c0,color:#fff
    style Monitoring fill:#4a148c,color:#fff
```

### Production Run Command

```bash
# Start with production profile
java -jar target/automated-fuel-quota-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://<host>:5432/automated_fuel_quota \
  --spring.datasource.username=<user> \
  --spring.datasource.password=<pass> \
  --app.jwt.secret=<256-bit-secret>
```

---

## Container / Docker Topology (Example)

```mermaid
graph TD
    subgraph DockerNetwork["Docker Network: fuel-quota-network"]
        App["fuel-quota-app\nPort 8080:8080\nSpring Boot JAR"]
        DB["fuel-quota-db\nPort 5432:5432\nPostgreSQL 15"]
    end

    Host["Host Machine / CI/CD"]
    Host -->|"docker-compose up"| DockerNetwork
    App -->|"JDBC"| DB

    style App fill:#1b5e20,color:#fff
    style DB fill:#336791,color:#fff
```

---

## Required External Dependencies

| Service | Version | Role | Optional? |
|---------|---------|------|-----------|
| PostgreSQL | 15+ | Primary database | ❌ Required |
| JVM | Java 25 | Runtime | ❌ Required |
| Node.js | 20+ | Build only (not runtime) | ❌ Required for build |

---

## Monitoring Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Overall health (DB connectivity) |
| `GET /actuator/metrics` | JVM heap, GC, HTTP request rates |
| `GET /actuator/info` | Build version and artifact metadata |
| `GET /api/pump/health` | Pump API liveness check |

