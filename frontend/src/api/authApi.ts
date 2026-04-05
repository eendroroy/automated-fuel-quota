import axiosInstance from './axiosInstance'
import type { AuthResponse, LoginRequest, RegisterVehicleRequest } from '@/types'

export const customerLogin = (data: LoginRequest) =>
  axiosInstance.post<AuthResponse>('/auth/v1/customer/login', data).then((r) => r.data)

export const adminLogin = (data: LoginRequest) =>
  axiosInstance.post<AuthResponse>('/auth/v1/admin/login', data).then((r) => r.data)

export const sendOtp = (mobileNumber: string) =>
  axiosInstance.post<{ message: string }>('/auth/v1/customer/send-otp', { mobileNumber }).then((r) => r.data)

export const registerCustomer = (data: RegisterVehicleRequest) =>
  axiosInstance.post<{ message: string }>('/auth/v1/customer/register', data).then((r) => r.data)

export const uploadDocuments = (vehicleId: string, formData: FormData) =>
  axiosInstance
    .post(`/vehicles/${vehicleId}/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data)
