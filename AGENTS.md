# AI Coding Agent Guide

## Architecture Overview

This is a **Spring Boot 4.0.5 + React 18 monorepo** implementing a fuel quota management system with embedded frontend. The backend serves as both API server and static file server, combining two deployment models:

- **Development**: Frontend runs on Vite dev server (localhost:5173) with API proxy to backend (localhost:8080)
- **Production**: Frontend builds into `src/main/resources/static/` and gets served by Spring Boot

## Documentation

- [`documentation/BRD.md`](documentation/BRD.md) — Business Requirements Document (authoritative requirements reference)
- [`documentation/SRS.md`](documentation/SRS.md) — Software Requirements Specification (API contracts, entity schemas, config reference)

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
4. Returns authorization decision with vehicle info and authorized liters.
5. After dispensing → `/api/pump/confirm` updates quota and records transaction.

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

### Caching Strategy (Redis)
```java
@Cacheable(value = "quota", key = "#registrationNumber")  // Frequent quota lookups
@CacheEvict(value = {"vehicle", "quota"}, allEntries = true)  // On status changes
```

Cache keys: Vehicle and quota data are cached by ID and registration number. Always evict both caches when vehicle status changes.

### Security Implementation
- **JWT with dual expiration**: 24h for app tokens, 1h for QR tokens
- **Role-based routing**: Customer/Admin layouts with `ProtectedRoute` components
- **Request attribute pattern**: JWT filter adds `userId`, `userEmail`, `userRole` to request for easy access in controllers

### API Design Conventions
- `/api/pump/*` - **Public endpoints** for pump representative mobile apps (core BRD)
- `/api/customer/*` - **CUSTOMER role required** (JWT protected)
- `/api/admin/*` - **ADMIN role required** (JWT protected)
- `/api/public/*` - **Public reference data** (registration codes, BRTA offices)

### Frontend Architecture
- **Layout-based routing**: `PublicLayout`, `CustomerLayout`, `AdminLayout` with nested routes
- **API client pattern**: Centralized axios instance with interceptors for auth/errors
- **Type definitions**: Shared TypeScript interfaces in `frontend/src/types/index.ts`

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
- **Periodic reset job**: Verify quota calculations and cache eviction
- **Security**: JWT validation, role-based access, QR token expiration
- **Idempotency**: Duplicate transaction prevention in `/api/pump/confirm`
- **NID uniqueness**: Vehicle registration rejects duplicate `ownerNid`

### Error Handling Patterns
- Global exception handler returns structured JSON responses
- Service layer throws domain exceptions (ResourceNotFoundException, BadRequestException)
- Frontend shows toast notifications for all API errors

## Deployment & Production Considerations

### Single JAR Deployment
The application builds to a single executable JAR with embedded frontend. Set `spring.profiles.active=prod` and configure external database/Redis.

### Required External Dependencies
- **PostgreSQL 15+**: Primary database
- **Redis 7+**: Caching layer (optional but recommended for performance)

### Monitoring Endpoints
Spring Boot Actuator exposes `/actuator/health`, `/actuator/metrics` for monitoring. Check quota reset job execution and cache hit rates.
