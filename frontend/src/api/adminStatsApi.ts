import axiosInstance from './axiosInstance'
import type { AdminStats } from '@/types'

export const getAdminStats = () =>
  axiosInstance.get<AdminStats>('/admin/stats').then((r) => r.data)

