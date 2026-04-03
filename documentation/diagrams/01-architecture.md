# System Architecture

## Level 1 — System Context

> Who uses the system and what external systems does it interact with?

```mermaid
graph TD
    Customer["🧑 Customer<br/>(Vehicle Owner)<br/>Web Browser"]
    Admin["👤 System Admin<br/>Web Browser"]
    PumpRep["🔧 Pump Representative<br/>Web Browser / Mobile"]

    System["Automated Fuel Quota<br/>Management System<br/>(Spring Boot 4 + React 18)"]

    PG[("🐘 PostgreSQL 15+<br/>Primary Database")]
    BRTA["🏛️ BRTA Registry<br/>(Future — not yet integrated)"]

    Customer -->|"Self-register, manage vehicles,<br/>generate QR code, view quota"| System
    Admin -->|"Manage vehicles, stations,<br/>quotas, pump reps, audit logs"| System
    PumpRep -->|"Login, scan QR / manual lookup,<br/>confirm fuel dispensed"| System

    System --> PG
    System -.->|"Future: vehicle ownership<br/>verification"| BRTA

    style System fill:#1565c0,color:#fff,stroke:#0d47a1
    style PG fill:#336791,color:#fff,stroke:#1a3a5c
    style BRTA fill:#9e9e9e,color:#fff,stroke:#616161,stroke-dasharray: 5 5
```

---

## Level 2 — Container Diagram

> What are the major deployable units and how do they communicate?

```mermaid
graph TD
    subgraph Browser["Browser (Customer / Admin)"]
        ReactApp["React 18 SPA<br/>TypeScript + Tailwind CSS<br/>Vite bundled"]
    end

    subgraph PumpBrowser["Browser (Pump Rep Portal)"]
        PumpPortal["React 18 — Pump Portal<br/>/pump/* routes<br/>html5-qrcode scanning"]
    end

    subgraph SpringBoot["Spring Boot 4.0.5 JAR — :8080"]
        StaticFiles["Static File Server<br/>SpaController<br/>(serves React build)"]
        JwtFilter["JWT Auth Filter<br/>Spring Security"]
        AuthAPI["Auth API<br/>/api/auth/*"]
        CustomerAPI["Customer API<br/>/api/customer/*<br/>(CUSTOMER role)"]
        AdminAPI["Admin API<br/>/api/admin/*<br/>(ADMIN role)"]
        PumpAPI["Pump API<br/>/api/pump/*<br/>(Public)"]
        PublicAPI["Public Reference API<br/>/api/public/*"]
        Scheduler["Quota Reset Scheduler<br/>@Scheduled cron"]
        ActuatorAPI["Spring Actuator<br/>/actuator/*"]
    end

    subgraph DataLayer["Data Layer"]
        PG[("PostgreSQL 15+<br/>Primary Store")]
    end

    ReactApp -->|"HTTPS REST/JSON<br/>+ JWT Bearer token"| JwtFilter
    PumpPortal -->|"HTTPS REST/JSON<br/>(no JWT — public)"| PumpAPI

    JwtFilter --> AuthAPI
    JwtFilter --> CustomerAPI
    JwtFilter --> AdminAPI
    JwtFilter --> PublicAPI

    AuthAPI --> PG
    CustomerAPI --> PG
    AdminAPI --> PG
    PumpAPI --> PG
    Scheduler --> PG

    StaticFiles -.->|"Serves at runtime"| ReactApp

    style SpringBoot fill:#1b5e20,color:#fff,stroke:#0a3d0a
    style DataLayer fill:#e8f5e9,stroke:#2e7d32
    style Browser fill:#e3f2fd,stroke:#1565c0
    style PumpBrowser fill:#fff3e0,stroke:#e65100
```

---

## Level 3 — Backend Component Diagram

> What are the internal components of the Spring Boot application?

```mermaid
graph TD
    subgraph Controllers["Controllers Layer (REST)"]
        AuthC["AuthController<br/>/api/auth/*"]
        CustomerC["CustomerController<br/>/api/customer/*"]
        AdminC["AdminController<br/>/api/admin/*"]
        PumpC["PumpController<br/>/api/pump/*"]
        RefC["ReferenceDataController<br/>/api/public/*"]
        SpaC["SpaController<br/>/** (SPA fallback)"]
    end

    subgraph Services["Service Layer (Business Logic)"]
        AuthSvc["AuthService<br/>Registration / Login / JWT"]
        CustomerSvc["CustomerService<br/>Vehicle + QR management"]
        VehicleSvc["VehicleService<br/>Vehicle CRUD + BRTA"]
        QuotaSvc["QuotaService<br/>Quota calc + reset scheduler"]
        PumpSvc["PumpService<br/>QR/manual auth + confirm"]
        StationSvc["FuelStationService<br/>Station CRUD"]
        RepSvc["PumpRepresentativeService<br/>Rep CRUD + login"]
        ClaimSvc["VehicleClaimService<br/>Claim workflow"]
        AuditSvc["AuditLogService<br/>Immutable audit trail"]
        StatsSvc["AdminStatsService<br/>Dashboard analytics"]
        QuotaCfgSvc["QuotaConfigService<br/>Global config CRUD"]
    end

    subgraph Security["Security Layer"]
        JwtProvider["JwtTokenProvider<br/>Sign / Validate tokens"]
        JwtFilter["JwtAuthFilter<br/>Inject userId/role into request"]
        SecurityCfg["SecurityConfig<br/>CORS / CSRF / route rules"]
    end

    subgraph Repositories["Repository Layer (Spring Data JPA)"]
        UserRepo["UserRepository"]
        VehicleRepo["VehicleRepository"]
        QuotaRepo["QuotaRepository<br/>(bulk reset query)"]
        TxRepo["TransactionRepository"]
        StationRepo["FuelStationRepository"]
        RepRepo["PumpRepresentativeRepository"]
        ClaimRepo["VehicleClaimRepository"]
        AuditRepo["AuditLogRepository"]
        QuotaCfgRepo["QuotaConfigRepository"]
    end

    subgraph External["External / Infrastructure"]
        PG[("PostgreSQL")]
    end

    AuthC --> AuthSvc
    CustomerC --> CustomerSvc
    CustomerC --> VehicleSvc
    AdminC --> VehicleSvc
    AdminC --> StationSvc
    AdminC --> RepSvc
    AdminC --> QuotaSvc
    AdminC --> ClaimSvc
    AdminC --> AuditSvc
    AdminC --> StatsSvc
    AdminC --> QuotaCfgSvc
    PumpC --> PumpSvc
    PumpC --> RepSvc
    RefC --> VehicleSvc

    AuthSvc --> JwtProvider
    AuthSvc --> UserRepo
    AuthSvc --> VehicleRepo
    AuthSvc --> QuotaSvc
    CustomerSvc --> VehicleRepo
    CustomerSvc --> JwtProvider
    PumpSvc --> JwtProvider
    PumpSvc --> VehicleRepo
    PumpSvc --> QuotaSvc
    PumpSvc --> TxRepo
    QuotaSvc --> QuotaRepo

    QuotaRepo --> PG
    VehicleRepo --> PG
    UserRepo --> PG
    TxRepo --> PG
    StationRepo --> PG
    RepRepo --> PG
    ClaimRepo --> PG
    AuditRepo --> PG
    QuotaCfgRepo --> PG

    JwtFilter --> JwtProvider
    SecurityCfg --> JwtFilter

    style Controllers fill:#1565c0,color:#fff
    style Services fill:#1b5e20,color:#fff
    style Security fill:#4a148c,color:#fff
    style Repositories fill:#e65100,color:#fff
    style External fill:#37474f,color:#fff
```

---

## Level 3 — Frontend Component Architecture

> How is the React SPA structured?

```mermaid
graph TD
    Router["React Router v6<br/>BrowserRouter"]

    subgraph PublicLayout["PublicLayout"]
        Landing["LandingPage<br/>/"]
        CLogin["CustomerLoginPage<br/>/login"]
        Register["CustomerRegisterPage<br/>/register"]
        ALogin["AdminLoginPage<br/>/admin/login"]
        NotFound["NotFoundPage<br/>*"]
    end

    subgraph CustomerLayout["CustomerLayout (CUSTOMER JWT required)"]
        CDash["CustomerDashboardPage<br/>/dashboard"]
        CVehicles["CustomerVehiclesPage<br/>/vehicles"]
        CQR["CustomerQRCodePage<br/>/qr-code"]
        CTx["CustomerTransactionsPage<br/>/transactions"]
        CClaims["CustomerClaimsPage<br/>/claims"]
    end

    subgraph AdminLayout["AdminLayout (ADMIN JWT required)"]
        ADash["AdminDashboardPage<br/>/admin/dashboard"]
        AVehicles["AdminVehiclesPage<br/>/admin/vehicles"]
        AClaims["AdminVehicleClaimsPage<br/>/admin/vehicle-claims"]
        AStations["AdminStationsPage<br/>/admin/stations"]
        AQuotas["AdminQuotasPage<br/>/admin/quotas"]
        AQuotaCfg["AdminQuotaConfigPage<br/>/admin/quota-config"]
        AReps["AdminPumpRepsPage<br/>/admin/pump-reps"]
        AAudit["AdminAuditLogsPage<br/>/admin/audit-logs"]
    end

    subgraph PumpLayout["PumpRepLayout (localStorage session)"]
        PLogin["PumpLoginPage<br/>/pump"]
        PScan["PumpScanPage<br/>/pump/scan"]
        PDispense["PumpDispensePage<br/>/pump/dispense"]
    end

    Router --> PublicLayout
    Router --> CustomerLayout
    Router --> AdminLayout
    Router --> PumpLayout

    style PublicLayout fill:#e3f2fd,stroke:#1565c0
    style CustomerLayout fill:#e8f5e9,stroke:#2e7d32
    style AdminLayout fill:#fce4ec,stroke:#c62828
    style PumpLayout fill:#fff3e0,stroke:#e65100
```

