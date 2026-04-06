import axiosInstance from './axiosInstance'
import type { AuditLog, PagedResponse } from '@/types'

export const getAuditLogs = (params?: {
  page?: number
  size?: number
  actionType?: string
  startDate?: string
  endDate?: string
  adminSearch?: string
  targetEntity?: string
}) =>
  axiosInstance
    .get<PagedResponse<AuditLog>>('/admin/v1/audit-logs', { params })
    .then((r) => r.data)

