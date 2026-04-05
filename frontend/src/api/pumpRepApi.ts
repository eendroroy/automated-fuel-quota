import axiosInstance from './axiosInstance'
import type { PumpRepresentative, PumpRepFormData, PagedResponse } from '@/types'

export const getAllPumpReps = (params?: { page?: number; size?: number; stationId?: string }) =>
  axiosInstance
    .get<PagedResponse<PumpRepresentative>>('/admin/v1/pump-representatives', { params })
    .then((r) => r.data)

export const createPumpRep = (data: PumpRepFormData) =>
  axiosInstance.post<PumpRepresentative>('/admin/v1/pump-representatives', data).then((r) => r.data)

export const updatePumpRep = (id: string, data: Partial<PumpRepFormData>) =>
  axiosInstance
    .put<PumpRepresentative>(`/admin/v1/pump-representatives/${id}`, data)
    .then((r) => r.data)

export const togglePumpRepStatus = (id: string, status: 'ACTIVE' | 'SUSPENDED') =>
  axiosInstance
    .put(`/admin/v1/pump-representatives/${id}/status`, { status })
    .then((r) => r.data)

export const deletePumpRep = (id: string) =>
  axiosInstance.delete(`/admin/v1/pump-representatives/${id}`).then((r) => r.data)
