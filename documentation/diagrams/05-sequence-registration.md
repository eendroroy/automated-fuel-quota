# Customer Registration & Vehicle Onboarding Flow

> New customer self-registration with vehicle details, resulting in a verified vehicle
> record, an active quota, and a JWT session token.
> Corresponds to FR-01, FR-02, BR-7, ATS-09.

---

## Full Registration Sequence

```mermaid
sequenceDiagram
    actor Customer as 🧑 Customer
    participant SPA as React SPA<br/>(CustomerRegisterPage)
    participant API as AuthController<br/>/api/auth/customer/register
    participant AuthSvc as AuthService
    participant UserRepo as UserRepository
    participant VehicleSvc as VehicleService
    participant VehicleRepo as VehicleRepository
    participant QuotaSvc as QuotaService
    participant QuotaRepo as QuotaRepository
    participant QuotaCfgRepo as QuotaConfigRepository
    participant JwtProvider as JwtTokenProvider

    Customer->>SPA: Fills multi-step registration form
    Note over SPA: Step 1: Personal info (name, NID, mobile, email, password)<br/>Step 2: Vehicle registration number (4-part BRTA format)<br/>Step 3: Vehicle details (make, color, fuel type, engine CC, reg date)

    Customer->>SPA: Submits form
    SPA->>API: POST /api/auth/customer/register<br/>RegisterCustomerRequest { ownerName, ownerNid, ownerMobile,<br/>  ownerEmail, password, brtaOfficeCode, vehicleRegistrationCode,<br/>  serialPart1, serialPart2, vehicleMake, vehicleColor,<br/>  fuelType, engineDisplacement?, registrationDate }

    API->>AuthSvc: register(request)

    AuthSvc->>UserRepo: existsByEmail(ownerEmail)
    alt Email already registered
        UserRepo-->>AuthSvc: true
        AuthSvc-->>API: 400 — "Email already registered"
        API-->>SPA: ErrorResponse
        SPA-->>Customer: ❌ "Email already in use"
    end

    AuthSvc->>VehicleRepo: existsByOwnerNid(ownerNid)
    alt NID already registered
        VehicleRepo-->>AuthSvc: true
        AuthSvc-->>API: 400 — "NID already registered"
        API-->>SPA: ErrorResponse
        SPA-->>Customer: ❌ "NID already in use"
    end

    AuthSvc->>VehicleRepo: existsByRegistrationNumber(assembledRegNumber)
    alt Registration number already exists
        VehicleRepo-->>AuthSvc: true
        AuthSvc-->>API: 400 — "Registration number already exists"
        API-->>SPA: ErrorResponse
        SPA-->>Customer: ❌ "Vehicle already registered"
    end

    AuthSvc->>AuthSvc: hashPassword(BCrypt)

    AuthSvc->>UserRepo: save(User { email, passwordHash, name, role=CUSTOMER })
    UserRepo-->>AuthSvc: savedUser

    AuthSvc->>VehicleSvc: createVehicle(request, savedUser)
    VehicleSvc->>VehicleRepo: save(Vehicle { registrationNumber, ownerNid,<br/>  vehicleMake, fuelType, status=VERIFIED, ... })
    Note over VehicleRepo: Vehicle is auto-set to VERIFIED on creation<br/>(BRTA live integration is future scope)
    VehicleRepo-->>VehicleSvc: savedVehicle

    AuthSvc->>QuotaSvc: createQuota(savedVehicle)
    QuotaSvc->>QuotaCfgRepo: findByConfigKey("DEFAULT")
    QuotaCfgRepo-->>QuotaSvc: QuotaConfig { limitLitres=24, period=WEEKLY, ... }
    QuotaSvc->>QuotaRepo: save(Quota { vehicle, limitLiters=24, usedLiters=0,<br/>  remainingLiters=24, status=ACTIVE, period=WEEKLY })
    QuotaRepo-->>QuotaSvc: savedQuota

    AuthSvc->>JwtProvider: generateToken(userId, userEmail, role=CUSTOMER)
    Note over JwtProvider: 24-hour app JWT token
    JwtProvider-->>AuthSvc: appJwt

    AuthSvc-->>API: AuthResponse { token, user: { id, email, name, role } }
    API-->>SPA: 201 Created — AuthResponse
    SPA-->>Customer: ✅ Logged in — redirected to /dashboard
```

---

## Add Additional Vehicle (Existing Customer)

```mermaid
sequenceDiagram
    actor Customer as 🧑 Customer
    participant SPA as React SPA<br/>(CustomerVehiclesPage)
    participant API as CustomerController<br/>/api/customer/vehicles
    participant VehicleSvc as VehicleService
    participant QuotaSvc as QuotaService

    Customer->>SPA: Clicks "Add Vehicle"
    Customer->>SPA: Fills vehicle form (reg number, details)
    SPA->>API: POST /api/customer/vehicles<br/>Authorization: Bearer {appJwt}<br/>AddVehicleRequest { brtaOfficeCode, vehicleRegistrationCode,<br/>  serialPart1, serialPart2, vehicleMake, vehicleColor, fuelType, ... }

    API->>VehicleSvc: addVehicle(request, userId)
    VehicleSvc->>VehicleSvc: validate uniqueness (registrationNumber, NID optional)
    VehicleSvc->>VehicleSvc: save Vehicle (status=VERIFIED, linked to user)
    VehicleSvc->>QuotaSvc: createQuota(newVehicle)
    VehicleSvc-->>API: VehicleResponse

    API-->>SPA: 201 Created — VehicleResponse
    SPA-->>Customer: ✅ New vehicle appears in list
```

---

## Registration Form Validation Rules

| Field | Validation |
|-------|-----------|
| `ownerName` | Required, max 100 chars |
| `ownerNid` | Required, max 20 chars, must be unique |
| `ownerEmail` | Required, valid email format, must be unique |
| `password` | Required, minimum 8 characters |
| `brtaOfficeCode` | Required, must exist in `BRTA_OFFICE` reference data |
| `vehicleRegistrationCode` | Required, must exist in `REGISTRATION_CODE` reference data |
| `serialPart1` | Required, numeric |
| `serialPart2` | Required, numeric |
| `vehicleMake` | Required, max 50 chars |
| `vehicleColor` | Required, max 30 chars |
| `fuelType` | Required (Petrol / Diesel / CNG / LPG / Electric) |
| `registrationDate` | Required, valid date, not future |

---

## Summary Flow

```mermaid
flowchart TD
    A([Customer fills form]) --> B{Email unique?}
    B -->|No| C([❌ Error])
    B -->|Yes| D{NID unique?}
    D -->|No| C
    D -->|Yes| E{Reg number unique?}
    E -->|No| C
    E -->|Yes| F[Save User]
    F --> G[Save Vehicle\nstatus=VERIFIED]
    G --> H[Create Quota\nlimit=24L ACTIVE]
    H --> I[Generate App JWT\n24-hour token]
    I --> J([✅ Logged in\nRedirect to /dashboard])

    style C fill:#c62828,color:#fff
    style J fill:#2e7d32,color:#fff
```

