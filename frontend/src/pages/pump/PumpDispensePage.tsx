import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  CheckCircle2, XCircle, ShieldCheck, ShieldOff,
  Droplets, ChevronDown, Delete, Loader2, ArrowLeft, RotateCcw
} from 'lucide-react'
import toast from 'react-hot-toast'
import { confirmDispensing } from '@/api/pumpApi'
import type { AuthorizationResult, PumpRepSession, PumpConfirmResponse } from '@/types'

const FUEL_TYPES = ['Petrol', 'Diesel', 'Octane', 'CNG', 'LPG']

interface LocationState {
  auth: AuthorizationResult
  qrToken?: string               // present on QR scan path
  registrationNumber?: string    // present on manual entry path
  session: PumpRepSession
}

// ── Numeric keyboard ──────────────────────────────────────────────────────────
const KEYS = [
  ['7', '8', '9'],
  ['4', '5', '6'],
  ['1', '2', '3'],
  ['.', '0', 'DEL'],
]

function NumericKeyboard({ onKey }: { onKey: (k: string) => void }) {
  return (
    <div className="grid grid-cols-3 gap-2 mt-3">
      {KEYS.flat().map((key) => (
        <button
          key={key}
          type="button"
          onClick={() => onKey(key)}
          className={`
            h-14 rounded-xl font-semibold text-lg transition-colors select-none
            ${key === 'DEL'
              ? 'bg-red-50 text-red-500 hover:bg-red-100 active:bg-red-200'
              : 'bg-gray-100 text-gray-800 hover:bg-gray-200 active:bg-gray-300'}
          `}
        >
          {key === 'DEL' ? <Delete className="h-5 w-5 mx-auto" /> : key}
        </button>
      ))}
    </div>
  )
}

// ── Decision badge ─────────────────────────────────────────────────────────────
function DecisionBadge({ decision }: { decision: string }) {
  if (decision === 'APPROVED') {
    return (
      <span className="inline-flex items-center gap-1 bg-green-100 text-green-700 text-xs font-semibold px-2.5 py-1 rounded-full">
        <CheckCircle2 className="h-3.5 w-3.5" /> Approved
      </span>
    )
  }
  if (decision === 'PARTIAL') {
    return (
      <span className="inline-flex items-center gap-1 bg-yellow-100 text-yellow-700 text-xs font-semibold px-2.5 py-1 rounded-full">
        <CheckCircle2 className="h-3.5 w-3.5" /> Partial
      </span>
    )
  }
  return (
    <span className="inline-flex items-center gap-1 bg-red-100 text-red-700 text-xs font-semibold px-2.5 py-1 rounded-full">
      <XCircle className="h-3.5 w-3.5" /> Denied
    </span>
  )
}

// ── Main component ─────────────────────────────────────────────────────────────
export default function PumpDispensePage() {
  const location = useLocation()
  const navigate = useNavigate()
  const state = location.state as LocationState | null

  const [amount, setAmount] = useState('')
  const [fuelType, setFuelType] = useState(state?.auth?.fuelType ?? 'Petrol')
  const [submitting, setSubmitting] = useState(false)
  const [receipt, setReceipt] = useState<PumpConfirmResponse | null>(null)

  // Guard: redirect if landed here without auth data
  // Manual path has registrationNumber but no qrToken — accept either
  if (!state?.auth || (!state?.qrToken && !state?.registrationNumber) || !state?.session) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4 text-center">
        <XCircle className="h-12 w-12 text-red-400" />
        <p className="text-gray-600 font-medium">No authorization data found.</p>
        <button className="btn-primary" onClick={() => navigate('/pump/scan')}>
          Back to Scanner
        </button>
      </div>
    )
  }

  const { auth, qrToken, registrationNumber, session } = state
  const denied = auth.decision === 'DENIED'

  // Numeric keyboard handler
  const handleKey = (key: string) => {
    if (key === 'DEL') {
      setAmount((prev) => prev.slice(0, -1))
      return
    }
    if (key === '.' && amount.includes('.')) return
    if (key === '.' && amount === '') { setAmount('0.'); return }
    const next = amount + key
    // Max 4 digits before decimal, 2 after
    const [intPart, decPart] = next.split('.')
    if (intPart.length > 4) return
    if (decPart !== undefined && decPart.length > 2) return
    setAmount(next)
  }

  const parsedAmount = parseFloat(amount)
  const maxAuthorized = auth.authorizedLiters ?? 0
  const canSubmit =
    !denied &&
    amount !== '' &&
    !isNaN(parsedAmount) &&
    parsedAmount > 0 &&
    parsedAmount <= maxAuthorized

  const handleSubmit = async () => {
    if (!canSubmit) return
    setSubmitting(true)
    try {
      const resp = await confirmDispensing({
        qrToken: qrToken,
        registrationNumber: registrationNumber,
        stationId: session.stationId,
        pumpRepresentativeId: session.id,
        dispensedLiters: parsedAmount,
        fuelType,
      })
      setReceipt(resp)
      toast.success('Transaction recorded!')
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ??
        err?.response?.data ??
        'Failed to confirm. Try again.'
      toast.error(typeof msg === 'string' ? msg : 'Confirmation failed')
    } finally {
      setSubmitting(false)
    }
  }

  // ── Receipt screen ─────────────────────────────────────────────────────────
  if (receipt) {
    return (
      <div className="flex flex-col items-center gap-6 py-8 text-center">
        <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center">
          <CheckCircle2 className="h-10 w-10 text-green-600" />
        </div>
        <div>
          <h2 className="text-xl font-bold text-gray-900">Transaction Complete</h2>
          <p className="text-sm text-gray-500 mt-1">Fuel dispensed successfully</p>
        </div>

        <div className="card w-full space-y-3 text-left">
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Reference</span>
            <span className="font-mono font-semibold text-gray-800">{receipt.transactionReference}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Dispensed</span>
            <span className="font-semibold text-gray-800">{receipt.dispensedLiters} L — {fuelType}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Vehicle</span>
            <span className="font-semibold text-gray-800">{auth.vehicleFound}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Remaining Quota</span>
            <span className="font-semibold text-gray-800">{receipt.remainingQuota} L</span>
          </div>
        </div>

        <div className="flex gap-3 w-full">
          <button
            className="btn-secondary flex-1 gap-2"
            onClick={() => navigate('/pump/scan')}
          >
            <RotateCcw className="h-4 w-4" /> Scan Next
          </button>
        </div>
      </div>
    )
  }

  // ── Quota progress bar ─────────────────────────────────────────────────────
  const total = auth.totalQuota ?? 0
  const remaining = auth.remainingQuota ?? 0
  const usedPct = total > 0 ? Math.min(100, ((total - remaining) / total) * 100) : 0
  const remainingPct = total > 0 ? Math.min(100, (remaining / total) * 100) : 0

  return (
    <div className="space-y-4">
      {/* Back button */}
      <button
        onClick={() => navigate('/pump/scan')}
        className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 -mt-1"
      >
        <ArrowLeft className="h-4 w-4" /> Back to Scanner
      </button>

      {/* ── UPPER SECTION: Vehicle Verification ─────────────────────────────── */}
      <section className="card space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-gray-900">Vehicle Verification</h2>
          <DecisionBadge decision={auth.decision} />
        </div>

        {/* Registration + status row */}
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-gray-50 rounded-lg px-3 py-2.5">
            <p className="text-xs text-gray-400 mb-0.5">Registration No.</p>
            <p className="font-bold text-gray-900 text-base tracking-wide">
              {auth.vehicleFound || '—'}
            </p>
          </div>
          <div className="bg-gray-50 rounded-lg px-3 py-2.5">
            <p className="text-xs text-gray-400 mb-0.5">BRTA Status</p>
            <div className="flex items-center gap-1.5 mt-0.5">
              {auth.vehicleStatus === 'VERIFIED' ? (
                <>
                  <ShieldCheck className="h-4 w-4 text-green-600" />
                  <span className="text-sm font-semibold text-green-700">Verified</span>
                </>
              ) : (
                <>
                  <ShieldOff className="h-4 w-4 text-red-500" />
                  <span className="text-sm font-semibold text-red-600">
                    {auth.vehicleStatus ?? 'Unknown'}
                  </span>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Owner + vehicle info */}
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <p className="text-xs text-gray-400">Owner</p>
            <p className="font-medium text-gray-700 truncate">{auth.ownerName || '—'}</p>
          </div>
          <div>
            <p className="text-xs text-gray-400">Vehicle</p>
            <p className="font-medium text-gray-700 truncate">
              {auth.vehicleMake} · {auth.vehicleColor}
            </p>
          </div>
        </div>

        {/* Quota bar */}
        <div>
          <div className="flex justify-between text-xs text-gray-500 mb-1.5">
            <span className="font-medium">Remaining Quota</span>
            <span>
              <strong className="text-gray-800">{remaining}</strong>
              {' '}/{' '}{total} L
            </span>
          </div>
          <div className="h-3 rounded-full bg-gray-100 overflow-hidden">
            <div
              className={`h-full rounded-full transition-all ${
                remainingPct > 50
                  ? 'bg-green-500'
                  : remainingPct > 20
                  ? 'bg-yellow-400'
                  : 'bg-red-500'
              }`}
              style={{ width: `${remainingPct}%` }}
            />
          </div>
          {auth.message && (
            <p className="text-xs text-red-500 mt-1.5 flex items-center gap-1">
              <XCircle className="h-3.5 w-3.5 shrink-0" /> {auth.message}
            </p>
          )}
        </div>
      </section>

      {/* ── LOWER SECTION: Dispense Form ────────────────────────────────────── */}
      <section className={`card space-y-4 ${denied ? 'opacity-60 pointer-events-none select-none' : ''}`}>
        <div className="flex items-center gap-2 mb-1">
          <Droplets className="h-5 w-5 text-brand-600" />
          <h2 className="text-base font-bold text-gray-900">Record Fuel Dispensed</h2>
        </div>

        {/* Fuel type + amount row */}
        <div className="flex gap-3 items-end">
          {/* Fuel type dropdown */}
          <div className="flex-1">
            <label className="label text-xs">Fuel Type</label>
            <div className="relative">
              <select
                className="input-field appearance-none pr-8 text-sm"
                value={fuelType}
                onChange={(e) => setFuelType(e.target.value)}
              >
                {FUEL_TYPES.map((f) => (
                  <option key={f} value={f}>{f}</option>
                ))}
              </select>
              <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>

          {/* Amount display */}
          <div className="flex-1">
            <label className="label text-xs">Amount (Litres)</label>
            <div
              className={`
                input-field text-right text-2xl font-bold font-mono pr-3 cursor-default
                ${!amount ? 'text-gray-300' : parsedAmount > maxAuthorized ? 'text-red-500' : 'text-gray-900'}
              `}
            >
              {amount || '0'}
              <span className="text-sm font-normal text-gray-400 ml-1">L</span>
            </div>
          </div>
        </div>

        {/* Max authorized hint */}
        {!denied && (
          <p className="text-xs text-gray-400 -mt-1">
            Max authorized: <strong className="text-brand-600">{maxAuthorized} L</strong>
          </p>
        )}

        {/* Numeric keyboard */}
        <NumericKeyboard onKey={handleKey} />

        {/* Amount validation warning */}
        {amount && parsedAmount > maxAuthorized && (
          <p className="text-xs text-red-500 flex items-center gap-1">
            <XCircle className="h-3.5 w-3.5 shrink-0" />
            Amount exceeds authorized limit of {maxAuthorized} L
          </p>
        )}

        {/* Submit */}
        <button
          className="btn-primary w-full py-3.5 text-base mt-2 gap-2"
          disabled={!canSubmit || submitting}
          onClick={handleSubmit}
        >
          {submitting ? (
            <Loader2 className="h-5 w-5 animate-spin" />
          ) : (
            <CheckCircle2 className="h-5 w-5" />
          )}
          {submitting ? 'Recording Transaction…' : 'Confirm & Submit'}
        </button>
      </section>
    </div>
  )
}

