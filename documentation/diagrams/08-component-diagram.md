# Frontend Component Diagram

> React 18 SPA structure — layouts, routing, pages, shared components, and API clients.

---

## Routing & Layout Hierarchy

```mermaid
graph TD
    App["App.tsx\nBrowserRouter + Routes"]

    subgraph Public["PublicLayout — unauthenticated"]
        L1["/ → LandingPage"]
        L2["/login → CustomerLoginPage"]
        L3["/register → CustomerRegisterPage"]
        L4["/admin/login → AdminLoginPage"]
        L5["* → NotFoundPage"]
    end

    subgraph Customer["CustomerLayout — ProtectedRoute CUSTOMER"]
        C1["/dashboard → CustomerDashboardPage"]
        C2["/vehicles → CustomerVehiclesPage"]
        C3["/qr-code → CustomerQRCodePage"]
        C4["/transactions → CustomerTransactionsPage"]
        C5["/claims → CustomerClaimsPage"]
    end

    subgraph Admin["AdminLayout — ProtectedRoute ADMIN"]
        A1["/admin/dashboard → AdminDashboardPage"]
        A2["/admin/vehicles → AdminVehiclesPage"]
        A3["/admin/vehicle-claims → AdminVehicleClaimsPage"]
        A4["/admin/stations → AdminStationsPage"]
        A5["/admin/quotas → AdminQuotasPage"]
        A6["/admin/quota-config → AdminQuotaConfigPage"]
        A7["/admin/pump-reps → AdminPumpRepsPage"]
        A8["/admin/audit-logs → AdminAuditLogsPage"]
    end

    subgraph Pump["PumpRepLayout — localStorage session guard"]
        P1["/pump → PumpLoginPage"]
        P2["/pump/scan → PumpScanPage"]
        P3["/pump/dispense → PumpDispensePage"]
    end

    App --> Public
    App --> Customer
    App --> Admin
    App --> Pump

    style App fill:#1565c0,color:#fff
    style Public fill:#e3f2fd,stroke:#1565c0
    style Customer fill:#e8f5e9,stroke:#2e7d32
    style Admin fill:#fce4ec,stroke:#c62828
    style Pump fill:#fff3e0,stroke:#e65100
```

---

## Shared Components

```mermaid
graph TD
    subgraph Common["components/common/"]
        PR["ProtectedRoute.tsx\nChecks AuthContext role\nRedirects to login if unauthorized"]
        LS["LoadingSpinner.tsx\nCentered spinner overlay"]
        MOD["Modal.tsx\nGeneric confirm/form modal"]
        PAG["Pagination.tsx\nPage navigation for paginated lists"]
        RNI["RegistrationNumberInput.tsx\n4-part BRTA format input component"]
        SC["StatsCard.tsx\nDashboard metric card with icon"]
        SB["StatusBadge.tsx\nColour-coded status pill (VERIFIED/ACTIVE/etc.)"]
    end

    CustomerLayout --> PR
    AdminLayout --> PR
    CustomerVehiclesPage --> RNI
    CustomerRegisterPage --> RNI
    AdminDashboardPage --> SC
    AdminVehiclesPage --> SB
    AdminQuotasPage --> PAG
    CustomerTransactionsPage --> PAG
    AdminVehicleClaimsPage --> MOD
    AdminStationsPage --> MOD
```

---

## Authentication Context & State

```mermaid
graph TD
    subgraph AuthContext["context/AuthContext.tsx"]
        AC["AuthContext\nProvides: user, token, login(), logout()"]
        LS2["localStorage\n'token' + 'user' keys"]
        AC <-->|"persist / hydrate"| LS2
    end

    App -->|"wrap with"| AC
    PR -->|"reads"| AC
    CustomerLayout -->|"reads user.name"| AC
    AdminLayout -->|"reads user.name"| AC
    CustomerDashboardPage -->|"reads user"| AC

    subgraph PumpSession["PumpRepLayout.tsx — Session Helpers"]
        GS["getPumpSession()"]
        SS["savePumpSession()"]
        CS["clearPumpSession()"]
        LS3["localStorage\n'pumpRepSession' key"]
        GS <-->|"read"| LS3
        SS -->|"write"| LS3
        CS -->|"delete"| LS3
    end

    PumpScanPage -->|"reads"| GS
    PumpDispensePage -->|"reads"| GS
    PumpLoginPage -->|"writes"| SS
```

---

## API Client Layer

```mermaid
graph TD
    subgraph APIClients["api/ — Axios Client Modules"]
        AX["axiosInstance.ts\nBase URL: /api\nInterceptor: inject Bearer JWT\nInterceptor: redirect 401 → /login"]
        PAX["pumpApi.ts\npumpAxios instance\nNo JWT header\nBase URL: /api/pump"]
    end

    subgraph APIMods["API Module Functions"]
        AuthAPI["authApi.ts\nloginCustomer()\nloginAdmin()\nregisterCustomer()"]
        VehicleAPI["vehicleApi.ts\ngetVehicles()\naddVehicle()\nderegisterVehicle()"]
        QuotaAPI["quotaApi.ts\ngetMyQuota()"]
        QRApi["(inline in vehicleApi)\ngetQrCode(vehicleId)\nregenerateQrCode(vehicleId)"]
        TxAPI["transactionApi.ts\ngetTransactions(page)"]
        ClaimAPI["vehicleClaimApi.ts\nsubmitClaim()\ngetMyClaims()\napprove()\nreject()"]
        StationAPI["stationApi.ts\ngetStations()\ncreateStation()\nupdateStation()\ndeleteStation()"]
        RepAPI["pumpRepApi.ts\ngetReps()\ncreateRep()\nupdateRep()\ndeleteRep()"]
        AuditAPI["auditApi.ts\ngetAuditLogs(filters)"]
        StatsAPI["adminStatsApi.ts\ngetAdminStats()"]
        QuotaCfgAPI["quotaConfigApi.ts\ngetConfig()\nupdateConfig()"]
        PumpRepAPI["pumpRepApi.ts (pump)\npumpRepLogin()\nauthorizeDispensing()\nauthorizeByRegistration()\nconfirmDispensing()"]
    end

    AX --> AuthAPI
    AX --> VehicleAPI
    AX --> QuotaAPI
    AX --> TxAPI
    AX --> ClaimAPI
    AX --> StationAPI
    AX --> RepAPI
    AX --> AuditAPI
    AX --> StatsAPI
    AX --> QuotaCfgAPI
    PAX --> PumpRepAPI

    style AX fill:#1565c0,color:#fff
    style PAX fill:#e65100,color:#fff
```

---

## Pump Portal Data Flow

```mermaid
graph LR
    PLogin["PumpLoginPage\n/pump"] -->|"POST /api/pump/login\nsavePumpSession()"| PumpSession["localStorage\npumpRepSession"]
    PSession -->|"getPumpSession()"| PScan
    PSession -->|"getPumpSession()"| PDispense
    PumpSession --> PScan

    PScan["PumpScanPage\n/pump/scan"] -->|"Tab: Camera"| Html5QR["html5-qrcode\nCamera Scanner"]
    PScan -->|"Tab: Manual"| ManualInput["Registration\nNumber Input"]

    Html5QR -->|"Decoded JWT"| AuthorizeQR["POST /api/pump/authorize"]
    ManualInput -->|"Reg number"| AuthorizeManual["POST /api/pump/authorize-manual"]

    AuthorizeQR -->|"AuthorizationResult\nin router state"| PDispense
    AuthorizeManual -->|"AuthorizationResult\nin router state"| PDispense

    PDispense["PumpDispensePage\n/pump/dispense"] -->|"POST /api/pump/confirm"| Receipt["Receipt Display\ntransactionReference\nremainingQuota"]
```

