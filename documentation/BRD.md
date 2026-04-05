# Business Requirements Document (BRD)
## Automated Fuel Quota Management System

**Document Version:** 2.3
**Date:** 2026-04-04
**Status:** Approved — Updated with Driver Assignment, Driver-Only Registration, and Quota by Registration Code features

---

## Document Navigation

| Document | Description |
|----------|-------------|
| **This file** — BRD | Business requirements, rules, and acceptance criteria |
| [`SRS.md`](SRS.md) | Software Requirements Specification — API contracts, entity schemas |
| [`USER_JOURNEY.md`](USER_JOURNEY.md) | Step-by-step user journey maps for all actor types |

---

## 1. Executive Summary

The Automated Fuel Quota Management System is a platform to **control, authorize, and track** fuel dispensing per vehicle based on a configurable quota policy. The system supports **flexible registration** (vehicle owners or driver-only accounts), **driver assignment** to vehicles, and **registration-code-specific quota configurations**. Vehicle owners or assigned drivers present a **QR code** at a fuel station, where a pump representative scans it using the **web-based Pump Representative Portal** (or manually enters the vehicle registration number as a fallback). A backend validates eligibility (vehicle status, GPS/geofencing) and returns the authorized liters based on vehicle-specific or default quota configuration. All dispensing events are recorded and quotas reset on a configurable schedule.

---

## 2. Objectives & Success Criteria

### 2.1 Objectives
1. Enforce configurable periodic fuel quotas per vehicle, with support for registration-code-specific limits.
2. Enable flexible user registration: vehicle owners or driver-only accounts.
3. Support driver assignment to vehicles, allowing both owners and drivers to authorize fuel dispensing.
4. Prevent unauthorized refueling via JWT authentication, QR validation, and GPS geofencing.
5. Provide fast, reliable authorization at the pump (< 2 seconds).
6. Maintain a complete, auditable history of all refueling and administrative actions.
7. Support **partial dispensing** when remaining quota is less than requested.
8. Enable vehicle ownership transfer via a claim-and-approval workflow.

### 2.2 Success Criteria (KPIs)
- Authorization response time < 2 seconds under normal load.
- 100% of dispenses recorded with quota before/after values.
- Successful periodic quota reset execution with audit trail.
- Zero unauthorized cross-user data access incidents.

---

## 3. Scope

### 3.1 In Scope
- **Customer (Vehicle Owner) Portal** — Flexible registration (with or without vehicle), vehicle management, driver assignment, QR code generation (owner and assigned drivers), quota visibility, transaction history, vehicle ownership claims, and viewing vehicles where user is assigned as driver.
- **Admin Portal** — Vehicle management, fuel station management, quota configuration and adjustment, quota configuration by registration code (e.g., LA = 20L DAILY), pump representative management, and audit log viewer.
- **Pump Representative Web Portal** — Browser-based portal for pump representatives: employee-code login, QR code scanning, manual vehicle number lookup, dispense entry with on-screen numeric keypad, and transaction submission.
- **Pump Representative API** — QR scanning, manual authorization by registration number, and dispense confirmation (core BRD flow).
- **Fuel System Backend** — Request validation, vehicle status check, GPS geofencing, quota calculation (code-specific or default), authorization response, transaction recording, driver authorization validation, and scheduled quota reset.

### 3.2 Out of Scope (Current Release)
- Payments, billing, or pricing.
- Physical pump hardware integration.
- Multi-timezone support (single fixed timezone).
- BRTA API live integration (currently simulated; vehicles are auto-verified).
- SMS gateway integration (TOTP verification can work with future SMS service).
- Offline-first workflows.

---

## 4. Stakeholders & Users

### 4.1 Stakeholders
- Government fuel quota program sponsor.
- Fuel station operations teams.
- Compliance and audit teams.
- Engineering (backend, frontend, DevOps).

### 4.2 User Types
| Role | Description |
|------|-------------|
| **Customer (Vehicle Owner)** | Self-registers with or without vehicle, manages vehicles, assigns drivers, generates QR codes for refueling |
| **Customer (Driver)** | Self-registers without vehicle, can be assigned to vehicles by owners, generates QR codes for assigned vehicles |
| **Pump Representative** | Station operator who scans QR codes (or manually enters registration numbers) and confirms fuel dispensed via the Pump Rep Web Portal |
| **System Administrator** | Manages all system entities, configures quotas (including per registration code), reviews audit logs |

---

## 5. Definitions & Glossary

| Term | Definition |
|------|------------|
| **Quota** | Maximum allowable fuel volume for a vehicle within a period |
| **Remaining quota** | `period_limit_litres - used_litres_this_period` |
| **Partial dispense** | When requested litres exceed remaining quota; system authorizes only the remaining quota |
| **Geofencing** | Location-based rule: authorization only valid within a configurable radius of the fuel station |
| **QR token** | Encrypted JWT containing vehicle identity, presented by owner or assigned driver for scanning; 1-hour TTL |
| **NID** | National Identity Document number of the vehicle owner |
| **BRTA** | Bangladesh Road Transport Authority — the vehicle registration authority |
| **Periodic reset** | Scheduled job resetting usage counters for all active quotas (DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY) |
| **Driver** | A registered customer assigned to a vehicle by the owner; can generate QR codes and authorize fuel dispensing |
| **Registration Code** | BRTA vehicle category code (e.g., GA, LA, KHA) used for quota configuration |
| **Driver-Only Account** | Customer account registered without vehicle ownership; can be assigned as driver to vehicles |

---

## 6. Assumptions & Constraints

### 6.1 Assumptions
- A single fixed system timezone is used.
- Both apps require network connectivity for authorization (no offline mode).
- Vehicle identity exists in the system before pump authorization is attempted.
- BRTA verification always succeeds in the current implementation (future: real API).

### 6.2 Constraints
- Default quota reset occurs every **Sunday at 00:00** (configurable via cron expression).
- Default weekly fuel limit: **24 litres** (configurable by admin).
- QR tokens expire after **1 hour**.
- Partial dispense must be supported.
- Dispense confirmation must be idempotent (no double-deductions on retry).

---

## 7. Business Rules

### BR-1: Periodic Quota Limit
Each vehicle is assigned a configurable fuel quota (default: **24L per week**).

### BR-2: Remaining Quota Calculation
```
remaining_quota_litres = limit_litres - used_litres_this_period
```

### BR-3: Partial Dispense Authorization
- If `requested_litres ≤ remaining_quota_litres` → decision = `APPROVED`, authorized = requested.
- If `requested_litres > remaining_quota_litres` and `remaining_quota_litres > 0` → decision = `PARTIAL`, authorized = remaining quota.
- If `remaining_quota_litres == 0` → decision = `DENIED`.

### BR-4: Scheduled Quota Reset
At the configured cron schedule (default: every Sunday 00:00):
- All active quota usage counters reset.
- Reset is logged in the audit trail.

### BR-5: Eligibility Checks
Authorization requires all of the following:
- Valid, non-expired JWT QR token **or** a valid vehicle registration number (manual path).
- Vehicle status = `VERIFIED`.
- Pump representative GPS location within configured geofence radius of the station *(GPS check is skipped when coordinates are not provided)*.
- Remaining quota > 0 (for full approval).

### BR-6: Vehicle Ownership Transfer
- A customer may submit an ownership claim for a vehicle registered under another account.
- Claims are reviewed by an admin.
- On approval, vehicle ownership, owner name, owner email, and owner NID are transferred to the claimant.

### BR-7: Vehicle Registration Workflow
- On registration, vehicles are immediately set to `VERIFIED`.
- Admin may trigger BRTA re-verification at any time (currently always succeeds).
- Vehicle NID must be unique across the system.

### BR-8: Pump Representative Login
- Pump representatives authenticate to the web portal using their **employee ID** (no password required in the current demo implementation).
- Only representatives with `ACTIVE` status may log in.
- Successful login records `lastLoginTimestamp` on the representative record.

### BR-9: Manual Vehicle Authorization
- If a customer's QR code is unavailable (e.g. phone dead), the pump representative may enter the vehicle's registration number directly.
- The system performs the same vehicle status and quota checks as the QR path.
- Transactions recorded via the manual path are not subject to QR-token-based idempotency; the representative is responsible for avoiding duplicate submissions.

---

## 8. Functional Requirements

### 8.1 Customer (Vehicle Owner) Portal

| ID | Requirement |
|----|-------------|
| FR-01 | Customer shall self-register with name, NID, mobile, email, password, and vehicle details |
| FR-02 | Customer shall log in with email and password |
| FR-03 | Customer shall view their registered vehicles and quota status |
| FR-04 | Customer shall add additional vehicles to their account |
| FR-05 | Customer shall deregister a vehicle (soft-delete; history preserved) |
| FR-06 | Customer shall generate a QR code token for a selected vehicle |
| FR-07 | Customer shall regenerate a QR code (invalidating the previous token) |
| FR-08 | Customer shall view their fuel transaction history (paginated) |
| FR-09 | Customer shall submit a vehicle ownership claim with registration number, NID, and reason |
| FR-10 | Customer shall view the status of their ownership claims |

### 8.2 Pump Representative Web Portal & API

| ID | Requirement |
|----|-------------|
| FR-11 | Pump representative shall log in to the web portal using their employee ID |
| FR-12 | After login, pump representative shall see the QR code scanner and their assigned station name |
| FR-13 | Pump app shall send the scanned QR token and station ID to `/api/pump/authorize` |
| FR-14 | As an alternative to QR scanning, representative shall be able to enter the vehicle registration number manually via `/api/pump/authorize-manual` |
| FR-15 | Backend shall return vehicle identity (registration number, BRTA status, make, color, owner name), remaining quota, total quota, and authorized litres |
| FR-16 | Pump representative shall enter the dispensed fuel amount using an on-screen numeric keypad |
| FR-17 | Pump representative shall select the fuel type dispensed from a dropdown |
| FR-18 | Pump app shall confirm dispensed litres to `/api/pump/confirm` |
| FR-19 | Confirmation shall be idempotent for QR-path transactions (safe to retry without double-decrement) |
| FR-20 | After successful confirmation, portal shall display a transaction receipt with reference number and remaining quota |

### 8.3 Admin Portal

| ID | Requirement |
|----|-------------|
| FR-21 | Admin shall view, search, and filter all registered vehicles |
| FR-22 | Admin shall trigger BRTA re-verification for any vehicle |
| FR-23 | Admin shall create, update, and deactivate fuel stations with GPS coordinates |
| FR-24 | Admin shall create and manage pump representatives per station |
| FR-25 | Admin shall view and adjust individual vehicle quotas with a reason |
| FR-26 | Admin shall manually reset an individual vehicle quota |
| FR-27 | Admin shall configure global quota settings (limit, period, geofence radius, cron expression) |
| FR-28 | Admin shall view, search, and manage user accounts (suspend/activate customers and admins) |
| FR-29 | Admin shall view a searchable, filterable audit log of all administrative actions |
| FR-30 | Admin shall view dashboard statistics and analytics charts |

### 8.4 Backend / Infrastructure

| ID | Requirement |
|----|-------------|
| FR-31 | System shall run a scheduled quota reset job per configured cron expression |
| FR-32 | System shall persist all transactions with quota before/after values |
| FR-33 | System shall log all administrative actions in an immutable audit trail |

---

## 9. Non-Functional Requirements (NFR)

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-01 | Security | All API calls must use TLS (HTTPS) in production |
| NFR-02 | Security | JWT tokens must be validated for issuer, expiry, and signature |
| NFR-03 | Security | QR tokens must have a 1-hour TTL to prevent replay attacks |
| NFR-04 | Security | Role-based access control: customers cannot access other customers' data |
| NFR-05 | Security | Passwords must be hashed (BCrypt) |
| NFR-06 | Performance | Authorization response time < 2 seconds |
| NFR-07 | Performance | System must support 10,000+ concurrent users |
| NFR-08 | Reliability | Dispense confirmation must be idempotent |
| NFR-09 | Reliability | Quota reset job must be monitored and alertable on failure |
| NFR-10 | Observability | Centralized structured logging for all API requests |
| NFR-11 | Observability | Spring Boot Actuator health and metrics endpoints exposed |
| NFR-12 | Availability | System uptime target: 99.9% |
| NFR-13 | Data Retention | Transaction and audit records must be retained (retention period TBD) |

---

## 10. Error & Exception Handling

| Code | Scenario | System Response |
|------|----------|----------------|
| ER-01 | Expired or invalid QR token | Reject authorization; return error message |
| ER-02 | Vehicle not found | Deny authorization; return `Vehicle not found` |
| ER-03 | Vehicle not VERIFIED | Deny authorization; return vehicle status reason |
| ER-04 | GPS outside geofence | Deny authorization; return geofence failure reason |
| ER-05 | Quota exhausted (0L remaining) | Deny authorization; return `DENIED` decision |
| ER-06 | Partial quota | Authorize remaining litres; return `PARTIAL` decision |
| ER-07 | Duplicate QR confirm request | Idempotent: return error; no double-deduction |
| ER-08 | NID already registered | Reject registration; return validation error |
| ER-09 | Registration number already exists | Reject registration; return validation error |
| ER-10 | Employee ID not found at pump login | Return 400 with descriptive message |
| ER-11 | Representative account not ACTIVE | Return 400; deny login |
| ER-12 | Vehicle not found on manual lookup | Return DENIED authorization with message |

---

## 11. Acceptance Test Scenarios

| ID | Scenario | Expected Outcome |
|----|----------|-----------------|
| ATS-01 | Full approval (QR path) | remaining ≥ requested → `APPROVED`, authorized = requested |
| ATS-02 | Partial approval | 0 < remaining < requested → `PARTIAL`, authorized = remaining |
| ATS-03 | Quota exhausted | remaining = 0 → `DENIED` |
| ATS-04 | Geofence denial | GPS > radius → `DENIED`, reason = geofence |
| ATS-05 | Invalid/expired QR | `DENIED`, reason = invalid token |
| ATS-06 | Quota reset | After scheduled reset: used = 0, remaining = limit |
| ATS-07 | Idempotent confirm | Second confirm with same QR token: 400 error, no deduction |
| ATS-08 | Second-hand vehicle add — BRTA passes | Vehicle transfers to new customer; old quota deleted, new quota created |
| ATS-09 | New registration | Vehicle auto-VERIFIED; quota created as ACTIVE |
| ATS-10 | Pump rep login — valid employee ID | 200 response with rep details and station info |
| ATS-11 | Pump rep login — invalid employee ID | 400 error |
| ATS-12 | Manual authorization — valid reg number | Same authorization response shape as QR path |
| ATS-13 | Manual authorization — unknown reg number | `DENIED` with `Vehicle not found` message |
| ATS-14 | Manual confirm — no QR token | Transaction recorded; quota deducted |

---

## 12. Traceability Matrix

| Business Rule | FR IDs | Use Cases | State Diagram |
|---------------|--------|-----------|---------------|
| BR-1 Periodic Quota Limit | FR-01, FR-25, FR-27 | UC-25, UC-27, UC-31 | [Quota States](diagrams/07-state-diagrams.md) |
| BR-2 Remaining Quota Calc | FR-15, FR-20 | UC-03, UC-14 | — |
| BR-3 Partial Dispense | FR-15, FR-18 | UC-15, UC-17 | — |
| BR-4 Scheduled Reset | FR-31 | UC-31 | [Quota States](diagrams/07-state-diagrams.md) |
| BR-5 Eligibility Checks | FR-13, FR-14 | UC-12, UC-13 | [Vehicle States](diagrams/07-state-diagrams.md) |
| BR-6 Ownership Transfer  | FR-09 | UC-09 | [Vehicle States](diagrams/07-state-diagrams.md) |
| BR-7 Vehicle Registration | FR-01, FR-04 | UC-01, UC-04 | [Vehicle States](diagrams/07-state-diagrams.md) |
| BR-8 Rep Login | FR-11 | UC-11 | [Rep States](diagrams/07-state-diagrams.md) |
| BR-9 Manual Authorization | FR-14, FR-18 | UC-13, UC-17 | — |

---

## 13. Open Items / Future Scope

1. **BRTA API Integration** — Real-time ownership verification against national vehicle registry.
2. **SMS Gateway Integration** — Connect TOTP OTP system to SMS provider for production deployment.
3. **Pump Hardware Integration** — Direct integration with pump telemetry for automatic dispense confirmation.
4. **Offline Mode** — Pump app operation during connectivity loss with sync-on-reconnect.
5. **Multi-tenant Support** — Multiple quota programs per vehicle class or region.
6. **Data Retention Policy** — Define and enforce retention periods for transactions and audit logs.
7. **Advanced Analytics** — Fuel consumption forecasting and station performance reports.
8. **Pump Rep Password Authentication** — Replace employee-ID-only demo login with full username + password flow.

---

*Document maintained by: Engineering Team*  
*Next review date: 2026-07-01*  
*See [`USER_JOURNEY.md`](USER_JOURNEY.md) for actor-level journey maps and [`diagrams/`](diagrams/README.md) for visual system diagrams.*
