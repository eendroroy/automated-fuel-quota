import axiosInstance from './axiosInstance'
import type { QuotaConfig, QuotaConfigUpdateRequest, QuotaConfigSet, QuotaConfigSetRequest } from '@/types'

// ── Global quota configuration ────────────────────────────────────────────────

/** Returns the current global quota configuration (admin only). */
export const getQuotaConfig = () =>
  axiosInstance.get<QuotaConfig>('/admin/v1/quota-config').then((r) => r.data)

/** Updates the global quota configuration (admin only). */
export const updateQuotaConfig = (data: QuotaConfigUpdateRequest) =>
  axiosInstance.put<QuotaConfig>('/admin/v1/quota-config', data).then((r) => r.data)

// ── Quota config sets (registration-code-based) ───────────────────────────────

/** Returns all quota config sets. */
export const getAllQuotaConfigSets = () =>
  axiosInstance.get<QuotaConfigSet[]>('/admin/v1/quota-config-sets').then((r) => r.data)

/** Returns a single quota config set by ID. */
export const getQuotaConfigSet = (id: string) =>
  axiosInstance.get<QuotaConfigSet>(`/admin/v1/quota-config-sets/${id}`).then((r) => r.data)

/** Creates a new quota config set. */
export const createQuotaConfigSet = (data: QuotaConfigSetRequest) =>
  axiosInstance.post<QuotaConfigSet>('/admin/v1/quota-config-sets', data).then((r) => r.data)

/** Updates an existing quota config set. */
export const updateQuotaConfigSet = (id: string, data: QuotaConfigSetRequest) =>
  axiosInstance.put<QuotaConfigSet>(`/admin/v1/quota-config-sets/${id}`, data).then((r) => r.data)

/** Deletes a quota config set. */
export const deleteQuotaConfigSet = (id: string) =>
  axiosInstance.delete(`/admin/v1/quota-config-sets/${id}`).then((r) => r.data)

// ── Sync ─────────────────────────────────────────────────────────────────────

/** Syncs quota config set limits to all non-individually-overridden vehicles. */
export const syncQuotaConfigs = () =>
  axiosInstance
    .post<{ message: string; updatedCount: number }>('/admin/v1/quota-config/sync')
    .then((r) => r.data)
