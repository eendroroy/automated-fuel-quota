import axiosInstance from './axiosInstance'
import type { FuelStation, StationFormData, PagedResponse } from '@/types'

export const getAllStations = (params?: {
  page?: number;
  size?: number;
  status?: string;
  search?: string;
  district?: string;
}) =>
  axiosInstance.get<PagedResponse<FuelStation>>('/admin/v1/stations', { params }).then((r) => r.data)

export const getStation = (id: string) =>
  axiosInstance.get<FuelStation>(`/admin/v1/stations/${id}`).then((r) => r.data)

export const createStation = (data: StationFormData) =>
  axiosInstance.post<FuelStation>('/admin/v1/stations', data).then((r) => r.data)

export const updateStation = (id: string, data: Partial<StationFormData>) =>
  axiosInstance.put<FuelStation>(`/admin/v1/stations/${id}`, data).then((r) => r.data)

export const deleteStation = (id: string) =>
  axiosInstance.delete(`/admin/v1/stations/${id}`).then((r) => r.data)
