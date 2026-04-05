import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  CheckCircle2, XCircle, ShieldCheck, ShieldOff,
  Droplets, Delete, Loader2, ArrowLeft, RotateCcw
} from 'lucide-react'
import toast from 'react-hot-toast'
import { confirmDispensing } from '@/api/pumpApi'
import type { AuthorizationResult, PumpRepSession, PumpConfirmResponse } from '@/types'

interface LocationState {
  auth: AuthorizationResult
  qrToken?: string
  registrationNumber?: string
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
    <div className="grid grid-cols-3 gap-2 mt-2">
      {KEYS.flat().map((key) => (
        <button
          key={key}
          type="button"
          onClick={() => onKey(key)}
          className={`
            h-14 rounded-xl font-semibold text-xl transition-colors select-none active:scale-95
            ${key === 'DEL'
              ? 'bg-red-50 dark:bg-red-900/30 text-red-500 hover:bg-red-100 dark:hover:bg-red-900/50'
              : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-600'}
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
  const { t } = useTranslation()
  if (decision === 'APPROVED') return (
    <span className="inline-flex items-center gap-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-xs font-semibold px-2.5 py-1 rounded-full">
      <CheckCircle2 className="h-3.5 w-3.5" /> {t('pumpDispense.APPROVED')}
    </span>
  )
  if (decision === 'PARTIAL') return (
    <span className="inline-flex items-center gap-1 bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300 text-xs font-semibold px-2.5 py-1 rounded-full">
      <CheckCircle2 className="h-3.5 w-3.5" /> {t('pumpDispense.PARTIAL')}
    </span>
  )
  return (
    <span className="inline-flex items-center gap-1 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-xs font-semibold px-2.5 py-1 rounded-full">
      <XCircle className="h-3.5 w-3.5" /> {t('pumpDispense.DENIED')}
    </span>
  )
}

// ── Main component ─────────────────────────────────────────────────────────────
export default function PumpDispensePage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const state = location.state as LocationState | null

  const [amount, setAmount] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [receipt, setReceipt] = useState<PumpConfirmResponse | null>(null)

  // Guard: redirect if landed here without auth data
  // Manual path has registrationNumber but no qrToken — accept either
  if (!state?.auth || (!state?.qrToken && !state?.registrationNumber) || !state?.session) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4 text-center">
        <XCircle className="h-12 w-12 text-red-400" />
        <p className="text-gray-600 dark:text-gray-400 font-medium">{t('pumpDispense.noAuthData')}</p>
        <button className="btn-primary" onClick={() => navigate('/pump/scan')}>
          {t('pumpDispense.backToScanner')}
        </button>
      </div>
    )
  }

  const { auth, qrToken, registrationNumber, session } = state
  const denied = auth.decision === 'DENIED'
  const fuelType = auth.fuelType

  // Numeric keyboard handler
  const handleKey = (key: string) => {
    if (key === 'DEL') { setAmount((prev) => prev.slice(0, -1)); return }
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
    !denied && amount !== '' && !isNaN(parsedAmount) && parsedAmount > 0 && parsedAmount <= maxAuthorized

  const handleSubmit = async () => {
    if (!canSubmit) return
    setSubmitting(true)
    try {
      const resp = await confirmDispensing({
        qrToken,
        registrationNumber,
        stationId: session.stationId,
        pumpRepresentativeId: session.id,
        dispensedLiters: parsedAmount,
        fuelType,
      })
      setReceipt(resp)
      toast.success('Transaction recorded!')
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? err?.response?.data ?? 'Failed to confirm. Try again.'
      toast.error(typeof msg === 'string' ? msg : 'Confirmation failed')
    } finally {
      setSubmitting(false)
    }
  }

  // ── Receipt ────────────────────────────────────────────────────────────────
  if (receipt) {
    return (
      <div className="flex flex-col items-center gap-5 py-8 text-center">
        <div className="w-20 h-20 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
          <CheckCircle2 className="h-10 w-10 text-green-600 dark:text-green-400" />
        </div>
        <div>
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">{t('pumpDispense.transactionComplete')}</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{t('pumpDispense.fuelDispensedSuccess')}</p>
        </div>
        <div className="card w-full space-y-3 text-left">
          <div className="flex justify-between text-sm">
            <span className="text-gray-500 dark:text-gray-400">{t('pumpDispense.reference')}</span>
            <span className="font-mono font-semibold text-gray-800 dark:text-gray-200 text-xs">{receipt.transactionReference}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500 dark:text-gray-400">{t('pumpDispense.dispensed')}</span>
            <span className="font-semibold text-gray-800 dark:text-gray-200">{receipt.dispensedLiters} L — {fuelType}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500 dark:text-gray-400">{t('pumpDispense.vehicle')}</span>
            <span className="font-semibold text-gray-800 dark:text-gray-200">{auth.vehicleFound}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500 dark:text-gray-400">{t('pumpDispense.remainingQuota')}</span>
            <span className="font-semibold text-gray-800 dark:text-gray-200">{receipt.remainingQuota} L</span>
          </div>
        </div>
        <button className="btn-secondary w-full gap-2" onClick={() => navigate('/pump/scan')}>
          <RotateCcw className="h-4 w-4" /> {t('pumpDispense.scanNext')}
        </button>
      </div>
    )
  }

  // ── Quota progress ─────────────────────────────────────────────────────────
  const total = auth.totalQuota ?? 0
  const remaining = auth.remainingQuota ?? 0
  const remainingPct = total > 0 ? Math.min(100, (remaining / total) * 100) : 0

  return (
    <div className="space-y-4">
      {/* Back */}
      <button
        onClick={() => navigate('/pump/scan')}
        className="flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 -mt-1"
      >
        <ArrowLeft className="h-4 w-4" /> {t('pumpDispense.backToScanner')}
      </button>

      {/* ── UPPER SECTION: Vehicle Verification ─────────────────────────────── */}
      <section className="card space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-gray-900 dark:text-white">{t('pumpDispense.vehicleInfo')}</h2>
          <div className="flex items-center gap-2">
            <DecisionBadge decision={auth.decision} />
          </div>
        </div>

        {/* Fuel type — highlighted in upper section */}
        <div className="flex items-center gap-3 bg-brand-50 dark:bg-brand-900/20 border border-brand-200 dark:border-brand-800 rounded-xl px-4 py-3">
          <Droplets className="h-5 w-5 text-brand-600 dark:text-brand-400 flex-shrink-0" />
          <div>
            <p className="text-xs text-brand-500 dark:text-brand-400 font-medium">{t('pumpDispense.fuelType')}</p>
            <p className="font-bold text-lg text-brand-700 dark:text-brand-300 leading-tight">{fuelType}</p>
          </div>
        </div>

        {/* Registration + status */}
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-gray-50 dark:bg-gray-800 rounded-lg px-3 py-2.5">
            <p className="text-xs text-gray-400 dark:text-gray-500 mb-0.5">{t('pumpDispense.registrationNumber')}</p>
            <p className="font-bold text-gray-900 dark:text-white text-base tracking-wide">
              {auth.vehicleFound || '—'}
            </p>
          </div>
          <div className="bg-gray-50 dark:bg-gray-800 rounded-lg px-3 py-2.5">
            <p className="text-xs text-gray-400 dark:text-gray-500 mb-0.5">{t('pumpDispense.vehicleStatus')}</p>
            <div className="flex items-center gap-1.5 mt-0.5">
              {auth.vehicleStatus === 'VERIFIED' ? (
                <><ShieldCheck className="h-4 w-4 text-green-600" /><span className="text-sm font-semibold text-green-700 dark:text-green-400">Verified</span></>
              ) : (
                <><ShieldOff className="h-4 w-4 text-red-500" /><span className="text-sm font-semibold text-red-600 dark:text-red-400">{auth.vehicleStatus ?? 'Unknown'}</span></>
              )}
            </div>
          </div>
        </div>

        {/* Owner + vehicle */}
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <p className="text-xs text-gray-400 dark:text-gray-500">{t('pumpDispense.owner')}</p>
            <p className="font-medium text-gray-700 dark:text-gray-300 truncate">{auth.ownerName || '—'}</p>
          </div>
          <div>
            <p className="text-xs text-gray-400 dark:text-gray-500">{t('pumpDispense.vehicleMake')}</p>
            <p className="font-medium text-gray-700 dark:text-gray-300 truncate">{auth.vehicleMake} · {auth.vehicleColor}</p>
          </div>
        </div>

        {/* Quota bar */}
        <div>
          <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mb-1.5">
            <span className="font-medium">{t('pumpDispense.remaining')}</span>
            <span><strong className="text-gray-800 dark:text-gray-200">{remaining}</strong> / {total} L</span>
          </div>
          <div className="h-3 rounded-full bg-gray-100 dark:bg-gray-700 overflow-hidden">
            <div
              className={`h-full rounded-full transition-all ${
                remainingPct > 50 ? 'bg-green-500' : remainingPct > 20 ? 'bg-yellow-400' : 'bg-red-500'
              }`}
              style={{ width: `${remainingPct}%` }}
            />
          </div>
          {auth.message && (
            <p className="text-xs text-red-500 dark:text-red-400 mt-1.5 flex items-center gap-1">
              <XCircle className="h-3.5 w-3.5 shrink-0" /> {auth.message}
            </p>
          )}
        </div>
      </section>

      {/* ── LOWER SECTION: Dispense Form ────────────────────────────────────── */}
      <section className={`card space-y-3 ${denied ? 'opacity-60 pointer-events-none select-none' : ''}`}>
        <div className="flex items-center gap-2">
          <Droplets className="h-5 w-5 text-brand-600" />
          <h2 className="text-base font-bold text-gray-900 dark:text-white">{t('pumpDispense.recordDispense')}</h2>
        </div>

        {/* Amount — full width */}
        <div>
          <label className="label text-xs">{t('pumpDispense.amountToDispense')} *</label>
          <div className={`
            input-field text-center text-4xl font-bold font-mono cursor-default py-4
            ${!amount ? 'text-gray-300 dark:text-gray-600' : parsedAmount > maxAuthorized ? 'text-red-500' : 'text-gray-900 dark:text-gray-100'}
          `}>
            {amount || '0'}
            <span className="text-lg font-normal text-gray-400 dark:text-gray-500 ml-1.5">L</span>
          </div>
        </div>

        {/* Max authorized hint */}
        {!denied && (
          <p className="text-xs text-gray-400 -mt-1">
            {t('pumpDispense.maxHint')}: <strong className="text-brand-600 dark:text-brand-400">{maxAuthorized} L</strong>
          </p>
        )}

        {/* Numeric keyboard */}
        <NumericKeyboard onKey={handleKey} />

        {/* Validation */}
        {amount && parsedAmount > maxAuthorized && (
          <p className="text-xs text-red-500 flex items-center gap-1">
            <XCircle className="h-3.5 w-3.5 shrink-0" />
            {t('pumpDispense.exceedsLimit', { max: maxAuthorized })}
          </p>
        )}

        {/* Submit */}
        <button
          className="btn-primary w-full py-4 text-base mt-1 gap-2"
          disabled={!canSubmit || submitting}
          onClick={handleSubmit}
        >
          {submitting ? <Loader2 className="h-5 w-5 animate-spin" /> : <CheckCircle2 className="h-5 w-5" />}
          {submitting ? t('pumpDispense.confirming') : t('pumpDispense.confirmDispense')}
        </button>
      </section>
    </div>
  )
}
