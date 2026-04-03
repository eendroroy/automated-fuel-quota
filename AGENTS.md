# AI Coding Agent Guide

## Architecture Overview

This is a **Spring Boot 4.0.5 + React 18 monorepo** implementing a fuel quota management system with embedded frontend. The backend serves as both API server and static file server, combining two deployment models:

- **Development**: Frontend runs on Vite dev server (localhost:5173) with API proxy to backend (localhost:8080)
- **Production**: Frontend builds into `src/main/resources/static/` and gets served by Spring Boot

## Documentation

- [`documentation/BRD.md`](documentation/BRD.md) — Business Requirements Document (authoritative requirements reference)
- [`documentation/SRS.md`](documentation/SRS.md) — Software Requirements Specification (API contracts, entity schemas, config reference)
- [`documentation/USER_JOURNEY.md`](documentation/USER_JOURNEY.md) — User journey maps for all actor types (Customer, Pump Rep, Admin, System)
- [`documentation/diagrams/README.md`](documentation/diagrams/README.md) — Index of all system diagrams

### Diagram Reference

| Diagram | File | Key Info |
|---------|------|----------|
| System Architecture | [`diagrams/01-architecture.md`](documentation/diagrams/01-architecture.md) | Context / Container / Component views |
| Entity Relationships | [`diagrams/02-entity-relationship.md`](documentation/diagrams/02-entity-relationship.md) | Full DB schema with all fields |
| QR Auth Sequence | [`diagrams/03-sequence-qr-authorization.md`](documentation/diagrams/03-sequence-qr-authorization.md) | Primary fuel dispense flow |
| Manual Auth Sequence | [`diagrams/04-sequence-manual-authorization.md`](documentation/diagrams/04-sequence-manual-authorization.md) | Fallback (no QR) dispense flow |
| Registration Sequence | [`diagrams/05-sequence-registration.md`](documentation/diagrams/05-sequence-registration.md) | Customer onboarding flow |
| Quota Reset Sequence | [`diagrams/06-sequence-quota-reset.md`](documentation/diagrams/06-sequence-quota-reset.md) | Scheduled reset job flow |
| State Machines | [`diagrams/07-state-diagrams.md`](documentation/diagrams/07-state-diagrams.md) | Vehicle / Quota / Claim / Rep lifecycle |
| Frontend Components | [`diagrams/08-component-diagram.md`](documentation/diagrams/08-component-diagram.md) | React SPA hierarchy and API clients |
| Use Cases | [`diagrams/09-use-case.md`](documentation/diagrams/09-use-case.md) | Actor–use case relationships |
| Deployment | [`diagrams/10-deployment.md`](documentation/diagrams/10-deployment.md) | Dev and prod deployment topologies |

## Critical Build & Development Workflows

### Full Stack Development
```bash
# Backend only (API + serves built frontend)
mvn spring-boot:run

# Frontend development (with hot reload + API proxy)
cd frontend && npm run dev

# Production build (frontend embedded in Spring Boot JAR)
mvn clean package  # Triggers frontend-maven-plugin
```

### Frontend Build Integration
The **frontend-maven-plugin** automatically handles Node.js installation and React build during Maven lifecycle. The `copy-to-static` npm script moves built assets to Spring Boot's static directory.

## Core Domain Models & Business Logic

### Quota System (Heart of the Application)
- **Configurable Periodic Limit**: Default 24L per week, configurable via admin UI and `app.quota.weekly-limit-litres`
- **Reset Schedule**: Configurable cron expression, default every Sunday 00:00 via `@Scheduled(cron = "${app.quota.reset-cron-expression}")`
- **Remaining Calculation**: `remaining = limit - used_this_period`
- **Partial Dispense**: When requested > remaining, authorize only available amount

### Vehicle Workflow States
```java
// Vehicle BRTA verification states
VERIFIED    → fuel dispensing allowed
UNVERIFIED  → dispensing denied (future: BRTA API check failed)
DEREGISTERED → soft-deleted; history preserved
```

### Vehicle Ownership Transfer (Claim Workflow)
1. Customer submits a claim (registration number + NID + reason).
2. Admin reviews and approves or rejects.
3. On approval: vehicle user, ownerName, ownerEmail, and **ownerNid** are updated to the claimant.

### QR Token Flow (Core BRD Implementation)
1. Customer generates QR token (JWT with 1-hour expiration).
2. Pump rep scans QR → calls `/api/pump/authorize`.
3. System validates: JWT → Vehicle status → GPS geofence → Quota availability.
4. Returns authorization decision with vehicle info, authorized liters, vehicle status, fuel type, and total quota.
5. After dispensing → `/api/pump/confirm` updates quota and records transaction.

### Manual Authorization Flow (Fallback)
1. Pump rep switches to "Enter Manually" tab on the scan page.
2. Rep types the vehicle registration number → calls `/api/pump/authorize-manual`.
3. System validates: Vehicle exists → Vehicle status → Quota availability (same checks as QR path, no JWT or geofence).
4. Returns same `AuthorizationResponse` shape.
5. After dispensing → `/api/pump/confirm` with `registrationNumber` instead of `qrToken` (idempotency check skipped).

### Pump Representative Portal Flow
1. Rep navigates to `/pump`, enters their **employee ID** → `POST /api/pump/login`.
2. Session saved to `localStorage` (`pumpRepSession` key) via helpers in `PumpRepLayout.tsx`.
3. Rep is shown scan page; tab switcher selects between camera QR scanner and manual entry.
4. After authorization → navigates to `/pump/dispense` with `AuthorizationResult` in router state.
5. Dispense page shows vehicle info panel + color-coded quota bar + fuel type dropdown + on-screen numeric keypad.
6. Rep confirms → `POST /api/pump/confirm` → receipt shown with transaction reference.

## Field Naming Conventions

> **Important**: The identifier field for vehicle owner and claimant identity documents uses **`Nid`** (National Identity Document), not `Nic`.

| Entity / DTO | Field Name |
|---|---|
| `Vehicle` entity | `ownerNid` |
| `RegisterCustomerRequest` | `ownerNid` |
| `AddVehicleRequest` | `ownerNid` |
| `VehicleResponse` | `ownerNid` |
| `VehicleClaim` entity | `claimantNid` |
| `ClaimVehicleRequest` | `claimantNid` |
| `VehicleClaimResponse` | `claimantNid` |
| TypeScript `Vehicle` interface | `ownerNid` |
| TypeScript `RegisterVehicleRequest` | `ownerNid` |
| TypeScript `AddVehicleRequest` | `ownerNid` |
| TypeScript `VehicleClaim` | `claimantNid` |
| TypeScript `ClaimVehicleRequest` | `claimantNid` |

## Project-Specific Patterns

### Project-Specific Patterns

### API Design Conventions
- **JWT with dual expiration**: 24h for app tokens, 1h for QR tokens
- **Role-based routing**: Customer/Admin layouts with `ProtectedRoute` components
- **Request attribute pattern**: JWT filter adds `userId`, `userEmail`, `userRole` to request for easy access in controllers
- **Pump rep portal**: No JWT — uses `localStorage` session object; pages self-redirect to `/pump` if session is absent

### API Design Conventions
- `/api/pump/*` - **Public endpoints** for pump representative portal (core BRD)
  - `POST /api/pump/login` — employee-ID login (no password in demo)
  - `POST /api/pump/authorize` — QR token authorization
  - `POST /api/pump/authorize-manual` — registration-number authorization (no QR)
  - `POST /api/pump/confirm` — dispense confirmation (`qrToken` optional; use `registrationNumber` for manual path)
- `/api/customer/*` - **CUSTOMER role required** (JWT protected)
- `/api/admin/*` - **ADMIN role required** (JWT protected)
- `/api/public/*` - **Public reference data** (registration codes, BRTA offices)

### Frontend Architecture
- **Layout-based routing**: `PublicLayout`, `CustomerLayout`, `AdminLayout`, `PumpRepLayout` with nested routes
- **API client pattern**: Centralized axios instance with interceptors for auth/errors; **separate** `pumpAxios` instance (no JWT) in `api/pumpApi.ts`
- **Type definitions**: Shared TypeScript interfaces in `frontend/src/types/index.ts`
- **Pump portal pages**: `pages/pump/PumpLoginPage.tsx`, `PumpScanPage.tsx`, `PumpDispensePage.tsx`

### `AuthorizationResponse` Shape (Updated)
The `AuthorizationResponse` DTO now includes:
```java
AuthorizationDecision decision     // APPROVED | PARTIAL | DENIED
BigDecimal authorizedLiters
BigDecimal remainingQuota
BigDecimal totalQuota              // NEW: periodic limit in litres
String message                     // deny reason or null
String vehicleFound                // registration number
String vehicleMake
String vehicleColor
String ownerName
String vehicleStatus               // NEW: VERIFIED | UNVERIFIED | DEREGISTERED
String fuelType                    // NEW: vehicle's registered fuel type
```

### `QuotaAuthorizationResult` (Updated)
Added `limitLiters` field to carry total quota to `PumpService`:
```java
public QuotaAuthorizationResult(decision, authorizedLiters, remainingQuota, limitLiters, denyReason)
```

### `DispenseConfirmationRequest` (Updated)
- `qrToken` is now **optional** (removed `@NotBlank`) — absent on manual authorization path.
- Added optional `registrationNumber` field — used on manual path to resolve the vehicle.
- Service logic: resolves vehicle from QR token when present, otherwise from `registrationNumber`; skips idempotency check when `qrToken` is blank.

### Frontend npm Dependencies Added
- **`html5-qrcode`** — Camera-based QR code scanning in the browser (`PumpScanPage.tsx`)

## Database & Entity Relationships

### Key Relationships
```java
User (1) -> (*) Vehicle -> (1) Quota
FuelStation (1) -> (*) Transaction
Vehicle (1) -> (*) Transaction
Vehicle (1) -> (*) VehicleClaim
User (1) -> (*) VehicleClaim (as claimant)
```

### Critical Indexes & Queries
- Vehicle lookup by `registration_number` (unique constraint)
- Vehicle lookup by `owner_nid` (unique constraint)
- Quota queries by `vehicle_id` and `vehicle.registration_number`
- Transaction history by `vehicle_id` with pagination
- Periodic quota reset uses bulk update query in `QuotaRepository`

## Configuration & Environment

### Development Database Setup
```sql
CREATE DATABASE automated_fuel_quota;
-- Default credentials: postgres/postgres (see application.yaml)
```

### Key Configuration Properties
```yaml
app.jwt.secret: # Long JWT signing secret
app.jwt.qr-expiration-ms: 3600000  # 1 hour QR tokens
app.quota.weekly-limit-litres: 24.0
app.quota.reset-cron-expression: "0 0 0 ? * SUN"
app.quota.geofence-radius-meters: 100
```

### Default Admin Account
- Email: `admin@fuelquota.gov`
- Password: `admin123`
- Created by `DataInitializer` on startup

## Testing & Quality Assurance

### Critical Test Scenarios
- **Quota authorization logic**: Partial dispense, geofencing, vehicle status checks
- **Periodic reset job**: Verify quota calculations and audit logging
- **Security**: JWT validation, role-based access, QR token expiration
- **Idempotency**: Duplicate transaction prevention in `/api/pump/confirm` (QR path only)
- **NID uniqueness**: Vehicle registration rejects duplicate `ownerNid`
- **Pump rep login**: Valid employee ID → 200 with session; invalid ID → 400
- **Manual authorization**: Valid reg number → same response shape as QR path; unknown → DENIED
- **Manual confirm**: No `qrToken` provided; `registrationNumber` used to resolve vehicle

### Error Handling Patterns
- Global exception handler returns structured JSON responses
- Service layer throws domain exceptions (ResourceNotFoundException, BadRequestException)
- Frontend shows toast notifications for all API errors

## Deployment & Production Considerations

### Single JAR Deployment
The application builds to a single executable JAR with embedded frontend. Set `spring.profiles.active=prod` and configure external database.

### Required External Dependencies
- **PostgreSQL 15+**: Primary database

### Monitoring Endpoints
Spring Boot Actuator exposes `/actuator/health`, `/actuator/metrics` for monitoring. Check quota reset job execution and database query performance.
