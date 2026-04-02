# Automated Fuel Quota System

A comprehensive **QR-code-driven fuel quota management platform** built with **Spring Boot 4.0.5** and **React 18** that ensures fair, transparent fuel distribution based on a configurable weekly quota policy.

> **📄 Documentation:** [`documentation/BRD.md`](documentation/BRD.md) (requirements) | [`documentation/SRS.md`](documentation/SRS.md) (technical spec)

## 🏗️ System Architecture

### Complete Implementation Status ✅

This project is a **fully functional** fuel quota management system implementing all BRD requirements:

- ✅ **Backend API** - Complete Spring Boot application with security, caching, and scheduling
- ✅ **Frontend SPA** - React/TypeScript customer and admin portals  
- ✅ **Database Schema** - PostgreSQL with proper relationships and indexes
- ✅ **Business Logic** - Weekly quota management with partial dispense support
- ✅ **Security** - JWT authentication with role-based access control
- ✅ **Caching** - Redis integration for performance optimization
- ✅ **Scheduled Jobs** - Automatic weekly quota reset (Sunday 00:00)

### Core Business Logic (Per BRD Requirements)

- ✅ **Weekly 24L Quota** - Strict fuel limit enforcement per vehicle
- ✅ **QR Code Authentication** - Encrypted JWT tokens with 1-hour expiration
- ✅ **GPS Geofencing** - Location-based validation for authorized stations
- ✅ **Partial Dispense Support** - Smart authorization when requested > remaining quota
- ✅ **Automatic Weekly Reset** - Sunday 00:00 scheduled quota restoration
- ✅ **Real-time Validation** - Instant vehicle status and quota checking
- ✅ **Idempotent Transactions** - Prevents double-deducting from quota
- ✅ **Complete Audit Trail** - Every action logged with timestamp and user

## 🚀 Key Features

### Vehicle Owner App (Customer Portal)
- **Multi-step Registration** - Personal info → Vehicle details → Document upload
- **QR Code Management** - Generate, regenerate, and download fuel QR codes  
- **Quota Dashboard** - Visual gauge showing used vs. remaining fuel allocation
- **Transaction History** - Complete record of fuel dispensing activities
- **Real-time Updates** - Live quota status and transaction notifications

### Admin Dashboard
- **Vehicle Management** - Approve, reject, or suspend vehicle registrations
- **Fuel Station Management** - CRUD operations with GPS coordinate validation
- **Quota Administration** - Adjust limits, manual resets, and bulk operations
- **Analytics & Reporting** - Usage charts, transaction trends, and system metrics
- **Audit Log Viewer** - Searchable, filterable administrative action history

### Pump Representative API (Core BRD Implementation)
- **QR Code Scanning** - Validate and decode vehicle authorization tokens
- **Authorization Engine** - Real-time eligibility, quota, and geofence checking
- **Transaction Recording** - Dispense confirmation with quota deduction  
- **Receipt Generation** - Transaction details with remaining quota balance

## 🛠️ Technology Stack

### Backend
- **Spring Boot 4.0.5** with Java 25
- **Spring Security** with JWT authentication
- **Spring Data JPA** with PostgreSQL
- **Spring Cache** with Redis
- **Spring Scheduler** for quota reset jobs
- **Bean Validation** for request validation

### Frontend
- **React 18** with TypeScript
- **Vite** for build tooling and HMR
- **Tailwind CSS** for responsive styling
- **React Router v6** for client-side routing
- **Axios** for API communication
- **React QR Code** for QR generation
- **Recharts** for admin analytics

### Infrastructure
- **PostgreSQL 15+** - Primary database
- **Redis 7+** - Caching layer
- **Maven** - Java dependency management
- **npm** - Node.js package management

## 📋 Quick Start

### Prerequisites
- Java 25 (or compatible JDK)
- Node.js 20+ and npm
- PostgreSQL 15+
- Redis 7+ (optional but recommended)
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

## 🔧 API Endpoints

### Authentication
```
POST /api/auth/customer/login     - Customer login
POST /api/auth/admin/login        - Admin login  
POST /api/auth/customer/register  - Vehicle registration
```

### Customer API (JWT Required)
```
GET  /api/customer/vehicle        - Get vehicle info
GET  /api/customer/quota          - Get quota status  
GET  /api/customer/qr-code        - Generate QR token
POST /api/customer/qr-code/regenerate - Regenerate QR
GET  /api/customer/transactions   - Transaction history
```

### Pump Representative API (Public - Core BRD)
```
POST /api/pump/authorize          - Authorize fuel dispensing ⭐
POST /api/pump/confirm            - Confirm fuel dispensed ⭐
GET  /api/pump/health            - API health check
```

### Admin API (JWT + Admin Role Required)
```
GET    /api/admin/vehicles        - List vehicles (paginated)
PUT    /api/admin/vehicles/{id}/approve - Approve vehicle
GET    /api/admin/stations        - List fuel stations
POST   /api/admin/stations        - Create station
GET    /api/admin/stats           - Dashboard statistics
```

## 🔄 Core Business Process Flow

### 1. Vehicle Registration & Approval
```
Customer Registration → Admin Review → Vehicle Approval → 
Quota Creation → QR Code Generation → Ready for Use
```

### 2. Fuel Dispensing Transaction (Core BRD Flow)
```
QR Code Scan → Token Validation → Vehicle Verification → 
Geofence Check → Quota Authorization → Fuel Dispense → 
Transaction Recording → Quota Update
```

### 3. Weekly Quota Reset (Automated)
```
Sunday 00:00 Trigger → Reset All Quotas → Clear Cache → 
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
- **Cache Monitoring** - Redis performance metrics
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
AutomatedFuelQuota/
├── src/main/java/com/reddotdigitalit/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST API controllers
│   ├── dto/            # Data transfer objects  
│   ├── entity/         # JPA entities
│   ├── exception/      # Exception handling
│   ├── repository/     # Data access layer
│   ├── security/       # JWT and security config
│   └── service/        # Business logic layer
├── src/main/resources/
│   ├── application.yaml # Application configuration
│   └── static/         # Frontend build output
├── frontend/           # React/TypeScript SPA
│   ├── src/components/ # Reusable components
│   ├── src/pages/      # Page components  
│   ├── src/services/   # API client services
│   └── src/types/      # TypeScript definitions
└── documentation/      # BRD and design docs
```

## 🎯 Implementation Highlights

This implementation fully satisfies all **Business Requirements Document (BRD)** specifications:

### Functional Requirements Implemented ✅
- **FR-01 through FR-15** - All vehicle owner, pump representative, and backend requirements
- **FR-16 & FR-17** - Complete database persistence and auditability  

### Non-Functional Requirements Met ✅
- **NFR-01 through NFR-13** - Security, performance, reliability, and observability

### Business Rules Enforced ✅
- **BR-1 through BR-5** - Weekly quota limits, partial dispense, automatic reset, eligibility checks

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)  
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**🎉 Ready for Production Use** - Complete fuel quota management system implementing all BRD requirements with modern technology stack and best practices.
