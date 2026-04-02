import axiosInstance from './axiosInstance'
import type { Transaction, PagedResponse } from '@/types'

export const getMyTransactions = (params: { page?: number; size?: number }) =>
  axiosInstance
    .get<PagedResponse<Transaction>>('/customer/transactions', { params })
    .then((r) => r.data)

export const getAllTransactions = (params: {
  page?: number
  size?: number
  stationId?: string
  vehicleId?: string
  startDate?: string
  endDate?: string
}) =>
  axiosInstance
    .get<PagedResponse<Transaction>>('/admin/transactions', { params })
    .then((r) => r.data)

