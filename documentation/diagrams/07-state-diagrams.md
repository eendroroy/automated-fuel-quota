# State Diagrams

> Lifecycle state machines for the four key domain entities:
> Vehicle, Quota, VehicleClaim, and PumpRepresentative.

---

## 1. Vehicle Lifecycle

```mermaid
stateDiagram-v2
    [*] --> VERIFIED : Customer registers vehicle\n(auto-verified on creation)

    VERIFIED --> UNVERIFIED : Admin triggers BRTA re-verify\n(future: BRTA API returns failure)
    UNVERIFIED --> VERIFIED : Admin triggers BRTA re-verify\n(success / admin override)

    VERIFIED --> DEREGISTERED : Customer deregisters vehicle\n(soft delete — history preserved)
    UNVERIFIED --> DEREGISTERED : Admin deregisters unverified vehicle

    DEREGISTERED --> [*] : Terminal state\n(no further transitions)

    note right of VERIFIED
        Fuel dispensing ALLOWED.
        QR token generation enabled.
        Quota is ACTIVE.
    end note

    note right of UNVERIFIED
        Fuel dispensing DENIED.
        QR token generation disabled.
        Future: BRTA API integration.
    end note

    note right of DEREGISTERED
        Soft-deleted — record retained.
        All transactions preserved.
        Quota set to EXPIRED.
    end note
```

---

## 2. Quota Lifecycle

```mermaid
stateDiagram-v2
    [*] --> ACTIVE : Created when vehicle registered\n(limit from QuotaConfig DEFAULT)

    ACTIVE --> ACTIVE : Periodic reset\n(usedLiters = 0, remainingLiters = limit)
    ACTIVE --> ACTIVE : Dispense confirmed\n(usedLiters += dispensed)
    ACTIVE --> ACTIVE : Admin adjusts limit\n(limitLiters updated)

    ACTIVE --> SUSPENDED : Admin suspends quota\n(e.g. fraud investigation)
    SUSPENDED --> ACTIVE : Admin re-activates quota

    ACTIVE --> EXPIRED : Vehicle DEREGISTERED\n(cascading status change)
    SUSPENDED --> EXPIRED : Vehicle DEREGISTERED

    EXPIRED --> [*] : Terminal state

    note right of ACTIVE
        Dispensing allowed.
        Included in periodic reset job.
        remainingLiters recalculated on each dispense.
    end note

    note right of SUSPENDED
        Dispensing DENIED.
        Excluded from periodic reset.
        Admin must re-activate manually.
    end note

    note right of EXPIRED
        Dispensing DENIED.
        Historical record preserved.
        Not included in reset job.
    end note
```

---

## 3. VehicleClaim (Ownership Transfer) Workflow

```mermaid
stateDiagram-v2
    [*] --> PENDING : Customer submits ownership claim\n(registrationNumber + claimantNid + reason)

    PENDING --> APPROVED : Admin reviews and approves
    PENDING --> REJECTED : Admin reviews and rejects

    APPROVED --> [*] : Terminal state\n(Vehicle ownership transferred)
    REJECTED --> [*] : Terminal state\n(Vehicle ownership unchanged)

    note right of PENDING
        Claim is awaiting admin review.
        Vehicle ownership unchanged.
        Customer can view claim status.
    end note

    note right of APPROVED
        Vehicle.user → claimant.
        Vehicle.ownerName → claimant.name.
        Vehicle.ownerEmail → claimant.email.
        Vehicle.ownerNid → claimant.claimantNid.
    end note

    note right of REJECTED
        Admin may add rejection notes.
        Customer notified via claim status.
        Customer may resubmit if appropriate.
    end note
```

---

## 4. PumpRepresentative Account Status

```mermaid
stateDiagram-v2
    [*] --> ACTIVE : Admin creates representative\n(defaulted to ACTIVE)

    ACTIVE --> INACTIVE : Admin deactivates account\n(e.g. employee left)
    INACTIVE --> ACTIVE : Admin re-activates account

    ACTIVE --> SUSPENDED : Admin suspends account\n(e.g. policy violation)
    SUSPENDED --> ACTIVE : Admin lifts suspension

    INACTIVE --> SUSPENDED : Admin suspends inactive account
    SUSPENDED --> INACTIVE : Admin deactivates suspended account

    note right of ACTIVE
        Login to pump portal ALLOWED.
        lastLoginTimestamp updated on login.
    end note

    note right of INACTIVE
        Login to pump portal DENIED.
        Account record preserved.
    end note

    note right of SUSPENDED
        Login to pump portal DENIED.
        Account requires admin intervention.
    end note
```

---

## 5. Transaction Status

```mermaid
stateDiagram-v2
    [*] --> COMPLETED : POST /api/pump/confirm succeeds\n(quota deducted, record saved)

    COMPLETED --> CANCELLED : Future — admin voids transaction

    note right of COMPLETED
        Quota already deducted.
        Idempotency hash stored (QR path).
        Transaction receipt available.
    end note

    note right of CANCELLED
        Future scope: admin void workflow.
        Quota credited back.
        Audit trail preserved.
    end note
```

---

## Combined State Overview

```mermaid
flowchart LR
    subgraph Vehicle["Vehicle Status"]
        V1[VERIFIED]
        V2[UNVERIFIED]
        V3[DEREGISTERED]
        V1 <-->|BRTA reverify| V2
        V1 -->|Deregister| V3
        V2 -->|Deregister| V3
    end

    subgraph Quota["Quota Status"]
        Q1[ACTIVE]
        Q2[SUSPENDED]
        Q3[EXPIRED]
        Q1 <-->|Admin toggle| Q2
        Q1 -->|Vehicle deregistered| Q3
        Q2 -->|Vehicle deregistered| Q3
    end

    subgraph Claim["VehicleClaim Status"]
        C1[PENDING]
        C2[APPROVED]
        C3[REJECTED]
        C1 -->|Admin approves| C2
        C1 -->|Admin rejects| C3
    end

    V3 -->|Cascades to| Q3

    style V1 fill:#2e7d32,color:#fff
    style V2 fill:#f57f17,color:#fff
    style V3 fill:#616161,color:#fff
    style Q1 fill:#2e7d32,color:#fff
    style Q2 fill:#f57f17,color:#fff
    style Q3 fill:#616161,color:#fff
    style C1 fill:#1565c0,color:#fff
    style C2 fill:#2e7d32,color:#fff
    style C3 fill:#c62828,color:#fff
```

