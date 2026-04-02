// ─── Auth ────────────────────────────────────────────────────────────────────
export type UserRole = 'CUSTOMER' | 'ADMIN'

export interface AuthUser {
  id: string
  email: string
  name: string
  role: UserRole
}

export interface AuthResponse {
  token: string
  user: AuthUser
}

export interface LoginRequest {
  email: string
  password: string
}

// ─── Vehicle ──────────────────────────────────────────────────────────────────
export type VehicleStatus = 'VERIFIED' | 'UNVERIFIED' | 'DEREGISTERED'

export interface Vehicle {
  id: string
  userId: string
  registrationNumber: string
  brtaOfficeCode: string
  vehicleRegistrationCode: string
  ownerName: string
  ownerNid: string
  ownerMobile: string
  ownerEmail: string
  vehicleMake: string
  vehicleColor: string
  vehicleClass: string
  fuelType: string
  engineDisplacement?: number
  registrationDate: string
  status: VehicleStatus
  createdAt: string
}

export interface RegisterVehicleRequest {
  // Personal
  ownerName: string
  ownerNid: string
  ownerMobile: string
  ownerEmail: string
  password: string
  // Registration number (structured 4-part)
  brtaOfficeCode: string
  vehicleRegistrationCode: string
  serialPart1: string
  serialPart2: string
  // Vehicle details
  vehicleMake: string
  vehicleColor: string
  fuelType: string
  engineDisplacement?: number
  registrationDate: string
}

export interface AddVehicleRequest {
  // Registration number (structured 4-part)
  brtaOfficeCode: string
  vehicleRegistrationCode: string
  serialPart1: string
  serialPart2: string
  // Vehicle details
  vehicleMake: string
  vehicleColor: string
  fuelType: string
  engineDisplacement?: number
  registrationDate: string
}

// ─── Reference Data ───────────────────────────────────────────────────────────
export interface RegistrationCode {
  code: string
  description: string
}

export interface BrtaOffice {
  brtaCode: string
  description: string
}

export interface QrTokenResponse {
  token: string
  vehicleId: string
  registrationNumber: string
  expiresInSeconds: number
}

// ─── Quota ────────────────────────────────────────────────────────────────────
export type QuotaStatus = 'ACTIVE' | 'SUSPENDED' | 'EXPIRED'

export interface Quota {
  id: string
  vehicleId: string
  registrationNumber: string
  ownerName: string
  limitLiters: number
  usedLiters: number
  remainingLiters: number
  period: string          // DAILY | WEEKLY | MONTHLY | QUARTERLY | YEARLY
  resetTimestamp: string
  lastTransactionTimestamp: string | null
  status: QuotaStatus
}

export interface QuotaAdjustRequest {
  newLimitLiters: number
  reason: string
}

// ─── Transaction ──────────────────────────────────────────────────────────────
export type TransactionStatus = 'COMPLETED' | 'CANCELLED' | 'FAILED'

export interface Transaction {
  id: string
  vehicleId: string
  registrationNumber: string
  stationId: string
  stationName: string
  pumpRepresentativeId: string
  amountDispensedLiters: number
  fuelTypeDispensed: string
  transactionTimestamp: string
  remainingQuotaAfter: number
  geofenceVerified: boolean
  status: TransactionStatus
}

// ─── Fuel Station ─────────────────────────────────────────────────────────────
export type StationStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

export interface FuelStation {
  id: string
  stationName: string
  stationCode: string
  latitude: number
  longitude: number
  geofenceRadiusMeters: number
  phoneNumber: string
  managerName: string
  managerEmail: string
  district: string
  registrationDate: string
  status: StationStatus
  createdAt: string
  updatedAt: string
}

export interface StationFormData {
  stationName: string
  stationCode: string
  latitude: string
  longitude: string
  geofenceRadiusMeters: string
  phoneNumber: string
  managerName: string
  managerEmail: string
  district: string
  status: StationStatus
}

// ─── Pump Representative ──────────────────────────────────────────────────────
export type RepStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

export interface PumpRepresentative {
  id: string
  stationId: string
  stationName: string
  name: string
  mobileNumber: string
  email: string
  employeeId: string
  username: string
  status: RepStatus
  lastLoginTimestamp: string | null
  createdAt: string
}

export interface PumpRepFormData {
  stationId: string
  name: string
  mobileNumber: string
  email: string
  employeeId: string
  username: string
  password: string
}

// ─── Audit Log ────────────────────────────────────────────────────────────────
export type AuditAction =
  | 'QUOTA_ADJUSTMENT'
  | 'QUOTA_RESET'
  | 'VEHICLE_APPROVED'
  | 'VEHICLE_REJECTED'
  | 'VEHICLE_REVERIFIED'
  | 'STATION_CREATED'
  | 'STATION_UPDATED'
  | 'STATION_DEACTIVATED'
  | 'USER_SUSPENDED'
  | 'USER_ACTIVATED'
  | 'REP_CREATED'
  | 'REP_UPDATED'

export interface AuditLog {
  id: string
  adminUserId: string
  adminName: string
  actionType: AuditAction
  targetEntity: string
  targetEntityId: string
  oldValue: Record<string, unknown> | null
  newValue: Record<string, unknown> | null
  reasonNotes: string | null
  actionTimestamp: string
}

// ─── Dashboard Stats ──────────────────────────────────────────────────────────
export interface AdminStats {
  totalVehicles: number
  unverifiedVehicles: number
  verifiedVehicles: number
  transactionsToday: number
  activeStations: number
  averageQuotaUsedPercent: number
  totalTransactionsThisWeek: number
  dailyTransactions: { date: string; count: number }[]
  quotaUsageByVehicleClass: { vehicleClass: string; avgUsed: number }[]
}

// ─── Quota Configuration ─────────────────────────────────────────────────────
export type QuotaPeriod = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'

export interface QuotaConfig {
  id: string
  limitLitres: number
  geofenceRadiusMeters: number
  quotaPeriod: QuotaPeriod
  resetCronExpression: string
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface QuotaConfigUpdateRequest {
  limitLitres: number
  geofenceRadiusMeters: number
  quotaPeriod: QuotaPeriod
  resetCronExpression: string
  description?: string
}

// ─── Vehicle Claims ──────────────────────────────────────────────────────────
export type ClaimStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface VehicleClaim {
  id: string
  vehicleId: string
  registrationNumber: string
  claimantUserId: string
  claimantName: string
  claimantNid: string
  reason: string
  status: ClaimStatus
  adminNotes: string | null
  createdAt: string
  updatedAt: string
}

export interface ClaimVehicleRequest {
  registrationNumber: string
  claimantNid: string
  reason: string
}

// ─── Generic ─────────────────────────────────────────────────────────────────
export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
