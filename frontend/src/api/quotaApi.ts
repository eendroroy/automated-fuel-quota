import axiosInstance from './axiosInstance'
import type { Quota, QuotaAdjustRequest, PagedResponse } from '@/types'

export const getMyQuota = () =>
  axiosInstance.get<Quota>('/customer/v1/quota').then((r) => r.data)

export const getVehicleQuota = (vehicleId: string) =>
  axiosInstance.get<Quota>(`/customer/v1/vehicles/${vehicleId}/quota`).then((r) => r.data)

export const getAllQuotas = (params: {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
  sortBy?: string;
  sortOrder?: string;
}) =>
  axiosInstance.get<PagedResponse<Quota>>('/admin/v1/quotas', { params }).then((r) => r.data)

export const adjustQuota = (vehicleId: string, data: QuotaAdjustRequest) =>
  axiosInstance.put(`/admin/v1/quotas/${vehicleId}/adjust`, data).then((r) => r.data)

export const manualResetQuota = (vehicleId: string) =>
  axiosInstance.post(`/admin/v1/quotas/${vehicleId}/reset`).then((r) => r.data)

export const bulkResetQuotas = () =>
  axiosInstance.post('/admin/v1/quotas/bulk-reset').then((r) => r.data)
