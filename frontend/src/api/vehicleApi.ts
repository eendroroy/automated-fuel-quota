import axiosInstance from './axiosInstance'
import type { Vehicle, PagedResponse, AddVehicleRequest, QrTokenResponse } from '@/types'

// ── Customer: single vehicle (backward-compatible) ────────────────────────────
export const getMyVehicle = () =>
  axiosInstance.get<Vehicle>('/customer/vehicle').then((r) => r.data)

export const getQrToken = () =>
  axiosInstance.get<{ token: string; vehicleId: string }>('/customer/qr-code').then((r) => r.data)

export const regenerateQrToken = () =>
  axiosInstance.post<{ token: string }>('/customer/qr-code/regenerate').then((r) => r.data)

// ── Customer: multi-vehicle management ───────────────────────────────────────
export const getMyVehicles = () =>
  axiosInstance.get<Vehicle[]>('/customer/vehicles').then((r) => r.data)

export const addMyVehicle = (data: AddVehicleRequest) =>
  axiosInstance.post<Vehicle>('/customer/vehicles', data).then((r) => r.data)

export const removeMyVehicle = (vehicleId: string) =>
  axiosInstance.delete(`/customer/vehicles/${vehicleId}`).then((r) => r.data)

export const getQrTokenForVehicle = (vehicleId: string) =>
  axiosInstance.get<QrTokenResponse>(`/customer/vehicles/${vehicleId}/qr-code`).then((r) => r.data)

export const regenerateQrTokenForVehicle = (vehicleId: string) =>
  axiosInstance.post<{ token: string }>(`/customer/vehicles/${vehicleId}/qr-code/regenerate`).then((r) => r.data)

// ── Admin: vehicle management ─────────────────────────────────────────────────
export const getAllVehicles = (params: {
  page?: number
  size?: number
  status?: string
  search?: string
}) =>
  axiosInstance
    .get<PagedResponse<Vehicle>>('/admin/vehicles', { params })
    .then((r) => r.data)

/** Triggers a BRTA re-verification for the given vehicle (currently always succeeds). */
export const reverifyVehicle = (id: string) =>
  axiosInstance.put<Vehicle>(`/admin/vehicles/${id}/reverify`).then((r) => r.data)
