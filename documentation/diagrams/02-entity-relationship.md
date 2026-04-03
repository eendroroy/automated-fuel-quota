# Entity Relationship Diagram

> Complete database schema with all entities, fields, and relationships for the Automated Fuel Quota Management System.

```mermaid
erDiagram

    USER {
        UUID id PK
        string email UK
        string password
        string name
        string role "CUSTOMER | ADMIN"
        string mobileNumber
        boolean enabled
        datetime createdAt
        datetime updatedAt
    }

    VEHICLE {
        UUID id PK
        string registrationNumber UK
        string brtaOfficeCode
        string vehicleRegistrationCode
        string ownerName
        string ownerNid UK
        string ownerMobile
        string ownerEmail
        string vehicleMake
        string vehicleColor
        string vehicleClass
        string fuelType
        int engineDisplacement
        date registrationDate
        string status "VERIFIED | UNVERIFIED | DEREGISTERED"
        UUID user_id FK
        datetime createdAt
        datetime updatedAt
    }

    QUOTA {
        UUID id PK
        UUID vehicle_id FK "UNIQUE"
        decimal limitLiters
        decimal usedLiters
        decimal remainingLiters
        string period "DAILY|WEEKLY|MONTHLY|QUARTERLY|YEARLY"
        datetime resetTimestamp
        datetime lastTransactionTimestamp
        string status "ACTIVE | SUSPENDED | EXPIRED"
    }

    TRANSACTION {
        UUID id PK
        UUID vehicle_id FK
        UUID station_id FK
        UUID pumpRepresentativeId
        decimal amountDispensedLiters
        string fuelTypeDispensed
        decimal quotaBefore
        decimal quotaAfter
        datetime transactionTimestamp
        boolean geofenceVerified
        decimal latitude
        decimal longitude
        string status "COMPLETED | CANCELLED | FAILED"
    }

    FUEL_STATION {
        UUID id PK
        string stationName
        string stationCode UK
        decimal latitude
        decimal longitude
        int geofenceRadiusMeters
        string phoneNumber
        string managerName
        string managerEmail
        string district
        string status "ACTIVE | INACTIVE | SUSPENDED"
        datetime createdAt
        datetime updatedAt
    }

    PUMP_REPRESENTATIVE {
        UUID id PK
        UUID station_id FK
        string name
        string mobileNumber
        string email UK
        string employeeId UK
        string username UK
        string passwordHash
        string status "ACTIVE | INACTIVE | SUSPENDED"
        datetime lastLoginTimestamp
        datetime createdAt
        datetime updatedAt
    }

    VEHICLE_CLAIM {
        UUID id PK
        UUID vehicle_id FK
        UUID claimant_id FK
        string claimantNid
        string reason
        string status "PENDING | APPROVED | REJECTED"
        string adminNotes
        datetime createdAt
        datetime updatedAt
    }

    AUDIT_LOG {
        UUID id PK
        UUID adminUserId
        string adminName
        string actionType
        string targetEntity
        UUID targetEntityId
        json oldValue
        json newValue
        string reasonNotes
        datetime actionTimestamp
    }

    QUOTA_CONFIG {
        UUID id PK
        string configKey UK "DEFAULT"
        decimal limitLitres
        int geofenceRadiusMeters
        string quotaPeriod "DAILY|WEEKLY|MONTHLY|QUARTERLY|YEARLY"
        string resetCronExpression
        string description
        datetime createdAt
        datetime updatedAt
    }

    BRTA_OFFICE {
        UUID id PK
        string brtaCode UK
        string description
    }

    REGISTRATION_CODE {
        UUID id PK
        string code UK
        string description
    }

    %% Relationships
    USER ||--o{ VEHICLE : "owns (1 user → many vehicles)"
    VEHICLE ||--|| QUOTA : "has exactly one quota"
    VEHICLE ||--o{ TRANSACTION : "appears in"
    VEHICLE ||--o{ VEHICLE_CLAIM : "subject of"
    FUEL_STATION ||--o{ TRANSACTION : "records at station"
    FUEL_STATION ||--o{ PUMP_REPRESENTATIVE : "employs"
    USER ||--o{ VEHICLE_CLAIM : "submits as claimant"
```

---

## Key Constraints

| Entity | Field | Constraint | Notes |
|--------|-------|-----------|-------|
| `USER` | `email` | UNIQUE | Login identifier |
| `VEHICLE` | `registrationNumber` | UNIQUE | Assembled from 4 BRTA components |
| `VEHICLE` | `ownerNid` | UNIQUE | One vehicle per NID (implicit) |
| `QUOTA` | `vehicle_id` | UNIQUE | One quota record per vehicle |
| `FUEL_STATION` | `stationCode` | UNIQUE | System station identifier |
| `PUMP_REPRESENTATIVE` | `email` | UNIQUE | Work email |
| `PUMP_REPRESENTATIVE` | `employeeId` | UNIQUE | Printed on ID card, used for login |
| `PUMP_REPRESENTATIVE` | `username` | UNIQUE | Pump app login handle |
| `QUOTA_CONFIG` | `configKey` | UNIQUE | Singleton `"DEFAULT"` row |

---

## Registration Number Format

A vehicle registration number is assembled from 4 structured parts:

```
{brtaOfficeCode} {vehicleRegistrationCode} {serialPart1}-{serialPart2}

Example: DHAKA METRO GA 11-1234
         ─────────── ── ──────
         Office Code RC Serial
```

- **BRTA Office Codes** are reference data from the `BRTA_OFFICE` table.
- **Registration Codes** (e.g. `GA`, `KHA`) are reference data from the `REGISTRATION_CODE` table.

---

## Quota Calculation

```
remainingLiters = limitLiters - usedLiters

On dispense:  usedLiters    += dispensedLiters
              remainingLiters -= dispensedLiters

On reset:     usedLiters     = 0
              remainingLiters = limitLiters
```

