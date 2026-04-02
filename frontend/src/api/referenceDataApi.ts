import axiosInstance from './axiosInstance'
import type { RegistrationCode, BrtaOffice } from '@/types'

/** Returns all vehicle category registration codes (public endpoint — no auth needed). */
export const getRegistrationCodes = () =>
  axiosInstance.get<RegistrationCode[]>('/public/registration-codes').then((r) => r.data)

/** Returns all BRTA regional office codes (public endpoint — no auth needed). */
export const getBrtaOffices = () =>
  axiosInstance.get<BrtaOffice[]>('/public/brta-offices').then((r) => r.data)

