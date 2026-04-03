# QR Authorization Flow (Primary Path)

> The primary fuel dispensing sequence using a JWT-based QR token.
> Corresponds to business rules BR-1 through BR-5 and FR-11 through FR-20.

---

## Phase 1 — QR Token Generation (Customer App)

```mermaid
sequenceDiagram
    actor Customer as 🧑 Customer
    participant CustomerApp as React SPA<br/>(Customer Portal)
    participant CustomerAPI as CustomerController<br/>/api/customer/*
    participant CustomerSvc as CustomerService
    participant JwtProvider as JwtTokenProvider
    participant VehicleRepo as VehicleRepository

    Customer->>CustomerApp: Opens QR Code page
    CustomerApp->>CustomerAPI: GET /api/customer/vehicles/{vehicleId}/qr-code<br/>Authorization: Bearer {app-jwt}
    CustomerAPI->>CustomerSvc: generateQrToken(vehicleId, userId)
    CustomerSvc->>VehicleRepo: findByIdAndUserId(vehicleId, userId)
    VehicleRepo-->>CustomerSvc: Vehicle (VERIFIED)
    CustomerSvc->>JwtProvider: generateQrToken(vehicleId, registrationNumber)
    Note over JwtProvider: Signs JWT with 1-hour TTL<br/>Claims: vehicleId, registrationNumber
    JwtProvider-->>CustomerSvc: qrToken (JWT string)
    CustomerSvc-->>CustomerAPI: QrTokenResponse
    CustomerAPI-->>CustomerApp: { token, vehicleId, registrationNumber, expiresInSeconds: 3600 }
    CustomerApp-->>Customer: QR Code displayed (encoded JWT)
```

---

## Phase 2 — Pump Rep Login

```mermaid
sequenceDiagram
    actor Rep as 🔧 Pump Rep
    participant PumpPortal as React SPA<br/>(Pump Portal)
    participant PumpAPI as PumpController<br/>/api/pump/*
    participant RepSvc as PumpRepresentativeService
    participant RepRepo as PumpRepRepository
    participant localStorage as localStorage

    Rep->>PumpPortal: Navigates to /pump
    Rep->>PumpPortal: Enters Employee ID (e.g. EMP-001)
    PumpPortal->>PumpAPI: POST /api/pump/login<br/>{ employeeId: "EMP-001" }
    PumpAPI->>RepSvc: login(employeeId)
    RepSvc->>RepRepo: findByEmployeeId("EMP-001")
    RepRepo-->>RepSvc: PumpRepresentative (ACTIVE)
    RepSvc->>RepRepo: save (update lastLoginTimestamp)
    RepSvc-->>PumpAPI: PumpRepLoginResponse
    PumpAPI-->>PumpPortal: { id, name, employeeId, stationId, stationName, stationCode }
    PumpPortal->>localStorage: savePumpSession(session)
    PumpPortal-->>Rep: Redirected to /pump/scan
```

---

## Phase 3 — QR Scan & Authorization

```mermaid
sequenceDiagram
    actor Rep as 🔧 Pump Rep
    participant PumpPortal as React SPA<br/>(Pump Portal — PumpScanPage)
    participant html5QR as html5-qrcode<br/>(Camera Scanner)
    participant PumpAPI as PumpController<br/>/api/pump/*
    participant PumpSvc as PumpService
    participant JwtProvider as JwtTokenProvider
    participant VehicleRepo as VehicleRepository
    participant QuotaSvc as QuotaService

    Rep->>PumpPortal: Clicks "Scan QR" tab
    PumpPortal->>html5QR: Start camera scanner
    Rep->>html5QR: Points camera at Customer QR code
    html5QR-->>PumpPortal: Decoded QR text (JWT string)

    PumpPortal->>PumpAPI: POST /api/pump/authorize<br/>{ qrToken, stationId, requestedLiters? }
    PumpAPI->>PumpSvc: authorize(qrToken, stationId, requestedLiters)

    PumpSvc->>JwtProvider: validateQrToken(qrToken)
    alt Token invalid or expired
        JwtProvider-->>PumpSvc: throws InvalidJwtException
        PumpSvc-->>PumpAPI: DENIED — "Invalid or expired QR token"
        PumpAPI-->>PumpPortal: AuthorizationResponse { decision: DENIED }
        PumpPortal-->>Rep: ❌ Error displayed
    else Token valid
        JwtProvider-->>PumpSvc: { vehicleId, registrationNumber }
    end

    PumpSvc->>VehicleRepo: findByRegistrationNumber(registrationNumber)
    alt Vehicle not found
        PumpSvc-->>PumpAPI: DENIED — "Vehicle not found"
    else Vehicle found
        VehicleRepo-->>PumpSvc: Vehicle
    end

    alt Vehicle status ≠ VERIFIED
        PumpSvc-->>PumpAPI: DENIED — "Vehicle is {status}"
    end

    PumpSvc->>PumpSvc: checkGeofence(repLatitude, repLongitude, station)
    Note over PumpSvc: Haversine distance formula<br/>Must be ≤ geofenceRadiusMeters<br/>(skipped if coords not provided)
    alt GPS outside geofence
        PumpSvc-->>PumpAPI: DENIED — "Outside geofence"
    end

    PumpSvc->>QuotaSvc: getQuotaForVehicle(registrationNumber)
    QuotaSvc->>VehicleRepo: load quota via vehicle
    QuotaSvc-->>PumpSvc: QuotaAuthorizationResult

    Note over PumpSvc: Authorization Decision:<br/>remaining ≥ requested → APPROVED<br/>0 < remaining < requested → PARTIAL<br/>remaining = 0 → DENIED

    PumpSvc-->>PumpAPI: AuthorizationResponse
    PumpAPI-->>PumpPortal: { decision, authorizedLiters, remainingQuota, totalQuota,<br/>vehicleFound, vehicleMake, vehicleColor, ownerName,<br/>vehicleStatus, fuelType, message }

    PumpPortal-->>Rep: Vehicle info panel + quota bar shown
    PumpPortal->>PumpPortal: Navigate to /pump/dispense (router state)
```

---

## Phase 4 — Dispense Confirmation

```mermaid
sequenceDiagram
    actor Rep as 🔧 Pump Rep
    participant PumpPortal as React SPA<br/>(PumpDispensePage)
    participant PumpAPI as PumpController<br/>/api/pump/*
    participant PumpSvc as PumpService
    participant JwtProvider as JwtTokenProvider
    participant VehicleRepo as VehicleRepository
    participant QuotaRepo as QuotaRepository
    participant TxRepo as TransactionRepository

    Rep->>PumpPortal: Views vehicle info panel + quota bar
    Rep->>PumpPortal: Enters dispensed liters via numeric keypad
    Rep->>PumpPortal: Selects fuel type from dropdown
    Rep->>PumpPortal: Taps "Confirm Dispense"

    PumpPortal->>PumpAPI: POST /api/pump/confirm<br/>{ qrToken, stationId, pumpRepresentativeId,<br/>  dispensedLiters, fuelType }

    PumpAPI->>PumpSvc: confirmDispense(request)

    PumpSvc->>JwtProvider: validateQrToken(qrToken)
    JwtProvider-->>PumpSvc: { vehicleId, registrationNumber }

    PumpSvc->>TxRepo: existsByQrTokenHash(hash(qrToken))
    alt Duplicate QR confirm
        TxRepo-->>PumpSvc: true
        PumpSvc-->>PumpAPI: 400 — "Transaction already recorded"
        PumpAPI-->>PumpPortal: Error response
        PumpPortal-->>Rep: ❌ "Already dispensed for this QR"
    end

    PumpSvc->>VehicleRepo: findByRegistrationNumber(registrationNumber)
    VehicleRepo-->>PumpSvc: Vehicle (with Quota)

    PumpSvc->>QuotaRepo: save(quota) — deduct dispensedLiters
    Note over QuotaRepo: usedLiters += dispensedLiters<br/>remainingLiters -= dispensedLiters

    PumpSvc->>TxRepo: save(Transaction)
    Note over TxRepo: Records: vehicleId, stationId, repId,<br/>amountDispensed, fuelType,<br/>quotaBefore, quotaAfter,<br/>geofenceVerified, lat/lon, status=COMPLETED


    PumpSvc-->>PumpAPI: DispenseConfirmationResponse
    PumpAPI-->>PumpPortal: { transactionId, transactionReference,<br/>  dispensedLiters, remainingQuota, timestamp, message }

    PumpPortal-->>Rep: ✅ Receipt displayed
    Note over PumpPortal: Shows: Transaction ref #,<br/>Dispensed litres, Remaining quota
```

---

## Summary Flow (End-to-End)

```mermaid
flowchart LR
    A([Customer opens app]) --> B[GET QR token\n/api/customer/vehicles/id/qr-code]
    B --> C([QR code displayed])
    C --> D([Rep scans QR])
    D --> E[POST /api/pump/authorize\nQR token + station GPS]
    E --> F{Validation\nChecks}
    F -->|JWT invalid| G([DENIED])
    F -->|Not VERIFIED| G
    F -->|Geofence fail| G
    F -->|Quota = 0| G
    F -->|Quota partial| H([PARTIAL])
    F -->|All pass| I([APPROVED])
    H --> J[Rep enters liters]
    I --> J
    J --> K[POST /api/pump/confirm]
    K --> L([Transaction recorded\nReceipt shown])

    style G fill:#c62828,color:#fff
    style H fill:#f57f17,color:#fff
    style I fill:#2e7d32,color:#fff
    style L fill:#1565c0,color:#fff
```

