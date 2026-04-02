import axiosInstance from './axiosInstance'
import type { QuotaConfig, QuotaConfigUpdateRequest } from '@/types'

/** Returns the current global quota configuration (admin only). */
export const getQuotaConfig = () =>
  axiosInstance.get<QuotaConfig>('/admin/quota-config').then((r) => r.data)

/** Updates the global quota configuration (admin only). */
export const updateQuotaConfig = (data: QuotaConfigUpdateRequest) =>
  axiosInstance.put<QuotaConfig>('/admin/quota-config', data).then((r) => r.data)

