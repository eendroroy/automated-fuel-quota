import axiosInstance from './axiosInstance'
import type { Vehicle, PagedResponse, AddVehicleRequest, QrTokenResponse, AssignDriverRequest } from '@/types'

// ── Customer: single vehicle (backward-compatible) ────────────────────────────
export const getMyVehicle = () =>
  axiosInstance.get<Vehicle>('/customer/v1/vehicle').then((r) => r.data)

export const getQrToken = () =>
  axiosInstance.get<{ token: string; vehicleId: string }>('/customer/v1/qr-code').then((r) => r.data)

export const regenerateQrToken = () =>
  axiosInstance.post<{ token: string }>('/customer/v1/qr-code/regenerate').then((r) => r.data)

// ── Customer: multi-vehicle management ───────────────────────────────────────
export const getMyVehicles = (params?: { page?: number; size?: number }) =>
  axiosInstance.get<PagedResponse<Vehicle>>('/customer/v1/vehicles', { params }).then((r) => r.data)

export const addMyVehicle = (data: AddVehicleRequest) =>
  axiosInstance.post<Vehicle>('/customer/v1/vehicles', data).then((r) => r.data)

export const removeMyVehicle = (vehicleId: string) =>
  axiosInstance.delete(`/customer/v1/vehicles/${vehicleId}`).then((r) => r.data)

export const getQrTokenForVehicle = (vehicleId: string) =>
  axiosInstance.get<QrTokenResponse>(`/customer/v1/vehicles/${vehicleId}/qr-code`).then((r) => r.data)

export const regenerateQrTokenForVehicle = (vehicleId: string) =>
  axiosInstance.post<{ token: string }>(`/customer/v1/vehicles/${vehicleId}/qr-code/regenerate`).then((r) => r.data)

// ── Customer: driver assignment ───────────────────────────────────────────────
export const assignDriver = (vehicleId: string, data: AssignDriverRequest) =>
  axiosInstance.post<Vehicle>(`/customer/v1/vehicles/${vehicleId}/driver`, data).then((r) => r.data)

export const removeDriver = (vehicleId: string) =>
  axiosInstance.delete<Vehicle>(`/customer/v1/vehicles/${vehicleId}/driver`).then((r) => r.data)

export const getVehiclesAsDriver = () =>
  axiosInstance.get<Vehicle[]>('/customer/v1/vehicles-as-driver').then((r) => r.data)

// ── Admin: vehicle management ─────────────────────────────────────────────────
export const getAllVehicles = (params: {
  page?: number
  size?: number
  status?: string
  search?: string
}) =>
  axiosInstance
    .get<PagedResponse<Vehicle>>('/admin/v1/vehicles', { params })
    .then((r) => r.data)

/** Triggers a BRTA re-verification for the given vehicle (currently always succeeds). */
export const reverifyVehicle = (id: string) =>
  axiosInstance.put<Vehicle>(`/admin/v1/vehicles/${id}/reverify`).then((r) => r.data)
