import axiosInstance from './axiosInstance'
import type { VehicleClaim, ClaimVehicleRequest, PagedResponse } from '@/types'

// ── Customer ──────────────────────────────────────────────────────────────────

/** Submits an ownership claim for an already-registered vehicle. */
export const claimVehicle = (data: ClaimVehicleRequest) =>
  axiosInstance.post<VehicleClaim>('/customer/vehicles/claim', data).then((r) => r.data)

/** Returns paginated claims submitted by the authenticated customer. */
export const getMyClaims = (params?: { page?: number; size?: number }) =>
  axiosInstance.get<PagedResponse<VehicleClaim>>('/customer/vehicles/claims', { params }).then((r) => r.data)

// ── Admin ─────────────────────────────────────────────────────────────────────

/** Returns paginated vehicle ownership claims for admin review. */
export const getAllClaims = (params: { page?: number; size?: number; status?: string }) =>
  axiosInstance
    .get<PagedResponse<VehicleClaim>>('/admin/vehicle-claims', { params })
    .then((r) => r.data)

/** Approves a pending vehicle ownership claim. */
export const approveClaim = (claimId: string, adminNotes?: string) =>
  axiosInstance
    .put<VehicleClaim>(`/admin/vehicle-claims/${claimId}/approve`, { adminNotes })
    .then((r) => r.data)

/** Rejects a pending vehicle ownership claim. */
export const rejectClaim = (claimId: string, adminNotes?: string) =>
  axiosInstance
    .put<VehicleClaim>(`/admin/vehicle-claims/${claimId}/reject`, { adminNotes })
    .then((r) => r.data)

