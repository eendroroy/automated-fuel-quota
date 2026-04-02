import axiosInstance from './axiosInstance'
import type { Quota, QuotaAdjustRequest, PagedResponse } from '@/types'

export const getMyQuota = () =>
  axiosInstance.get<Quota>('/customer/quota').then((r) => r.data)

export const getAllQuotas = (params: { page?: number; size?: number; search?: string }) =>
  axiosInstance.get<PagedResponse<Quota>>('/admin/quotas', { params }).then((r) => r.data)

export const adjustQuota = (vehicleId: string, data: QuotaAdjustRequest) =>
  axiosInstance.put(`/admin/quotas/${vehicleId}/adjust`, data).then((r) => r.data)

export const manualResetQuota = (vehicleId: string) =>
  axiosInstance.post(`/admin/quotas/${vehicleId}/reset`).then((r) => r.data)

export const bulkResetQuotas = () =>
  axiosInstance.post('/admin/quotas/bulk-reset').then((r) => r.data)

