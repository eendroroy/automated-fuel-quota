# System Diagrams

This directory contains all architectural and design diagrams for the **Automated Fuel Quota Management System**.

All diagrams are written in [Mermaid](https://mermaid.js.org/) syntax and render natively in:
- GitHub / GitLab markdown
- JetBrains IDEs (built-in Mermaid preview)
- VS Code with the Mermaid plugin
- Any Markdown viewer with Mermaid support

---

## Diagram Index

| # | File | Type | Description |
|---|------|------|-------------|
| 01 | [System Architecture](01-architecture.md) | Architecture | System context, container, and component overview |
| 02 | [Entity Relationship](02-entity-relationship.md) | Data Model | Full database entity relationships and field reference |
| 03 | [QR Authorization Flow](03-sequence-qr-authorization.md) | Sequence | Primary fuel dispensing sequence (QR token path) |
| 04 | [Manual Authorization Flow](04-sequence-manual-authorization.md) | Sequence | Fallback fuel dispensing sequence (manual registration number path) |
| 05 | [Customer Registration Flow](05-sequence-registration.md) | Sequence | Customer self-registration and vehicle onboarding sequence |
| 06 | [Quota Reset Flow](06-sequence-quota-reset.md) | Sequence | Scheduled periodic quota reset job sequence |
| 07 | [State Diagrams](07-state-diagrams.md) | State Machine | Lifecycle states for Vehicle, Quota, VehicleClaim, and PumpRepresentative |
| 08 | [Frontend Component Diagram](08-component-diagram.md) | Component | React SPA layout, routing hierarchy, and page components |
| 09 | [Use Case Diagram](09-use-case.md) | Use Case | Actor–use case relationships for all user roles |
| 10 | [Deployment Architecture](10-deployment.md) | Deployment | Development and production deployment topologies |

---

## Related Documentation

- [`../BRD.md`](../BRD.md) — Business Requirements Document
- [`../SRS.md`](../SRS.md) — Software Requirements Specification
- [`../USER_JOURNEY.md`](../USER_JOURNEY.md) — Detailed user journey maps
- [`../../AGENTS.md`](../../AGENTS.md) — AI coding agent guide

