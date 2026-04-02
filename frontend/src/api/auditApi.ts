import axiosInstance from './axiosInstance'
import type { AuditLog, PagedResponse } from '@/types'

export const getAuditLogs = (params?: {
  page?: number
  size?: number
  actionType?: string
  startDate?: string
  endDate?: string
}) =>
  axiosInstance
    .get<PagedResponse<AuditLog>>('/admin/audit-logs', { params })
    .then((r) => r.data)

