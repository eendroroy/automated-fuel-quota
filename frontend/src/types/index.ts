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
  mobileNumber: string
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
  driverId?: string
  driverName?: string
  driverMobile?: string
  vehicleMake: string
  vehicleColor: string
  vehicleClass: string
  fuelType: string
  /** Secondary fuel types the vehicle can use (e.g. CNG for a petrol car) */
  secondaryFuelTypes?: string[]
  engineDisplacement?: number
  registrationDate: string
  status: VehicleStatus
  createdAt: string
  /** True when the vehicle has an individually admin-overridden quota not managed by config sets */
  customQuotaConfig?: boolean
}

export interface RegisterVehicleRequest {
  // Personal (always required)
  ownerName: string
  ownerNid: string
  ownerMobile: string
  ownerEmail: string
  password: string
  /** 6-digit OTP received on mobile for verification */
  otp: string
  // Registration number (optional - for driver-only registration)
  brtaOfficeCode?: string
  vehicleRegistrationCode?: string
  serialPart1?: string
  serialPart2?: string
  // Vehicle details (optional - for driver-only registration)
  vehicleMake?: string
  vehicleColor?: string
  fuelType?: string
  engineDisplacement?: number
  registrationDate?: string
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
  secondaryFuelTypes?: string[]
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
  /** True when an admin manually adjusted this quota — excluded from bulk sync */
  individuallyOverridden?: boolean
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

// ─── Pump Representative Demo App ─────────────────────────────────────────────
export type RepStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

export interface PumpRepresentative {
  id: string
  stationId: string
  stationName: string
  name: string
  mobileNumber: string
  email?: string
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
  email?: string
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

// ─── Quota Config Sets ────────────────────────────────────────────────────────
export interface RegistrationCodeInfo {
  code: string
  description: string
}

export interface QuotaConfigSet {
  id: string
  name: string
  limitLitres: number
  quotaPeriod: QuotaPeriod
  description: string | null
  registrationCodes: string[]
  registrationCodeDetails: RegistrationCodeInfo[]
  createdAt: string
  updatedAt: string
}

export interface QuotaConfigSetRequest {
  name: string
  limitLitres: number
  quotaPeriod: QuotaPeriod
  description?: string
  registrationCodes: string[]
}

// ─── Driver Assignment ───────────────────────────────────────────────────────
export interface AssignDriverRequest {
  driverMobile: string
}


// ─── Generic ─────────────────────────────────────────────────────────────────
export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// ─── Pump Representative Demo App ─────────────────────────────────────────────
export interface PumpRepLoginRequest {
  mobileNumber: string
}

export interface PumpRepSession {
  id: string
  name: string
  employeeId: string
  stationId: string
  stationName: string
  stationCode: string
}

export type AuthorizationDecision = 'APPROVED' | 'PARTIAL' | 'DENIED'

export interface AuthorizationResult {
  decision: AuthorizationDecision
  authorizedLiters: number
  remainingQuota: number
  totalQuota: number
  message: string | null
  vehicleFound: string
  vehicleMake: string
  vehicleColor: string
  ownerName: string
  vehicleStatus: string
  fuelType: string
}

export interface PumpAuthorizeRequest {
  qrToken: string
  stationId: string
  requestedLiters?: number
}

export interface ManualAuthorizeRequest {
  registrationNumber: string
  stationId: string
  requestedLiters?: number
}

export interface PumpConfirmRequest {
  qrToken?: string           // absent on manual path
  registrationNumber?: string // used on manual path instead of qrToken
  stationId: string
  pumpRepresentativeId: string
  dispensedLiters: number
  fuelType: string
}

export interface PumpConfirmResponse {
  transactionId: string
  transactionReference: string
  dispensedLiters: number
  remainingQuota: number
  timestamp: string
  message: string
}

// ─── User Management ──────────────────────────────────────────────────────────
export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE'

export interface AppUser {
  id: string
  name: string
  mobileNumber: string
  email: string | null
  nid: string
  role: UserRole
  status: UserStatus
  createdAt: string
  lastLoginTimestamp: string | null
  vehicleCount?: number
}

export interface UserStatusUpdateRequest {
  status: UserStatus
  reason?: string
}

