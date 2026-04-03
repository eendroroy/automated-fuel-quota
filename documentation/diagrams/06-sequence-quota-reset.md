# Scheduled Quota Reset Flow

> The automated periodic quota reset job that zeroes all used-litre counters.
> Triggered by a configurable Spring cron expression.
> Corresponds to BR-4, FR-31, NFR-09, ATS-06.

---

## Scheduled Reset Sequence

```mermaid
sequenceDiagram
    participant Scheduler as Spring @Scheduled<br/>Quota Reset Job
    participant QuotaSvc as QuotaService<br/>resetAllQuotas()
    participant QuotaCfgRepo as QuotaConfigRepository
    participant QuotaRepo as QuotaRepository<br/>(bulk UPDATE)
    participant AuditSvc as AuditLogService
    participant DB as PostgreSQL

    Note over Scheduler: Cron fires per configured expression<br/>Default: "0 0 0 ? * SUN" (Sunday 00:00)

    Scheduler->>QuotaSvc: resetAllQuotas()
    QuotaSvc->>QuotaSvc: log("Quota reset job started")

    QuotaSvc->>QuotaCfgRepo: findByConfigKey("DEFAULT")
    QuotaCfgRepo-->>QuotaSvc: QuotaConfig { limitLitres, resetCronExpression, ... }

    QuotaSvc->>DB: BEGIN TRANSACTION
    QuotaSvc->>QuotaRepo: resetAllActiveQuotas(limitLitres)
    Note over QuotaRepo: Bulk UPDATE:<br/>UPDATE quotas<br/>SET used_liters = 0,<br/>    remaining_liters = :limitLitres,<br/>    reset_timestamp = NOW()<br/>WHERE status = 'ACTIVE'
    DB-->>QuotaSvc: rowsAffected (count of reset quotas)
    QuotaSvc->>DB: COMMIT


    QuotaSvc->>AuditSvc: logQuotaReset(rowsAffected, timestamp)
    AuditSvc->>DB: INSERT INTO audit_logs<br/>{ actionType=QUOTA_RESET, targetEntity=QUOTA,<br/>  newValue={ resetCount: N, timestamp: "..." } }

    QuotaSvc->>QuotaSvc: log("Quota reset complete — N quotas reset")
```

---

## Manual Per-Vehicle Reset (Admin Portal)

```mermaid
sequenceDiagram
    actor Admin as 👤 Admin
    participant AdminPortal as React SPA<br/>(AdminQuotasPage)
    participant AdminAPI as AdminController<br/>/api/admin/quotas/{vehicleId}/reset
    participant QuotaSvc as QuotaService
    participant QuotaRepo as QuotaRepository
    participant AuditSvc as AuditLogService

    Admin->>AdminPortal: Finds vehicle in quota list
    Admin->>AdminPortal: Clicks "Reset Quota"
    AdminPortal->>AdminAPI: POST /api/admin/quotas/{vehicleId}/reset<br/>Authorization: Bearer {adminJwt}

    AdminAPI->>QuotaSvc: resetQuotaForVehicle(vehicleId)
    QuotaSvc->>QuotaRepo: findByVehicleId(vehicleId)
    QuotaRepo-->>QuotaSvc: Quota (ACTIVE)

    QuotaSvc->>QuotaRepo: save(quota) — reset used/remaining
    Note over QuotaRepo: usedLiters = 0<br/>remainingLiters = limitLiters<br/>resetTimestamp = NOW()


    QuotaSvc->>AuditSvc: log(QUOTA_RESET, vehicleId, adminId)

    QuotaSvc-->>AdminAPI: updated QuotaResponse
    AdminAPI-->>AdminPortal: 200 OK — QuotaResponse
    AdminPortal-->>Admin: ✅ Quota reset — remaining = limitLiters
```

---

## Quota Configuration Flow (Admin Changes Settings)

```mermaid
sequenceDiagram
    actor Admin as 👤 Admin
    participant AdminPortal as React SPA<br/>(AdminQuotaConfigPage)
    participant AdminAPI as AdminController<br/>/api/admin/quota-config
    participant QuotaCfgSvc as QuotaConfigService
    participant QuotaCfgRepo as QuotaConfigRepository
    participant AuditSvc as AuditLogService

    Admin->>AdminPortal: Opens Quota Configuration page
    AdminPortal->>AdminAPI: GET /api/admin/quota-config
    AdminAPI-->>AdminPortal: QuotaConfigResponse { limitLitres, period, cron, geofenceRadius }

    Admin->>AdminPortal: Changes limit to 30L, period to WEEKLY
    Admin->>AdminPortal: Clicks "Save Configuration"
    AdminPortal->>AdminAPI: PUT /api/admin/quota-config<br/>{ limitLitres: 30, quotaPeriod: WEEKLY,<br/>  resetCronExpression: "0 0 0 ? * SUN",<br/>  geofenceRadiusMeters: 100 }

    AdminAPI->>QuotaCfgSvc: updateConfig(request)
    QuotaCfgSvc->>QuotaCfgRepo: findByConfigKey("DEFAULT")
    QuotaCfgRepo-->>QuotaCfgSvc: existing QuotaConfig
    QuotaCfgSvc->>QuotaCfgRepo: save(updatedConfig)

    Note over QuotaCfgSvc: New limit applies to FUTURE quota creations.<br/>Existing individual vehicle quotas are NOT<br/>automatically changed (admin must adjust manually).

    QuotaCfgSvc->>AuditSvc: log(QUOTA_CONFIG_UPDATED, ...)
    QuotaCfgSvc-->>AdminAPI: QuotaConfigResponse
    AdminAPI-->>AdminPortal: 200 OK — updated config
    AdminPortal-->>Admin: ✅ Configuration saved
```

---

## Summary Flow

```mermaid
flowchart TD
    A([Cron fires\ne.g. Sunday 00:00]) --> B[QuotaService.resetAllQuotas]
    B --> C[Bulk UPDATE quotas\nused=0, remaining=limit]
    C --> D[Write audit log\nQUOTA_RESET]
    D --> E([All quotas reset\nSystem ready for new period])

    G([Admin manually resets\nindividual vehicle]) --> H[POST /api/admin/quotas/id/reset]
    H --> C2[UPDATE single quota\nused=0]
    C2 --> D2[Write audit log]
    D2 --> F2([Quota reset for that vehicle])

    style F fill:#2e7d32,color:#fff
    style F2 fill:#2e7d32,color:#fff
 ```
