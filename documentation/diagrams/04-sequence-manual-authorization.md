# Manual Authorization Flow (Fallback Path)

> The fallback fuel dispensing sequence when the customer's QR code is unavailable
> (e.g. dead phone battery). The pump rep enters the vehicle registration number directly.
> Corresponds to BR-9, FR-14, FR-18, FR-19 (manual path).

---

## Key Differences vs QR Path

| Aspect | QR Path | Manual Path |
|--------|---------|-------------|
| Token validation | Required (JWT, 1-hour TTL) | Not required |
| Geofence check | Performed when GPS provided | **Skipped** |
| Idempotency | QR token hash checked | **Not checked** (rep responsibility) |
| Vehicle resolution | From JWT claims | From `registrationNumber` field |
| Endpoint | `POST /api/pump/authorize` | `POST /api/pump/authorize-manual` |
| Confirm payload | `qrToken` present | `registrationNumber` present, no `qrToken` |

---

## Authorization Sequence

```mermaid
sequenceDiagram
    actor Rep as 🔧 Pump Rep
    participant PumpPortal as React SPA<br/>(PumpScanPage — Manual Tab)
    participant PumpAPI as PumpController<br/>/api/pump/*
    participant PumpSvc as PumpService
    participant VehicleRepo as VehicleRepository
    participant QuotaSvc as QuotaService

    Rep->>PumpPortal: Clicks "Enter Manually" tab
    Rep->>PumpPortal: Types vehicle registration number<br/>(e.g. DHAKA METRO GA 11-1234)
    Rep->>PumpPortal: Clicks "Check Vehicle"

    PumpPortal->>PumpAPI: POST /api/pump/authorize-manual<br/>{ registrationNumber, stationId, requestedLiters? }
    PumpAPI->>PumpSvc: authorizeManual(registrationNumber, stationId, requestedLiters)

    Note over PumpSvc: No JWT validation on manual path

    PumpSvc->>VehicleRepo: findByRegistrationNumber(registrationNumber)
    alt Vehicle not found
        VehicleRepo-->>PumpSvc: empty
        PumpSvc-->>PumpAPI: AuthorizationResponse { decision: DENIED, message: "Vehicle not found" }
        PumpAPI-->>PumpPortal: DENIED response
        PumpPortal-->>Rep: ❌ "Vehicle not found"
    else Vehicle found
        VehicleRepo-->>PumpSvc: Vehicle
    end

    alt Vehicle status = DEREGISTERED
        PumpSvc-->>PumpAPI: DENIED — "Vehicle is DEREGISTERED"
    else Vehicle status = UNVERIFIED
        PumpSvc-->>PumpAPI: DENIED — "Vehicle is UNVERIFIED"
    end

    Note over PumpSvc: Geofence check SKIPPED on manual path

    PumpSvc->>QuotaSvc: getQuotaForVehicle(registrationNumber)
    QuotaSvc-->>PumpSvc: QuotaAuthorizationResult(decision, authorizedLiters, remaining, limit)

    Note over PumpSvc: Same decision logic as QR path:<br/>APPROVED / PARTIAL / DENIED

    PumpSvc-->>PumpAPI: AuthorizationResponse
    PumpAPI-->>PumpPortal: { decision, authorizedLiters, remainingQuota, totalQuota,<br/>vehicleFound, vehicleMake, vehicleColor, ownerName,<br/>vehicleStatus, fuelType, message }

    PumpPortal-->>Rep: Vehicle info panel + quota bar shown
    PumpPortal->>PumpPortal: Navigate to /pump/dispense (router state)
```

---

## Dispense Confirmation (Manual Path)

```mermaid
sequenceDiagram
    actor Rep as 🔧 Pump Rep
    participant PumpPortal as React SPA<br/>(PumpDispensePage)
    participant PumpAPI as PumpController<br/>/api/pump/*
    participant PumpSvc as PumpService
    participant VehicleRepo as VehicleRepository
    participant QuotaRepo as QuotaRepository
    participant TxRepo as TransactionRepository

    Rep->>PumpPortal: Enters dispensed liters via numeric keypad
    Rep->>PumpPortal: Selects fuel type from dropdown
    Rep->>PumpPortal: Taps "Confirm Dispense"

    PumpPortal->>PumpAPI: POST /api/pump/confirm<br/>{ registrationNumber, stationId, pumpRepresentativeId,<br/>  dispensedLiters, fuelType }<br/>[NO qrToken field]

    PumpAPI->>PumpSvc: confirmDispense(request)

    Note over PumpSvc: qrToken is absent (blank/null)<br/>→ Resolve vehicle from registrationNumber<br/>→ Skip idempotency check

    PumpSvc->>VehicleRepo: findByRegistrationNumber(registrationNumber)
    VehicleRepo-->>PumpSvc: Vehicle (with Quota)

    PumpSvc->>QuotaRepo: save(quota) — deduct dispensedLiters
    Note over QuotaRepo: usedLiters += dispensedLiters<br/>remainingLiters -= dispensedLiters

    PumpSvc->>TxRepo: save(Transaction)
    Note over TxRepo: geofenceVerified = false (manual path)<br/>latitude/longitude = null


    PumpSvc-->>PumpAPI: DispenseConfirmationResponse
    PumpAPI-->>PumpPortal: { transactionId, transactionReference,<br/>  dispensedLiters, remainingQuota, timestamp, message }

    PumpPortal-->>Rep: ✅ Receipt displayed
```

---

## Summary Flow

```mermaid
flowchart LR
    A([Rep switches to\nManual tab]) --> B[Types registration number]
    B --> C[POST /api/pump/authorize-manual]
    C --> D{Vehicle\nfound?}
    D -->|No| E([DENIED\nVehicle not found])
    D -->|Yes| F{Vehicle\nVERIFIED?}
    F -->|No| G([DENIED\nStatus reason])
    F -->|Yes| H{Quota\navailable?}
    H -->|0L remaining| I([DENIED])
    H -->|Partial| J([PARTIAL])
    H -->|Full| K([APPROVED])
    J --> L[Rep enters liters]
    K --> L
    L --> M[POST /api/pump/confirm\nno qrToken]
    M --> N([Transaction recorded\nReceipt shown])

    style E fill:#c62828,color:#fff
    style G fill:#c62828,color:#fff
    style I fill:#c62828,color:#fff
    style J fill:#f57f17,color:#fff
    style K fill:#2e7d32,color:#fff
    style N fill:#1565c0,color:#fff
```

