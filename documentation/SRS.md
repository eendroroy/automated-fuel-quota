# Software Requirements Specification (SRS)
## Automated Fuel Quota Management System

**Document Version:** 2.5  
**Date:** 2026-04-05  
**Status:** Approved  
**Based on:** BRD v2.4 (removes claim-and-approval workflow; ownership transfer is now BRTA-driven and automatic)

---

## Document Navigation

| Document | Description |
|----------|-------------|
| [`BRD.md`](BRD.md) | Business Requirements Document — requirements and business rules |
| **This file** — SRS | Technical specifications, API contracts, entity schemas |
| [`USER_JOURNEY.md`](USER_JOURNEY.md) | Detailed user journey maps for all actor types |

---

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional software requirements for the Automated Fuel Quota Management System. It serves as the authoritative reference for development, testing, and validation.

### 1.2 Scope
The system is a **Spring Boot 4.0.5 + React 18 monorepo** that implements a QR-code-driven fuel quota management platform. It comprises:
- A **RESTful backend API** built with Spring Boot (Java 25).
- A **single-page frontend application** built with React 18 and TypeScript.
- A **PostgreSQL 15+** primary database.

### 1.3 Definitions and Abbreviations
| Term | Definition |
|------|------------|
| SPA | Single-Page Application |
| JWT | JSON Web Token |
| NID | National Identity Document |
| BRTA | Bangladesh Road Transport Authority |
| QR | Quick Response (code) |
| API | Application Programming Interface |
| DTO | Data Transfer Object |
| TTL | Time-To-Live |
| BCrypt | Password hashing algorithm |
| RBAC | Role-Based Access Control |

### 1.4 References
- [`documentation/BRD.md`](BRD.md) — Business Requirements Document v2.3
- [`documentation/USER_JOURNEY.md`](USER_JOURNEY.md) — User Journey Maps
- `src/main/resources/application.yaml` — Application configuration
- `AGENTS.md` — AI agent coding guidelines

---

## 2. System Overview

### 2.1 Architecture


```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │ Security │  │Controllers│  │ Services │  │Repositories│  │
│  │ (JWT)    │  │  (REST)  │  │(Business)│  │   (JPA)   │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────┘  │
│                                                   ↕          │
│                                           ┌──────────────┐  │
│                                           │  PostgreSQL   │  │
│                                           │   Database    │  │
│                                           └──────────────┘  │
└─────────────────────────────────────────────────────────────┘
         ↑ REST API               ↑ Static Files
┌─────────────────┐     ┌──────────────────────────────┐
│  React 18 SPA   │     │   React 18 SPA (embedded)    │
│(Customer/Admin) │     │  Pump Rep Portal (/pump/*)   │
└─────────────────┘     └──────────────────────────────┘
```

### 2.2 Deployment Model
- **Development**: Frontend on Vite dev server (`:5173`) with proxy to backend (`:8080`).
- **Production**: Frontend built into `src/main/resources/static/` and served by Spring Boot as a single JAR.

---

## 3. System Entities


### 3.1 Entity Relationship
```
User (1) ─────────── (*) Vehicle (1) ─────── (1) Quota
                           │
                           └── (*) Transaction
FuelStation (1) ──── (*) Transaction
FuelStation (1) ──── (*) PumpRepresentative
QuotaConfigSet (1) ── (*) registrationCodes (element collection)
```

### 3.2 Entity Descriptions

#### User
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Auto-generated |
| email | String(100) | UNIQUE, NOT NULL | Login identifier |
| password | String | NOT NULL | BCrypt-hashed |
| name | String(100) | NOT NULL | Display name |
| role | Enum | NOT NULL | `CUSTOMER` or `ADMIN` |
| mobileNumber | String(15) | nullable | For future OTP verification |
| enabled | Boolean | NOT NULL, default true | Account active flag |

#### Vehicle
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Auto-generated |
| registrationNumber | String(50) | UNIQUE, NOT NULL | Assembled from 4 parts |
| brtaOfficeCode | String(50) | NOT NULL | BRTA region code |
| vehicleRegistrationCode | String(10) | NOT NULL | Category code (e.g. GA) |
| ownerName | String(100) | NOT NULL | Legal name of owner |
| ownerNid | String(20) | NOT NULL | National Identity Document number |
| ownerMobile | String(15) | NOT NULL | Contact number |
| ownerEmail | String(100) | NOT NULL | Email address |
| vehicleMake | String(50) | NOT NULL | Manufacturer |
| vehicleColor | String(30) | NOT NULL | Body colour |
| vehicleClass | String(100) | NOT NULL | Regulatory class from BRTA code |
| fuelType | String(30) | NOT NULL | e.g. Petrol, Diesel, CNG |
| engineDisplacement | Integer | nullable | CC displacement |
| registrationDate | LocalDate | NOT NULL | Official BRTA registration date |
| status | Enum | NOT NULL | `VERIFIED`, `UNVERIFIED`, `DEREGISTERED` |
| user_id | UUID | FK(User) | Owning user account |

#### Quota
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Auto-generated |
| vehicle_id | UUID | FK(Vehicle), UNIQUE | One quota per vehicle |
| limitLiters | Decimal(10,2) | NOT NULL | Maximum litres per period |
| usedLiters | Decimal(10,2) | NOT NULL, default 0 | Litres used this period |
| remainingLiters | Decimal(10,2) | NOT NULL | `limit - used` |
| period | Enum | NOT NULL | `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |
| resetTimestamp | LocalDateTime | NOT NULL | Next scheduled reset time |
| lastTransactionTimestamp | LocalDateTime | nullable | Timestamp of most recent dispense |
| status | Enum | NOT NULL | `ACTIVE`, `SUSPENDED`, `EXPIRED` |

#### Transaction
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Auto-generated |
| vehicle_id | UUID | FK(Vehicle) | Vehicle that received fuel |
| station_id | UUID | FK(FuelStation) | Station where fuel was dispensed |
| pumpRepresentativeId | UUID | NOT NULL | Rep who performed the dispense |
| amountDispensedLiters | Decimal(10,2) | NOT NULL | Actual litres dispensed |
| fuelTypeDispensed | String(30) | NOT NULL | Fuel type dispensed |
| quotaBefore | Decimal(10,2) | NOT NULL | Remaining quota before dispense |
| quotaAfter | Decimal(10,2) | NOT NULL | Remaining quota after dispense |
| transactionTimestamp | LocalDateTime | NOT NULL | When dispense occurred |
| geofenceVerified | Boolean | NOT NULL | Whether GPS passed geofence check |
| latitude | Decimal | nullable | GPS latitude at time of dispense |
| longitude | Decimal | nullable | GPS longitude at time of dispense |
| status | Enum | NOT NULL | `COMPLETED`, `CANCELLED`, `FAILED` |

#### FuelStation
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Auto-generated |
| stationName | String(100) | NOT NULL | Human-readable name |
| stationCode | String(20) | UNIQUE, NOT NULL | System code |
| latitude | Decimal | NOT NULL | GPS latitude |
| longitude | Decimal | NOT NULL | GPS longitude |
| geofenceRadiusMeters | Integer | NOT NULL | Authorization radius |
| phoneNumber | String(15) | nullable | Contact number |
| managerName | String(100) | nullable | Station manager |
| managerEmail | String(100) | nullable | Manager email |
| district | String(50) | nullable | Administrative district |
| status | Enum | NOT NULL | `ACTIVE`, `INACTIVE`, `SUSPENDED` |

#### VehicleClaim — **Removed**
> The vehicle claim entity has been removed. Ownership transfer is now automatic: when a customer calls `POST /api/customer/v1/vehicles` with a registration number that already exists, the backend runs BRTA verification. On success the vehicle is re-assigned to the new owner immediately; on failure a `400` error is returned.

---

## 4. API Specification

All REST endpoints follow the versioned URL scheme `/api/{role}/v1/`.

### 4.1 Authentication Endpoints (`/api/auth/v1/`, Public)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/v1/customer/register` | Customer self-registration |
| POST | `/api/auth/v1/customer/send-otp` | Send OTP to mobile |
| POST | `/api/auth/v1/customer/login` | Customer login → JWT |
| POST | `/api/auth/v1/admin/login` | Admin login → JWT |

### 4.2 Customer Endpoints (`/api/customer/v1/`, JWT: CUSTOMER role)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/customer/v1/vehicles` | List own vehicles |
| POST | `/api/customer/v1/vehicles` | Add a vehicle; if reg. number exists and BRTA verification passes, ownership transfers automatically |
| DELETE | `/api/customer/v1/vehicles/{id}` | Deregister a vehicle |
| GET | `/api/customer/v1/vehicles-as-driver` | List vehicles where user is assigned as driver |
| POST | `/api/customer/v1/vehicles/{id}/driver` | Assign a driver to a vehicle |
| DELETE | `/api/customer/v1/vehicles/{id}/driver` | Remove assigned driver from a vehicle |
| GET | `/api/customer/v1/vehicles/{id}/qr-code` | Get QR token for specific vehicle |
| POST | `/api/customer/v1/vehicles/{id}/qr-code/regenerate` | Regenerate QR token |
| GET | `/api/customer/v1/quota` | Get own quota status |
| GET | `/api/customer/v1/qr-code` | Get QR token (primary vehicle) |
| POST | `/api/customer/v1/qr-code/regenerate` | Regenerate QR (primary vehicle) |
| GET | `/api/customer/v1/transactions` | Paginated transaction history |

### 4.3 Pump Representative Endpoints (`/api/pump-rep/v1/`, Public — Core BRD)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/pump-rep/v1/login` | Pump rep login by mobile number → session details |
| POST | `/api/pump-rep/v1/authorize` | Validate QR token + check eligibility + authorize litres |
| POST | `/api/pump-rep/v1/authorize-manual` | Authorize by registration number (no QR token needed) |
| POST | `/api/pump-rep/v1/confirm` | Record dispensed litres + update quota |

#### `POST /api/pump-rep/v1/login`
**Request:**
```json
{ "mobileNumber": "01711123456" }
```
**Response:**
```json
{
  "id": "<uuid>",
  "name": "Mohammad Rahman",
  "employeeId": "EMP-001",
  "stationId": "<uuid>",
  "stationName": "Dhaka North Station",
  "stationCode": "DHK-N-01"
}
```

#### `POST /api/pump-rep/v1/authorize` and `POST /api/pump-rep/v1/authorize-manual`
Both return the same `AuthorizationResponse` shape:
```json
{
  "decision": "APPROVED | PARTIAL | DENIED",
  "authorizedLiters": 10.00,
  "remainingQuota": 14.00,
  "totalQuota": 24.00,
  "message": null,
  "vehicleFound": "DHK-KA-11-1234",
  "vehicleMake": "Toyota Corolla",
  "vehicleColor": "White",
  "ownerName": "John Doe",
  "vehicleStatus": "VERIFIED",
  "fuelType": "Petrol"
}
```

#### `POST /api/pump-rep/v1/confirm`
`qrToken` is **optional** — omit and supply `registrationNumber` for manual-path transactions:
```json
{
  "qrToken": "<jwt>",           // omit on manual path
  "registrationNumber": "...",  // omit on QR path
  "stationId": "<uuid>",
  "pumpRepresentativeId": "<uuid>",
  "dispensedLiters": 8.50,
  "fuelType": "Petrol"
}
```

### 4.4 Admin Endpoints (`/api/admin/v1/`, JWT: ADMIN role)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/v1/stats` | Dashboard statistics |
| GET | `/api/admin/v1/vehicles` | List vehicles (paginated, filterable) |
| PUT | `/api/admin/v1/vehicles/{id}/reverify` | Trigger BRTA re-verification |
| GET | `/api/admin/v1/users` | List users (customers + admins, paginated, filterable) |
| PUT | `/api/admin/v1/users/{id}/status` | Suspend or activate a user account |
| GET | `/api/admin/v1/stations` | List fuel stations |
| POST | `/api/admin/v1/stations` | Create fuel station |
| PUT | `/api/admin/v1/stations/{id}` | Update fuel station |
| DELETE | `/api/admin/v1/stations/{id}` | Delete fuel station |
| GET | `/api/admin/v1/quotas` | List all quotas (includes `individuallyOverridden` flag) |
| PUT | `/api/admin/v1/quotas/{vehicleId}/adjust` | Adjust quota limit (marks as individually overridden) |
| POST | `/api/admin/v1/quotas/{vehicleId}/reset` | Manual quota reset |
| POST | `/api/admin/v1/quotas/bulk-reset` | Bulk reset all quotas |
| GET | `/api/admin/v1/quota-config` | Get global (fallback) quota configuration |
| PUT | `/api/admin/v1/quota-config` | Update global quota configuration |
| GET | `/api/admin/v1/quota-config-sets` | List all quota config sets |
| POST | `/api/admin/v1/quota-config-sets` | Create quota config set |
| PUT | `/api/admin/v1/quota-config-sets/{id}` | Update quota config set |
| DELETE | `/api/admin/v1/quota-config-sets/{id}` | Delete quota config set |
| POST | `/api/admin/v1/quota-config/sync` | Sync config set limits to all non-overridden vehicles |
| GET | `/api/admin/v1/pump-representatives` | List pump representatives |
| POST | `/api/admin/v1/pump-representatives` | Create pump representative |
| PUT | `/api/admin/v1/pump-representatives/{id}` | Update pump representative |
| DELETE | `/api/admin/v1/pump-representatives/{id}` | Delete pump representative |
| GET | `/api/admin/v1/audit-logs` | View audit logs |
| GET | `/api/admin/v1/transactions` | All transactions (paginated) |

#### Quota Config Set — Request / Response
```json
// POST/PUT /api/admin/v1/quota-config-sets
{
  "name": "Private Cars",
  "limitLitres": 30.0,
  "quotaPeriod": "WEEKLY",
  "description": "Quota for private automobile category",
  "registrationCodes": ["GA", "KHA", "BHA"]
}
// Response includes id, registrationCodeDetails, createdAt, updatedAt
```

#### Quota Sync — Response
```json
// POST /api/admin/v1/quota-config/sync
{ "message": "Quota sync completed", "updatedCount": 42 }
```

### 4.5 Public Reference Data Endpoints (`/api/public/v1/`, No Auth)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/public/v1/registration-codes` | List BRTA vehicle registration codes |
| GET | `/api/public/v1/brta-offices` | List BRTA office codes |

---

## 5. Security Requirements

### 5.1 Authentication
- JWT-based authentication using HMAC-SHA256 signing.
- App tokens valid for **24 hours**.
- QR tokens valid for **1 hour** (configurable via `app.jwt.qr-expiration-ms`).
- Token claims include: `userId`, `userEmail`, `userRole`.

### 5.2 Authorization
- `CUSTOMER` role: access only own data.
- `ADMIN` role: access all system data.
- Pump endpoints: public (authenticated by QR token in request body).
- JWT filter injects `userId`, `userEmail`, `userRole` as request attributes for controller use.

### 5.3 Password Security
- Passwords hashed with BCrypt before storage.
- Minimum 8 characters enforced at the API level.

---


---

## 7. Scheduler Requirements

### 7.1 Quota Reset Job
- **Schedule**: Configurable cron expression (default: `0 0 0 ? * SUN` — every Sunday at midnight).
- **Operation**: Reset `usedLiters = 0`, `remainingLiters = limitLiters` for all ACTIVE quotas.
- **Logging**: Log job start, row count processed, and completion time.

---

## 8. Frontend Architecture


### 8.1 Routing
```
/                     → LandingPage (PublicLayout)
/login                → CustomerLoginPage
/register             → CustomerRegisterPage
/admin/login          → AdminLoginPage
/dashboard            → CustomerDashboardPage (CustomerLayout, CUSTOMER role)
/vehicles             → CustomerVehiclesPage
/qr-code              → CustomerQrPage
/transactions         → CustomerTransactionsPage
/admin/dashboard      → AdminDashboardPage (AdminLayout, ADMIN role)
/admin/vehicles       → AdminVehiclesPage (shows customQuotaConfig badge)
/admin/stations       → AdminStationsPage
/admin/quotas         → AdminQuotasPage (shows individuallyOverridden badge)
/admin/quota-config   → AdminQuotaConfigPage (unified: config sets + global config + sync)
/admin/pump-reps      → AdminPumpRepsPage
/admin/audit-logs     → AdminAuditLogsPage
/pump                 → PumpLoginPage     (PumpRepLayout, public)
/pump/scan            → PumpScanPage      (PumpRepLayout, session-guarded)
/pump/dispense        → PumpDispensePage  (PumpRepLayout, session-guarded)
*                     → NotFoundPage
```

### 8.2 Pump Representative Portal — State & Session
- Pump rep session stored in `localStorage` under `pumpRepSession` (JSON object with `id`, `name`, `employeeId`, `stationId`, `stationName`, `stationCode`).
- Session helpers exported from `PumpRepLayout.tsx`: `getPumpSession`, `savePumpSession`, `clearPumpSession`.
- Pages redirect to `/pump` if no session is found.
- Authorization result passed between `/pump/scan` and `/pump/dispense` via React Router `location.state`.

### 8.3 Pump Representative Portal — Page Components
| Page | File | Description |
|------|------|-------------|
| Login | `pages/pump/PumpLoginPage.tsx` | Employee ID input → calls `/api/pump/login` → saves session |
| Scan | `pages/pump/PumpScanPage.tsx` | Tab switcher: camera QR scanner ↔ manual reg number entry |
| Dispense | `pages/pump/PumpDispensePage.tsx` | Vehicle info panel + quota bar + fuel selector + numeric keypad + submit |

### 8.4 State Management
- Authentication state managed by `AuthContext` (React Context + localStorage).
- API calls via centralized Axios instance with request/response interceptors.
- Local component state for UI data (no global state library required).

### 8.5 API Client Pattern
- `/api/pump-rep/v1/*` calls use a **separate Axios instance** (`pumpApi.ts`) with no JWT header — these are public endpoints.
- All other API calls use the shared Axios instance with JWT header injection.
- All endpoints use versioned paths: `/api/{role}/v1/`.

### 8.6 Pump API Module (`src/api/pumpApi.ts`)
| Function | Endpoint | Notes |
|----------|----------|-------|
| `pumpRepLogin` | `POST /api/pump/login` | Returns `PumpRepSession` |
| `authorizeDispensing` | `POST /api/pump/authorize` | QR token path |
| `authorizeByRegistration` | `POST /api/pump/authorize-manual` | Manual path |
| `confirmDispensing` | `POST /api/pump/confirm` | `qrToken` or `registrationNumber` |

---

## 9. Configuration Reference

### 9.1 Key Application Properties
```yaml
# Database
spring.datasource.url: jdbc:postgresql://localhost:5432/automated_fuel_quota
spring.datasource.username: postgres
spring.datasource.password: postgres

# JWT
app.jwt.secret: <256-bit secret>
app.jwt.expiration-ms: 86400000          # 24 hours
app.jwt.qr-expiration-ms: 3600000        # 1 hour

# Quota
app.quota.weekly-limit-litres: 24.0
app.quota.reset-cron-expression: "0 0 0 ? * SUN"
app.quota.geofence-radius-meters: 100
```

### 9.2 Default Seed Data
On startup, `DataInitializer` creates the following if not already present:
- **Admin account**: `admin@fuelquota.gov` / `admin123`
- **Sample customers** with vehicles and quota records for testing.
- **Fuel stations**, **pump representatives**, and **registration reference data**.

---

## 10. Build & Deployment


### 10.1 Development
```bash
# Backend only
mvn spring-boot:run

# Frontend only (hot reload)
cd frontend && npm run dev

# Full stack (backend serves built frontend)
mvn clean package && java -jar target/*.jar
```

### 10.2 Production Build
```bash
# Single JAR (includes embedded frontend)
mvn clean package

# Run
java -jar target/automated-fuel-quota-0.0.1-SNAPSHOT.jar
```

The `frontend-maven-plugin` handles Node.js installation and React build automatically during `mvn package`. The `copy-to-static` npm script moves the built assets into `src/main/resources/static/`.

### 10.3 Database Setup
```sql
CREATE DATABASE automated_fuel_quota;
```

---

## 11. Testing Requirements

### 11.1 Critical Test Scenarios
| Scenario | Test Type |
|----------|-----------|
| Quota authorization: full, partial, denied | Integration |
| Weekly reset job execution | Integration |
| JWT validation and expiry | Unit |
| QR token generation and expiry | Unit |
| Geofence calculation | Unit |
| Idempotent transaction confirmation | Integration |
| RBAC: customer cannot access admin endpoints | Integration |
| Vehicle NID uniqueness enforcement | Integration |

### 11.2 Test Execution
```bash
# All backend tests
mvn test

# All integration tests
mvn verify

# Frontend tests
cd frontend && npm test
```

---

## 12. Monitoring & Observability

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | System health check |
| `GET /actuator/metrics` | JVM and application metrics |
| `GET /actuator/info` | Build info |

Key metrics to monitor:
- Authorization request rate and latency.
- Quota reset job execution (success/failure).
- Database connection pool utilization.

---

*Document maintained by: Engineering Team*  
*Next review date: 2026-07-01*  
*See [`USER_JOURNEY.md`](USER_JOURNEY.md) for actor-level journey maps.*
