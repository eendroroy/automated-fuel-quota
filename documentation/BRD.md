# Business Requirements Document (BRD)
## Automated Fuel Quota Management System

**Document Version:** 2.0  
**Date:** 2026-04-03  
**Status:** Approved

---

## 1. Executive Summary

The Automated Fuel Quota Management System is a platform to **control, authorize, and track** fuel dispensing per vehicle based on a configurable weekly quota policy. Vehicle owners present a **QR code** at a fuel station, where a pump representative scans it using a mobile app. A backend validates eligibility (vehicle status, GPS/geofencing) and returns the authorized liters. All dispensing events are recorded and quotas reset on a configurable schedule.

---

## 2. Objectives & Success Criteria

### 2.1 Objectives
1. Enforce configurable periodic fuel quotas per vehicle.
2. Prevent unauthorized refueling via JWT authentication, QR validation, and GPS geofencing.
3. Provide fast, reliable authorization at the pump (< 2 seconds).
4. Maintain a complete, auditable history of all refueling and administrative actions.
5. Support **partial dispensing** when remaining quota is less than requested.
6. Enable vehicle ownership transfer via a claim-and-approval workflow.

### 2.2 Success Criteria (KPIs)
- Authorization response time < 2 seconds under normal load.
- 100% of dispenses recorded with quota before/after values.
- Successful periodic quota reset execution with audit trail.
- Zero unauthorized cross-user data access incidents.

---

## 3. Scope

### 3.1 In Scope
- **Customer (Vehicle Owner) Portal** — Registration, vehicle management, QR code generation, quota visibility, transaction history, and vehicle ownership claims.
- **Admin Portal** — Vehicle management, fuel station management, quota configuration and adjustment, pump representative management, and audit log viewer.
- **Pump Representative API** — QR scanning, authorization, and dispense confirmation (core BRD flow).
- **Fuel System Backend** — Request validation, vehicle status check, GPS geofencing, quota calculation, authorization response, transaction recording, and scheduled quota reset.

### 3.2 Out of Scope (Current Release)
- Payments, billing, or pricing.
- Physical pump hardware integration.
- Multi-timezone support (single fixed timezone).
- BRTA API live integration (currently simulated; vehicles are auto-verified).
- OTP/SMS-based mobile verification.
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
| **Customer (Vehicle Owner)** | Self-registers, manages vehicles, generates QR codes for refueling |
| **Pump Representative** | Station operator who scans QR codes and confirms dispense via mobile API |
| **System Administrator** | Manages all system entities, configures quotas, reviews audit logs |

---

## 5. Definitions & Glossary

| Term | Definition |
|------|------------|
| **Quota** | Maximum allowable fuel volume for a vehicle within a period |
| **Remaining quota** | `period_limit_litres - used_litres_this_period` |
| **Partial dispense** | When requested litres exceed remaining quota; system authorizes only the remaining quota |
| **Geofencing** | Location-based rule: authorization only valid within a configurable radius of the fuel station |
| **QR token** | Encrypted JWT containing vehicle identity, presented by owner for scanning; 1-hour TTL |
| **NID** | National Identity Document number of the vehicle owner |
| **BRTA** | Bangladesh Road Transport Authority — the vehicle registration authority |
| **Weekly reset** | Scheduled job resetting usage counters for all active quotas |

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
- Redis cache is evicted.
- Reset is logged in the audit trail.

### BR-5: Eligibility Checks
Authorization requires all of the following:
- Valid, non-expired JWT QR token.
- Vehicle status = `VERIFIED`.
- Pump representative GPS location within configured geofence radius of the station.
- Remaining quota > 0 (for full approval).

### BR-6: Vehicle Ownership Transfer
- A customer may submit an ownership claim for a vehicle registered under another account.
- Claims are reviewed by an admin.
- On approval, vehicle ownership, owner name, owner email, and owner NID are transferred to the claimant.

### BR-7: Vehicle Registration Workflow
- On registration, vehicles are immediately set to `VERIFIED`.
- Admin may trigger BRTA re-verification at any time (currently always succeeds).
- Vehicle NID must be unique across the system.

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

### 8.2 Pump Representative API

| ID | Requirement |
|----|-------------|
| FR-11 | Pump app shall send the scanned QR token and GPS coordinates to `/api/pump/authorize` |
| FR-12 | Backend shall return vehicle identity, remaining quota, and authorized litres |
| FR-13 | Pump app shall confirm dispensed litres to `/api/pump/confirm` |
| FR-14 | Confirmation shall be idempotent (safe to retry without double-decrement) |

### 8.3 Admin Portal

| ID | Requirement |
|----|-------------|
| FR-15 | Admin shall view, search, and filter all registered vehicles |
| FR-16 | Admin shall trigger BRTA re-verification for any vehicle |
| FR-17 | Admin shall create, update, and deactivate fuel stations with GPS coordinates |
| FR-18 | Admin shall create and manage pump representatives per station |
| FR-19 | Admin shall view and adjust individual vehicle quotas with a reason |
| FR-20 | Admin shall manually reset an individual vehicle quota |
| FR-21 | Admin shall configure global quota settings (limit, period, geofence radius, cron expression) |
| FR-22 | Admin shall review, approve, and reject vehicle ownership claims |
| FR-23 | Admin shall view a searchable, filterable audit log of all administrative actions |
| FR-24 | Admin shall view dashboard statistics and analytics charts |

### 8.4 Backend / Infrastructure

| ID | Requirement |
|----|-------------|
| FR-25 | System shall run a scheduled quota reset job per configured cron expression |
| FR-26 | System shall persist all transactions with quota before/after values |
| FR-27 | System shall log all administrative actions in an immutable audit trail |
| FR-28 | System shall cache vehicle and quota data in Redis for performance |
| FR-29 | System shall evict caches on any vehicle or quota status change |

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
| ER-07 | Duplicate confirm request | Idempotent: return existing transaction; no double-deduction |
| ER-08 | NID already registered | Reject registration; return validation error |
| ER-09 | Registration number already exists | Reject registration; return validation error |

---

## 11. Acceptance Test Scenarios

| ID | Scenario | Expected Outcome |
|----|----------|-----------------|
| ATS-01 | Full approval | remaining ≥ requested → decision `APPROVED`, authorized = requested |
| ATS-02 | Partial approval | 0 < remaining < requested → decision `PARTIAL`, authorized = remaining |
| ATS-03 | Quota exhausted | remaining = 0 → decision `DENIED` |
| ATS-04 | Geofence denial | GPS > radius → decision `DENIED`, reason = geofence |
| ATS-05 | Invalid/expired QR | decision `DENIED`, reason = invalid token |
| ATS-06 | Quota reset | After scheduled reset: used = 0, remaining = limit |
| ATS-07 | Idempotent confirm | Second confirm with same transaction ID: no additional quota deduction |
| ATS-08 | Vehicle claim approval | Vehicle transfers to claimant; NID, name, email updated |
| ATS-09 | New registration | Vehicle auto-VERIFIED; quota created as ACTIVE |

---

## 12. Open Items / Future Scope

1. **BRTA API Integration** — Real-time ownership verification against national vehicle registry.
2. **OTP Verification** — Mobile number confirmation via SMS during customer registration.
3. **Pump Hardware Integration** — Direct integration with pump telemetry for automatic dispense confirmation.
4. **Offline Mode** — Pump app operation during connectivity loss with sync-on-reconnect.
5. **Multi-tenant Support** — Multiple quota programs per vehicle class or region.
6. **Data Retention Policy** — Define and enforce retention periods for transactions and audit logs.
7. **Advanced Analytics** — Fuel consumption forecasting and station performance reports.

---

*Document maintained by: Engineering Team*  
*Next review date: 2026-07-01*

