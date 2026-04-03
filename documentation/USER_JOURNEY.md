# User Journey Maps
## Automated Fuel Quota Management System

**Document Version:** 1.0  
**Date:** 2026-04-03  
**Status:** Approved

---

## Overview

This document maps the end-to-end journeys for each user type in the system. Each journey covers the actor's goal, their step-by-step experience, the system interactions involved, and the key decision points and error paths they may encounter.

### User Types

| Actor | Portal | Authentication |
|-------|--------|---------------|
| **Customer (Vehicle Owner)** | Customer Portal `/` | JWT (email + password) |
| **Pump Representative** | Pump Portal `/pump` | localStorage session (employee ID) |
| **System Administrator** | Admin Portal `/admin` | JWT (email + password) |
| **System (Automated)** | — | Internal scheduler |

---

## Journey 1: Customer — First-Time Registration

**Goal:** A new vehicle owner creates an account, registers their vehicle, and gets ready to generate a QR code for refueling.

**Persona:** Rahima Begum, 35, owns a Petrol car in Dhaka, has received her fuel quota program information notice.

### Journey Steps

| # | Step | Actor Action | System Response | Status |
|---|------|-------------|-----------------|--------|
| 1 | **Discover system** | Visits `http://localhost:8080` (or production URL) | Landing page with register/login CTAs | 🟢 |
| 2 | **Navigate to register** | Clicks "Register as Vehicle Owner" | `CustomerRegisterPage` loaded | 🟢 |
| 3 | **Fill personal info** | Enters: Name, NID (13-digit), mobile, email, password | Form validates each field inline | 🟢 |
| 4 | **Enter vehicle reg number** | Uses the 4-part structured input: selects BRTA Office, selects Registration Code, enters Serial Part 1 and Part 2 | Dropdown reference data loaded from `/api/public/brta-offices` and `/api/public/registration-codes` | 🟢 |
| 5 | **Enter vehicle details** | Selects fuel type (Petrol), enters make (Toyota Corolla), color (White), engine CC (1800), registration date | — | 🟢 |
| 6 | **Submit form** | Clicks "Register" | `POST /api/auth/customer/register` → validates uniqueness of email, NID, registration number | 🟢 |
| 7 | **Auto-login** | — | System creates: User record, Vehicle (VERIFIED), Quota (ACTIVE, 24L), returns JWT | 🟢 |
| 8 | **Redirected** | — | Browser redirects to `/dashboard` | 🟢 |

### Validation Error Paths

| Error Scenario | System Response | Customer Experience |
|----------------|-----------------|---------------------|
| Email already registered | `400 — "Email already registered"` | Toast: "Email already in use" |
| NID already registered | `400 — "NID already registered"` | Toast: "NID already in use" |
| Registration number exists | `400 — "Registration number already exists"` | Toast: "Vehicle already registered" |
| Password too short (<8 chars) | Client-side validation | Inline error under password field |

### Success Outcome
- User account created with `CUSTOMER` role
- Vehicle created with status `VERIFIED`
- Quota created: `limitLiters = 24, usedLiters = 0, remainingLiters = 24, period = WEEKLY, status = ACTIVE`
- Customer is logged in and can see their dashboard

---

## Journey 2: Customer — Weekly Refueling (QR Path)

**Goal:** The customer generates a QR code and presents it at a fuel station to refuel their vehicle.

**Persona:** Rahima Begum, returning customer, fuel quota partially used this week (8L of 24L used).

### Journey Steps

| # | Step | Actor Action | System Response | Status |
|---|------|-------------|-----------------|--------|
| 1 | **Login** | Opens app → `/login` → enters email/password | `POST /api/auth/customer/login` → JWT returned | 🟢 |
| 2 | **View dashboard** | Sees quota gauge: 8L used / 24L total | `GET /api/customer/quota` → QuotaResponse | 🟢 |
| 3 | **Navigate to QR** | Clicks "Generate QR Code" or `/qr-code` | `CustomerQRCodePage` loads | 🟢 |
| 4 | **Select vehicle** | Selects vehicle from dropdown (if multiple) | — | 🟢 |
| 5 | **Generate QR** | Clicks "Generate QR Code" | `GET /api/customer/vehicles/{id}/qr-code` → 1-hour JWT QR token | 🟢 |
| 6 | **View QR** | QR code displayed as scannable image | QR encodes the JWT; countdown timer shows expiry | 🟢 |
| 7 | **At fuel station** | Presents phone screen to pump representative | Rep scans QR → see Pump Rep Journey | 🟢 |
| 8 | **After dispensing** | Rep confirms dispense | Customer quota updated; 8L → 18L used | 🟢 |
| 9 | **View history** | Navigates to `/transactions` | `GET /api/customer/transactions` → paginated history | 🟢 |

### Alternative Paths

| Scenario | Customer Action | Outcome |
|----------|---------------|---------|
| QR expired (>1 hour) | Clicks "Regenerate QR" | New JWT issued, old QR invalidated |
| Partial quota | Requests 15L, only 16L remaining | Rep receives PARTIAL → can dispense ≤16L |
| Quota exhausted | 0L remaining | Rep receives DENIED; customer must wait for reset |
| Multiple vehicles | Selects different vehicle on QR page | QR generated for selected vehicle |

### Decision Points

```
Customer at QR page
     │
     ├─ Quota = 0 → "No quota remaining — resets Sunday 00:00"
     │
     ├─ QR not expired → Show QR code
     │
     └─ QR expired → Show "Regenerate" button
```

---

## Journey 3: Customer — Vehicle Ownership Claim

**Goal:** A customer wants to transfer ownership of a vehicle currently registered under another person's account (e.g. purchased a second-hand vehicle).

**Persona:** Karim, 42, just bought a used car. The car is registered to the previous owner in the system.

### Journey Steps

| # | Step | Actor Action | System Response | Status |
|---|------|-------------|-----------------|--------|
| 1 | **Login** | Logs into their customer account | JWT issued | 🟢 |
| 2 | **Navigate to Claims** | Clicks "My Claims" → `/claims` | `CustomerClaimsPage` loads | 🟢 |
| 3 | **Submit new claim** | Clicks "Submit Ownership Claim" | Claim form modal opens | 🟢 |
| 4 | **Fill claim form** | Enters: Vehicle registration number, their NID, reason for claim | — | 🟢 |
| 5 | **Submit** | Clicks "Submit Claim" | `POST /api/customer/vehicles/claim` → claim created in PENDING state | 🟢 |
| 6 | **Track status** | Returns to `/claims` page | `GET /api/customer/vehicles/claims` → list showing PENDING | 🟡 Waiting |
| 7 | **Admin approves** | (Admin action — see Admin Journey 4) | Claim status → APPROVED | 🟢 |
| 8 | **Ownership transferred** | Refreshes claims page | Status = APPROVED; vehicle now appears in own vehicle list | 🟢 |

### Error Paths

| Scenario | System Response |
|----------|-----------------|
| Vehicle not found | `400 — validation error` |
| Claim already pending for this vehicle | `400 — "Claim already pending"` |

---

## Journey 4: Customer — Manage Vehicles

**Goal:** The customer adds a second vehicle or deregisters a vehicle they no longer own.

### Add Vehicle

| # | Step | Actor Action | System Response |
|---|------|-------------|-----------------|
| 1 | Navigate to `/vehicles` | Views vehicle list | `GET /api/customer/vehicles` |
| 2 | Click "Add Vehicle" | Form modal opens | — |
| 3 | Fill vehicle form | Enters registration number + details | — |
| 4 | Submit | `POST /api/customer/vehicles` | New vehicle created (VERIFIED), quota created |
| 5 | View updated list | New vehicle appears with ACTIVE quota | — |

### Deregister Vehicle

| # | Step | Actor Action | System Response |
|---|------|-------------|-----------------|
| 1 | Find vehicle in list | Clicks "Deregister" | Confirm modal opens |
| 2 | Confirm deregistration | Clicks "Confirm" | `DELETE /api/customer/vehicles/{id}` |
| 3 | Vehicle soft-deleted | — | Vehicle status → DEREGISTERED, Quota → EXPIRED |
| 4 | Transaction history | All past transactions preserved | History still visible in `/transactions` |

---

## Journey 5: Pump Representative — Complete Dispensing (QR Path)

**Goal:** A pump representative logs in, scans a customer's QR code, and confirms fuel dispensed.

**Persona:** Mohammad Hossain, pump rep at Dhaka North Station, EMP-001.

### Journey Steps

| # | Step | Actor Action | System Response | Status |
|---|------|-------------|-----------------|--------|
| 1 | **Open portal** | Navigates to `http://<host>/pump` | `PumpLoginPage` loads | 🟢 |
| 2 | **Enter Employee ID** | Types `EMP-001`, clicks Login | `POST /api/pump/login { employeeId: "EMP-001" }` | 🟢 |
| 3 | **Login success** | — | Session saved to `localStorage`; redirected to `/pump/scan` | 🟢 |
| 4 | **View scan page** | Sees station name "Dhaka North Station" + QR tab active | — | 🟢 |
| 5 | **Request camera permission** | Browser requests camera access | `html5-qrcode` initializes camera preview | 🟢 |
| 6 | **Scan customer QR** | Points camera at customer's phone screen | QR decoded → JWT string extracted | 🟢 |
| 7 | **Authorization called** | — | `POST /api/pump/authorize { qrToken, stationId, requestedLiters? }` | 🟢 |
| 8 | **View result** | — | Vehicle info panel + color-coded quota bar displayed | 🟢 |
| 9 | **Navigate to dispense** | Router state carries `AuthorizationResult` to `/pump/dispense` | `PumpDispensePage` loads with vehicle info | 🟢 |
| 10 | **Verify vehicle info** | Reads: Registration, Owner name, Make/Color, Vehicle status badge | — | 🟢 |
| 11 | **View quota bar** | Sees: "16L / 24L remaining" (green bar → yellow at 30% → red at 10%) | — | 🟢 |
| 12 | **Select fuel type** | Chooses "Petrol" from dropdown (pre-filled from vehicle) | — | 🟢 |
| 13 | **Enter dispensed amount** | Uses on-screen numeric keypad: enters `10.00` (10L) | Keypad: digits + decimal, max 4+2 format | 🟢 |
| 14 | **Confirm dispense** | Clicks "Confirm Dispense" | `POST /api/pump/confirm { qrToken, stationId, pumpRepresentativeId, dispensedLiters: 10.00, fuelType: "Petrol" }` | 🟢 |
| 15 | **View receipt** | — | Receipt panel: Transaction Ref #, 10L dispensed, 6L remaining quota | 🟢 |
| 16 | **Next customer** | Clicks "New Transaction" | Returns to `/pump/scan` | 🟢 |

### Authorization Decision Outcomes

| Quota State | Decision | Rep Sees |
|-------------|----------|----------|
| 16L remaining, requests 10L | APPROVED | Green badge — authorized 10L |
| 5L remaining, requests 10L | PARTIAL | Yellow badge — authorized 5L only |
| 0L remaining | DENIED | Red badge — "No quota remaining" |
| Vehicle UNVERIFIED | DENIED | Red badge — "Vehicle unverified" |
| QR token expired | DENIED | Red badge — "Expired QR token" |
| GPS outside geofence | DENIED | Red badge — "Outside station geofence" |

### Error Paths

| Error Scenario | System Response | Rep Experience |
|----------------|-----------------|----------------|
| Employee ID not found | `400 — "Employee not found"` | Toast error on login page |
| Rep account INACTIVE | `400 — "Account inactive"` | Toast error on login page |
| Camera access denied | Browser permission error | Manual tab shown as fallback |
| Invalid QR code (not our JWT) | DENIED — "Invalid QR token" | Error displayed |
| Second confirm with same QR | `400 — "Transaction already recorded"` | Toast — rep cannot double-dispense |

---

## Journey 6: Pump Representative — Manual Authorization (Fallback Path)

**Goal:** Customer's phone is dead; rep looks up vehicle by registration number manually.

**Persona:** Same rep, customer's phone battery is flat.

### Journey Steps

| # | Step | Actor Action | System Response | Status |
|---|------|-------------|-----------------|--------|
| 1 | **On scan page** | Logged in, at `/pump/scan` | — | 🟢 |
| 2 | **Switch tab** | Clicks "Enter Manually" tab | Manual registration input shown | 🟢 |
| 3 | **Ask customer for reg number** | Customer verbally provides: "DHAKA METRO GA 11-1234" | — | 🟢 |
| 4 | **Type reg number** | Types registration number in input field | — | 🟢 |
| 5 | **Check vehicle** | Clicks "Check Vehicle" | `POST /api/pump/authorize-manual { registrationNumber, stationId }` | 🟢 |
| 6 | **Authorization result** | — | Same `AuthorizationResponse` as QR path (no geofence check) | 🟢 |
| 7 | **Dispense** | Enters amount + fuel type, confirms | `POST /api/pump/confirm { registrationNumber, stationId, ... }` (no `qrToken`) | 🟢 |
| 8 | **Receipt** | — | Receipt displayed; transaction recorded | 🟢 |

### Key Differences from QR Path

| Aspect | Manual Path Behavior |
|--------|---------------------|
| JWT validation | ❌ Not performed |
| Geofence check | ❌ Skipped |
| Idempotency | ❌ Not checked — rep must not submit twice |
| `qrToken` in confirm | ❌ Absent |
| `registrationNumber` in confirm | ✅ Present |
| `geofenceVerified` in transaction | Always `false` |

---

## Journey 7: System Administrator — Vehicle & Quota Management

**Goal:** Admin reviews vehicle statuses, adjusts quotas, and manages the fleet.

**Persona:** System admin reviewing the weekly quota usage report.

### Sub-Journey 7a: Review Vehicles

| # | Step | Actor Action | System Response |
|---|------|-------------|-----------------|
| 1 | Login at `/admin/login` | Admin email + password | JWT issued, redirected to `/admin/dashboard` |
| 2 | View dashboard | Sees KPI cards: total vehicles, transactions today, avg quota usage | `GET /api/admin/stats` |
| 3 | Navigate to Vehicles | `/admin/vehicles` | Paginated vehicle list with filters |
| 4 | Search/filter | Filters by status=VERIFIED, district, fuel type | `GET /api/admin/vehicles?status=VERIFIED&page=0` |
| 5 | Trigger BRTA reverify | Clicks "Re-verify" on a vehicle | `PUT /api/admin/vehicles/{id}/reverify` → status re-confirmed |
| 6 | View result | Status badge updated | Audit log entry created |

### Sub-Journey 7b: Adjust Individual Quota

| # | Step | Actor Action | System Response |
|---|------|-------------|-----------------|
| 1 | Navigate to `/admin/quotas` | Views quota list | `GET /api/admin/quotas` |
| 2 | Find vehicle | Searches by registration number | — |
| 3 | Adjust limit | Clicks "Adjust", enters new limit + reason | `PUT /api/admin/quotas/{vehicleId}/adjust { newLimitLiters: 30, reason: "Medical necessity" }` |
| 4 | Manual reset | Clicks "Reset Now" | `POST /api/admin/quotas/{vehicleId}/reset` — used=0, remaining=new limit |
| 5 | Audit trail | — | Both actions logged in `AUDIT_LOG` with old/new values |

### Sub-Journey 7c: Configure Global Quota Settings

| # | Step | Actor Action | System Response |
|---|------|-------------|-----------------|
| 1 | Navigate to `/admin/quota-config` | Loads current config | `GET /api/admin/quota-config` → { limitLitres: 24, period: WEEKLY, cron: "0 0 0 ? * SUN" } |
| 2 | Update settings | Changes limit to 30L, updates description | — |
| 3 | Save | Clicks "Save Configuration" | `PUT /api/admin/quota-config` → config updated |
| 4 | Confirm | — | New vehicles/quotas will use 30L limit going forward |

---

## Journey 8: System Administrator — Ownership Claim Review

**Goal:** Admin reviews a pending vehicle ownership claim and approves or rejects it.

| # | Step | Actor Action | System Response |
|---|------|-------------|-----------------|
| 1 | Navigate to `/admin/vehicle-claims` | Sees pending claims list | `GET /api/admin/vehicle-claims?status=PENDING` |
| 2 | Open claim details | Clicks on claim | Claim details: claimant name, NID, reason, vehicle reg number |
| 3 | Review | Admin cross-references NID with vehicle record | — |
| 4 | Approve | Clicks "Approve" + optional admin notes | `PUT /api/admin/vehicle-claims/{id}/approve { adminNotes: "..." }` |
| 5 | System transfers ownership | — | `Vehicle.user → claimant`, `Vehicle.ownerName/Email/Nid` updated |
| 6 | Audit log | — | `VEHICLE_APPROVED` action logged |
| 7 | Customer sees update | Claimant's vehicle list now includes the vehicle | — |

### Reject Path

| # | Step | System Response |
|---|------|-----------------|
| 4 | Admin clicks "Reject" | `PUT /api/admin/vehicle-claims/{id}/reject { adminNotes: "NID mismatch" }` |
| 5 | Claim status → REJECTED | Vehicle ownership unchanged |
| 6 | Audit log | `VEHICLE_REJECTED` action logged |

---

## Journey 9: System Administrator — Station & Representative Management

### Sub-Journey 9a: Create Fuel Station

| # | Step | Actor Action | System Response |
|---|------|-------------|-----------------|
| 1 | Navigate to `/admin/stations` | Views station list | `GET /api/admin/stations` |
| 2 | Create station | Clicks "Add Station" → fills form | Name, code, GPS coordinates, geofence radius, district |
| 3 | Save | `POST /api/admin/stations` | Station created (ACTIVE) |

### Sub-Journey 9b: Create Pump Representative

| # | Step | Actor Action | System Response |
|---|------|-------------|-----------------|
| 1 | Navigate to `/admin/pump-reps` | Views rep list | `GET /api/admin/pump-representatives` |
| 2 | Create rep | Clicks "Add Rep" → fills form | Name, email, employeeId, username, password, station |
| 3 | Save | `POST /api/admin/pump-representatives` | Rep created (ACTIVE); can now log into pump portal |
| 4 | Share credentials | Admin gives employee ID to rep | — |

---

## Journey 10: System Administrator — Audit Log Review

**Goal:** Admin investigates a suspected anomaly by reviewing recent audit entries.

| # | Step | Actor Action | System Response |
|---|------|-------------|-----------------|
| 1 | Navigate to `/admin/audit-logs` | Views log list | `GET /api/admin/audit-logs` (paginated) |
| 2 | Filter | Filters by actionType=QUOTA_ADJUSTMENT, date range | Filtered results |
| 3 | Inspect entry | Clicks on entry | Shows: adminName, targetEntity, oldValue JSON, newValue JSON, reason, timestamp |
| 4 | Export (future) | — | Future scope: CSV export |

---

## Journey 11: Automated System — Periodic Quota Reset

**Goal:** Automated weekly quota reset runs without human intervention.

| # | Step | Trigger | System Action |
|---|------|---------|---------------|
 1  **Cron fires**  Sunday 00:00 (or configured schedule)  `QuotaService.resetAllQuotas()` invoked by Spring Scheduler 
 2  **Bulk DB update**  —  `UPDATE quotas SET used_liters=0, remaining_liters=limit WHERE status='ACTIVE'` 
 3  **Audit log**  —  `QUOTA_RESET` entry written with count of reset quotas 
 4  **Logging**  —  Application log: "Quota reset complete — N quotas reset in Xms" 
 5  **Next morning**  —  All active vehicles show full quota in customer dashboards 

### Monitoring

 What to Check  How 
-------------------
 Job executed successfully  Application logs: "Quota reset complete" 
 Row count reasonable  Audit log `newValue.resetCount` 
 Health  `GET /actuator/health` 

---

## Cross-Journey Touchpoints Summary

```
Customer Registration ──────────────────────────────────► Quota Created (24L)
                                                                │
Customer Generates QR ──► Rep Scans ──► Authorization ──► Dispense ──► Quota Decremented
                                                                │
                                              Sunday 00:00 ──► Quota Reset ──► Back to 24L
                                                                │
Customer Submits Claim ──► Admin Reviews ──► Approve ──► Ownership Transferred
                                                                │
Admin Adjusts Quota ──────────────────────────────────────────► Individual Quota Updated
```

---

*Document maintained by: Engineering Team*  
*Next review date: 2026-07-01*

