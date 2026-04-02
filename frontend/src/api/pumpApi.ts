import axios from 'axios'
import type {
  PumpRepLoginRequest,
  PumpRepSession,
  AuthorizationResult,
  PumpAuthorizeRequest,
  ManualAuthorizeRequest,
  PumpConfirmRequest,
  PumpConfirmResponse,
} from '@/types'

// Pump API uses a bare axios instance (no JWT, public endpoints)
const pumpAxios = axios.create({
  baseURL: '/api/pump',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

export const pumpRepLogin = (data: PumpRepLoginRequest): Promise<PumpRepSession> =>
  pumpAxios.post<PumpRepSession>('/login', data).then((r) => r.data)

export const authorizeDispensing = (data: PumpAuthorizeRequest): Promise<AuthorizationResult> =>
  pumpAxios.post<AuthorizationResult>('/authorize', data).then((r) => r.data)

export const authorizeByRegistration = (data: ManualAuthorizeRequest): Promise<AuthorizationResult> =>
  pumpAxios.post<AuthorizationResult>('/authorize-manual', data).then((r) => r.data)

export const confirmDispensing = (data: PumpConfirmRequest): Promise<PumpConfirmResponse> =>
  pumpAxios.post<PumpConfirmResponse>('/confirm', data).then((r) => r.data)

