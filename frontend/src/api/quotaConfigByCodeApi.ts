import axiosInstance from './axiosInstance'
import type { QuotaConfigByRegistrationCode, QuotaConfigByRegistrationCodeRequest } from '@/types'

// ── Admin: quota configuration by registration code ──────────────────────────

export const getAllQuotaConfigsByCode = () =>
  axiosInstance.get<QuotaConfigByRegistrationCode[]>('/admin/quota-config-by-code').then((r) => r.data)

export const getQuotaConfigByCodeId = (id: string) =>
  axiosInstance.get<QuotaConfigByRegistrationCode>(`/admin/quota-config-by-code/${id}`).then((r) => r.data)

export const getQuotaConfigByCode = (code: string) =>
  axiosInstance.get<QuotaConfigByRegistrationCode>(`/admin/quota-config-by-code/code/${code}`).then((r) => r.data)

export const createQuotaConfigByCode = (data: QuotaConfigByRegistrationCodeRequest) =>
  axiosInstance.post<QuotaConfigByRegistrationCode>('/admin/quota-config-by-code', data).then((r) => r.data)

export const updateQuotaConfigByCode = (id: string, data: QuotaConfigByRegistrationCodeRequest) =>
  axiosInstance.put<QuotaConfigByRegistrationCode>(`/admin/quota-config-by-code/${id}`, data).then((r) => r.data)

export const deleteQuotaConfigByCode = (id: string) =>
  axiosInstance.delete(`/admin/quota-config-by-code/${id}`).then((r) => r.data)

