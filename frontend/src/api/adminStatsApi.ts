import axiosInstance from './axiosInstance'
import type { AdminStats } from '@/types'

export const getAdminStats = () =>
  axiosInstance.get<AdminStats>('/admin/v1/stats').then((r) => r.data)

