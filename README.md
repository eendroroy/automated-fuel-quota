# Automated Fuel Quota System

A comprehensive **QR-code-driven fuel quota management platform** built with **Spring Boot 4.0.5** and **React 18** that ensures fair, transparent fuel distribution based on a configurable weekly quota policy.

> **📄 Documentation:**
> [`documentation/BRD.md`](documentation/BRD.md) (requirements) |
> [`documentation/SRS.md`](documentation/SRS.md) (technical spec) |
> [`documentation/USER_JOURNEY.md`](documentation/USER_JOURNEY.md) (user journeys)

---

## 🏗️ System Architecture

```mermaid
graph TD
    subgraph Users
        C["🧑 Customer<br/>(Vehicle Owner)"]
        A["👤 Admin"]
        P["🔧 Pump Rep"]
    end

    subgraph SpringBoot["Spring Boot 4.0.5 — :8080"]
        API["REST API<br/>/api/**"]
        SPA["React 18 SPA<br/>(embedded static files)"]
        SEC["JWT Security<br/>+ RBAC"]
        SCHED["Quota Reset<br/>Scheduler"]
    end

    subgraph Data
        PG[("PostgreSQL 15+")]
    end

    C -->|JWT /api/customer/*| SEC
    A -->|JWT /api/admin/*| SEC
    P -->|Public /api/pump/*| API
    SEC --> API
    API --> PG
    SCHED --> PG

    style SpringBoot fill:#1b5e20,color:#fff
    style Data fill:#37474f,color:#fff
```


### Complete Implementation Status ✅

This project is a **fully functional** fuel quota management system implementing all BRD requirements plus additional features:

- ✅ **Backend API** - Complete Spring Boot application with security and scheduling
- ✅ **Frontend SPA** - React/TypeScript customer and admin portals
- ✅ **Pump Rep Web Portal** - Browser-based portal for pump representatives with QR scanning and manual lookup
- ✅ **Database Schema** - PostgreSQL with proper relationships and indexes
- ✅ **Business Logic** - Configurable quota management with partial dispense support
- ✅ **Security** - JWT authentication with role-based access control
- ✅ **Scheduled Jobs** - Automatic quota reset based on configurable period
- ✅ **Driver-Only Registration** - Users can register without owning vehicles
- ✅ **Quota by Registration Code** - Different quota limits per vehicle category
- ✅ **Driver Assignment** - Vehicle owners can assign drivers with full authorization

### Core Business Logic (Per BRD Requirements)

- ✅ **Configurable Quota** - Flexible limit enforcement per vehicle (default 24L weekly)
- ✅ **Registration-Code-Specific Quotas** - Different limits per vehicle category (LA = 20L DAILY, GA = 30L WEEKLY)
- ✅ **QR Code Authentication** - Encrypted JWT tokens with 1-hour expiration
- ✅ **GPS Geofencing** - Location-based validation for authorized stations
- ✅ **Partial Dispense Support** - Smart authorization when requested > remaining quota
- ✅ **Configurable Period Reset** - Scheduled quota restoration (DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY)
- ✅ **Real-time Validation** - Instant vehicle status and quota checking
- ✅ **Idempotent Transactions** - Prevents double-deducting from quota on QR path
- ✅ **Complete Audit Trail** - Every action logged with timestamp and user

## 🚀 Key Features

### Vehicle Owner App (Customer Portal)
- **Flexible Registration** - Register as vehicle owner OR driver-only account (optional vehicle)
- **Multi-step Registration** - Personal info → Vehicle details (optional) → Review & submit
- **QR Code Management** - Generate, regenerate, and download fuel QR codes  
- **Driver Management** - Assign registered drivers to vehicles; both owner and driver can generate QR codes
- **Quota Dashboard** - Visual gauge showing used vs. remaining fuel allocation
- **Transaction History** - Complete record of fuel dispensing activities
- **Vehicle Ownership Claims** - Submit and track ownership transfer requests
- **Vehicles as Driver** - View and manage vehicles where user is assigned as driver

### Admin Dashboard
- **Vehicle Management** - Approve, reject, or suspend vehicle registrations
- **Fuel Station Management** - CRUD operations with GPS coordinate validation
- **Quota Administration** - Adjust limits, manual resets, and bulk operations
- **Quota Config by Registration Code** - Set different quota limits per vehicle category (LA = 20L DAILY, GA = 30L WEEKLY, etc.)
- **Analytics & Reporting** - Usage charts, transaction trends, and system metrics
- **Audit Log Viewer** - Searchable, filterable administrative action history

### Pump Representative Web Portal (`/pump`)
- **Employee ID Login** - Authenticate with employee code; station info shown after login
- **QR Code Scanner** - Camera-based scanning via `html5-qrcode` library
- **Manual Fallback** - Enter vehicle registration number directly when QR is unavailable
- **Vehicle Verification Panel** - Shows registration number, BRTA status badge, owner name, vehicle make/color
- **Quota Progress Bar** - Color-coded remaining/total quota display (green → yellow → red)
- **On-screen Numeric Keypad** - Mobile-friendly fuel amount entry (4 digits + 2 decimals)
- **Fuel Type Selector** - Dropdown pre-populated from vehicle's registered fuel type
- **Transaction Receipt** - Reference number, dispensed amount, and remaining quota shown after confirmation

## 🛠️ Technology Stack

### Backend
- **Spring Boot 4.0.5** with Java 25
- **Spring Security** with JWT authentication
- **Spring Data JPA** with PostgreSQL
- **Spring Scheduler** for quota reset jobs
- **Bean Validation** for request validation

### Frontend
- **React 18** with TypeScript
- **Vite** for build tooling and HMR
- **Tailwind CSS** for responsive styling
- **React Router v6** for client-side routing
- **Axios** for API communication
- **React QR Code** for QR generation (customer portal)
- **html5-qrcode** for QR scanning (pump rep portal)
- **Recharts** for admin analytics

### Infrastructure
- **PostgreSQL 15+** - Primary database
- **Maven** - Java dependency management
- **npm** - Node.js package management

## 📋 Quick Start

> For complete user journey maps, see [`documentation/USER_JOURNEY.md`](documentation/USER_JOURNEY.md).

### Prerequisites
- Java 25 (or compatible JDK)
- Node.js 20+ and npm
- PostgreSQL 15+
- Maven 3.9+

### 1. Database Setup

```sql
CREATE DATABASE automated_fuel_quota;
CREATE USER fuel_user WITH PASSWORD 'fuel_password';
GRANT ALL PRIVILEGES ON DATABASE automated_fuel_quota TO fuel_user;
```

### 2. Configure Application

Update `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/automated_fuel_quota
    username: fuel_user
    password: fuel_password
```

### 3. Run the Application

```bash
# Build and run the complete application
./mvnw clean install
./mvnw spring-boot:run

# Access the application
# Frontend: http://localhost:8080
# API: http://localhost:8080/api
```

### 4. Default Login Credentials

**Admin Portal**: http://localhost:8080/admin/login
- Email: `admin@fuelquota.gov`
- Password: `admin123`

**Customer Registration**: http://localhost:8080/register
- Register new vehicle owners through the self-service portal

**Pump Representative Portal**: http://localhost:8080/pump
- Enter the employee ID of any active pump representative (created via Admin → Pump Reps)

## 🔧 API Endpoints

### Authentication
```
POST /api/auth/customer/login     - Customer login
POST /api/auth/admin/login        - Admin login  
POST /api/auth/customer/register  - Vehicle registration
```

### Customer API (JWT Required)
```
GET  /api/customer/vehicles                    - List own vehicles
POST /api/customer/vehicles                    - Add new vehicle
GET  /api/customer/vehicles-as-driver          - List vehicles where user is driver
POST /api/customer/vehicles/{id}/driver        - Assign driver to vehicle
DELETE /api/customer/vehicles/{id}/driver      - Remove driver from vehicle
GET  /api/customer/quota                       - Get quota status  
GET  /api/customer/qr-code                     - Generate QR token
POST /api/customer/qr-code/regenerate          - Regenerate QR
GET  /api/customer/transactions                - Transaction history
```

### Pump Representative API (Public — Core BRD)
```
POST /api/pump/login              - Employee ID login → session details ⭐
POST /api/pump/authorize          - Authorize via QR token ⭐
POST /api/pump/authorize-manual   - Authorize via registration number ⭐
POST /api/pump/confirm            - Confirm fuel dispensed ⭐
GET  /api/pump/health             - API health check
```

### Admin API (JWT + Admin Role Required)
```
GET    /api/admin/vehicles                       - List vehicles (paginated)
PUT    /api/admin/vehicles/{id}/reverify         - Re-verify vehicle
GET    /api/admin/stations                       - List fuel stations
POST   /api/admin/stations                       - Create station
GET    /api/admin/quota-config-by-code           - List quota configs by registration code
POST   /api/admin/quota-config-by-code           - Create quota config for registration code
PUT    /api/admin/quota-config-by-code/{id}      - Update quota config
DELETE /api/admin/quota-config-by-code/{id}      - Delete quota config
GET    /api/admin/stats                          - Dashboard statistics
```

## 🔄 Core Business Process Flow

### 1. Vehicle Registration & Approval
```
Customer Registration → Auto-VERIFIED → Quota Created → QR Code Ready
```

### 2. Fuel Dispensing — QR Path (Primary)
```
Rep Login (employee ID) → QR Code Scan → Token Validation →
Vehicle Verification → Geofence Check → Quota Authorization →
Enter Amount (numeric keypad) → Confirm → Transaction Recorded → Receipt
```

### 3. Fuel Dispensing — Manual Path (Fallback)
```
Rep Login (employee ID) → Enter Registration Number → Vehicle Lookup →
Vehicle Verification → Quota Authorization →
Enter Amount (numeric keypad) → Confirm → Transaction Recorded → Receipt
```

### 4. Weekly Quota Reset (Automated)
```
Sunday 00:00 Trigger → Reset All Quotas →
Audit Logging → System Ready for New Week
```

## 📊 System Capabilities

### Performance Targets
- **Authorization Response Time**: < 2 seconds
- **Concurrent Users**: 10,000+
- **Transaction Throughput**: 1000+ TPS
- **System Uptime**: 99.9%
- **Weekly Reset Success**: 100%

### Security Features  
- ✅ HTTPS/TLS encryption required
- ✅ JWT tokens with secure 256-bit secrets
- ✅ QR tokens expire after 1 hour
- ✅ SQL injection prevention via JPA
- ✅ Input validation on all endpoints
- ✅ Role-based access control
- ✅ Complete audit logging

### Monitoring & Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Centralized Logging** - Structured application logs  
- **Database Monitoring** - Connection pool and query performance

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run integration tests  
./mvnw verify

# Frontend tests
cd frontend && npm test
```

## 🏭 Production Deployment

The application is containerizable and cloud-ready:

```bash
# Build production build
./mvnw clean package -Pprod

# Frontend production build (embedded in Spring Boot)
cd frontend && npm run copy-to-static

# Run production JAR
java -jar target/automated-fuel-quota-0.0.1-SNAPSHOT.jar
```

## 📚 Project Structure

```
automated-fuel-quota/
├── documentation/
│   ├── BRD.md                    # Business Requirements Document
│   ├── SRS.md                    # Software Requirements Specification
│   └── USER_JOURNEY.md           # User journey maps (all actor types)
├── src/main/java/io/github/eendroroy/fuelquota/
│   ├── config/          # Security, OpenAPI, DataInitializer
│   ├── controller/      # REST controllers (Auth, Customer, Admin, Pump)
│   ├── dto/             # Request/Response DTOs
│   ├── entity/          # JPA entities
│   ├── enums/           # Domain enumerations
│   ├── exception/       # Global exception handling
│   ├── repository/      # JPA repositories (with Specification support)
│   ├── security/        # JWT provider and filter
│   └── service/         # Business logic layer
├── src/main/resources/
│   ├── application.yaml # Application configuration
│   └── static/          # Frontend build output (served by Spring Boot)
├── frontend/            # React 18 / TypeScript SPA
│   └── src/
│       ├── api/         # Axios API clients (axiosInstance + pumpApi)
│       ├── components/  # Reusable UI components
│       ├── layouts/     # PublicLayout, CustomerLayout, AdminLayout, PumpRepLayout
│       ├── pages/
│       │   ├── customer/    # Customer portal pages
│       │   ├── admin/       # Admin portal pages
│       │   └── pump/        # Pump rep portal (Login, Scan, Dispense)
│       └── types/           # Shared TypeScript interfaces
└── AGENTS.md            # AI coding agent guide
```

## 🎯 Implementation Highlights

This implementation fully satisfies all **Business Requirements Document (BRD)** specifications:

### Functional Requirements Implemented ✅
- **FR-01 through FR-10** — All vehicle owner portal requirements
- **FR-11 through FR-20** — Full pump representative web portal (login, scan, manual entry, dispense, receipt)
- **FR-21 through FR-30** — Complete admin portal
- **FR-31 through FR-33** — Backend infrastructure (scheduler, transactions, audit)

### Non-Functional Requirements Met ✅
- **NFR-01 through NFR-13** — Security, performance, reliability, and observability

### Business Rules Enforced ✅
- **BR-1 through BR-9** — Weekly quota limits, partial dispense, automatic reset, eligibility checks, pump rep login, manual authorization

---

**🎉 Ready for Production Use** - Complete fuel quota management system implementing all BRD requirements with modern technology stack and best practices.

---

## 📖 Documentation Index

| Document | Description |
|----------|-------------|
| [`documentation/BRD.md`](documentation/BRD.md) | Business Requirements Document — requirements, rules, acceptance tests |
| [`documentation/SRS.md`](documentation/SRS.md) | Software Requirements Specification — API contracts, entity schemas |
| [`documentation/USER_JOURNEY.md`](documentation/USER_JOURNEY.md) | Detailed user journey maps for Customer, Pump Rep, Admin, and System |
| [`AGENTS.md`](AGENTS.md) | AI coding agent guide — patterns, conventions, and domain model |

