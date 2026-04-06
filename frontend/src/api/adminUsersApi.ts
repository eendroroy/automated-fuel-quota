import axiosInstance from './axiosInstance'
import type { AppUser, PagedResponse, UserStatusUpdateRequest } from '@/types'

export interface CreateAdminUserRequest {
  name: string
  email: string
  password: string
}

export const getAdminUsers = (params: {
  page?: number
  size?: number
  role?: string
  status?: string
  search?: string
  createdAtFrom?: string
  createdAtTo?: string
}) =>
  axiosInstance
    .get<PagedResponse<AppUser>>('/admin/v1/users', { params })
    .then((r) => r.data)

export const createAdminUser = (data: CreateAdminUserRequest) =>
  axiosInstance
    .post<AppUser>('/admin/v1/users', data)
    .then((r) => r.data)

export const updateUserStatus = (userId: string, data: UserStatusUpdateRequest) =>
  axiosInstance
    .put<AppUser>(`/admin/v1/users/${userId}/status`, data)
    .then((r) => r.data)

export const getUserById = (userId: string) =>
  axiosInstance
    .get<AppUser>(`/admin/v1/users/${userId}`)
    .then((r) => r.data)

