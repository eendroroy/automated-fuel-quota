import axiosInstance from './axiosInstance'
import type { Transaction, PagedResponse } from '@/types'

export const getMyTransactions = (params: { page?: number; size?: number }) =>
  axiosInstance
    .get<PagedResponse<Transaction>>('/customer/v1/transactions', { params })
    .then((r) => r.data)

export const getVehicleTransactions = (vehicleId: string, params: { page?: number; size?: number }) =>
  axiosInstance
    .get<PagedResponse<Transaction>>(`/customer/v1/vehicles/${vehicleId}/transactions`, { params })
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
    .get<PagedResponse<Transaction>>('/admin/v1/transactions', { params })
    .then((r) => r.data)
