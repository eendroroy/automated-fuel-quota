import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import {
  Plus, Car, Droplets, QrCode, History, AlertCircle,
  Fuel, RefreshCw, Download, CheckCircle2, Clock, ArrowRight,
  ChevronRight,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { getMyVehicles, getQrTokenForVehicle, regenerateQrTokenForVehicle } from '@/api/vehicleApi'
import { getVehicleQuota } from '@/api/quotaApi'
import { getMyTransactions } from '@/api/transactionApi'
import { useAuth } from '@/context/AuthContext'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Modal from '@/components/common/Modal'
import { formatDateTime, formatLitres } from '@/utils/formatters'
import { downloadQrAsPng } from '@/utils/qrHelpers'
import QRCode from 'react-qr-code'
import toast from 'react-hot-toast'
import type { Vehicle, Quota, Transaction } from '@/types'

const DASHBOARD_VEHICLE_LIMIT = 3

// ── Quota bar ─────────────────────────────────────────────────────────────────
function QuotaBar({ quota }: { quota: Quota }) {
  const { t } = useTranslation()
  const pct = Math.min((quota.usedLiters / quota.limitLiters) * 100, 100)
  const barColor =
    pct >= 90 ? 'bg-red-500' : pct >= 60 ? 'bg-amber-400' : 'bg-emerald-500'
  const periodLabel = quota.period.charAt(0) + quota.period.slice(1).toLowerCase()
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-xs text-gray-500">
        <span>{periodLabel} {t('dashboard.quota')}</span>
        <span className={pct >= 90 ? 'text-red-600 font-semibold' : ''}>{Math.round(pct)}% {t('dashboard.used')}</span>
      </div>
      <div className="h-2.5 rounded-full bg-gray-100 overflow-hidden">
        <div className={`h-full rounded-full transition-all duration-500 ${barColor}`} style={{ width: `${pct}%` }} />
      </div>
      <div className="flex items-center justify-between">
        <span className="text-sm font-bold text-gray-900">
          {formatLitres(quota.remainingLiters)}{' '}
          <span className="font-normal text-gray-400">{t('dashboard.remaining')}</span>
        </span>
        <span className="text-xs text-gray-400">of {quota.limitLiters}L</span>
      </div>
      <div className="flex items-center gap-1 text-xs text-gray-400">
        <Clock className="h-3 w-3" />
        {t('dashboard.resets')} {formatDateTime(quota.resetTimestamp)}
      </div>
    </div>
  )
}

// ── Inline QR Modal ───────────────────────────────────────────────────────────
function VehicleQrModal({ vehicle, onClose }: { vehicle: Vehicle; onClose: () => void }) {
  const { t } = useTranslation()
  const [token, setToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [regenerating, setRegenerating] = useState(false)

  useEffect(() => {
    setLoading(true)
    getQrTokenForVehicle(vehicle.id)
      .then((r) => setToken(r.token))
      .catch(() => toast.error(t('errors.qrLoadFailed')))
      .finally(() => setLoading(false))
  }, [vehicle.id])

  const handleRegenerate = async () => {
    setRegenerating(true)
    try {
      const r = await regenerateQrTokenForVehicle(vehicle.id)
      setToken(r.token)
      toast.success(t('qrCode.qrRegenerated'))
    } catch {
      toast.error(t('errors.qrRegenFailed'))
    } finally {
      setRegenerating(false)
    }
  }

  const handleDownload = () => {
    const svg = document.querySelector<SVGSVGElement>(`#qr-modal-${vehicle.id} svg`)
    if (!svg) return toast.error(t('errors.qrLoadFailed'))
    downloadQrAsPng(svg, `fuel-quota-${vehicle.registrationNumber}.png`)
  }

  return (
    <Modal isOpen onClose={onClose} title={t('dashboard.qrModalTitle')}>
      <div className="space-y-4">
        <div className="flex items-center gap-3 bg-gray-50 rounded-xl px-4 py-3">
          <div className="h-9 w-9 bg-brand-100 rounded-lg flex items-center justify-center flex-shrink-0">
            <Car className="h-4 w-4 text-brand-600" />
          </div>
          <div className="flex-1">
            <p className="font-bold text-gray-900 font-mono">{vehicle.registrationNumber}</p>
            <p className="text-xs text-gray-500">{vehicle.vehicleMake} · {vehicle.vehicleColor} · {vehicle.fuelType}</p>
          </div>
          <StatusBadge status={vehicle.status} />
        </div>
        <div className="flex flex-col items-center">
          {loading ? (
            <div className="h-56 flex items-center justify-center"><LoadingSpinner size="lg" /></div>
          ) : token ? (
            <div id={`qr-modal-${vehicle.id}`} className="p-5 bg-white border-2 border-gray-200 rounded-2xl shadow-inner">
              <QRCode value={token} size={200} />
            </div>
          ) : (
            <div className="h-56 flex items-center justify-center text-gray-400">{t('dashboard.qrUnavailable')}</div>
          )}
        </div>
        <p className="text-center text-xs text-gray-400">{t('dashboard.qrShowToRep')}</p>
        <div className="flex gap-3">
          <button onClick={handleDownload} disabled={!token || loading}
            className="btn-secondary flex-1 gap-2 text-sm py-2.5">
            <Download className="h-4 w-4" /> {t('common.download')}
          </button>
          <button onClick={handleRegenerate} disabled={regenerating || !token || loading}
            className="btn-primary flex-1 gap-2 text-sm py-2.5">
            {regenerating ? <LoadingSpinner size="sm" /> : <RefreshCw className="h-4 w-4" />}
            {t('dashboard.regenerate')}
          </button>
        </div>
      </div>
    </Modal>
  )
}

// ── Vehicle Spotlight Card (compact dashboard version) ────────────────────────
function VehicleSpotlightCard({ vehicle, onShowQr }: {
  vehicle: Vehicle
  onShowQr: (v: Vehicle) => void
}) {
  const { t } = useTranslation()
  const [quota, setQuota] = useState<Quota | null>(null)
  const [quotaLoading, setQuotaLoading] = useState(vehicle.status === 'VERIFIED')

  useEffect(() => {
    if (vehicle.status !== 'VERIFIED') return
    getVehicleQuota(vehicle.id)
      .then(setQuota)
      .catch(() => {})
      .finally(() => setQuotaLoading(false))
  }, [vehicle.id, vehicle.status])

  const quotaAlmost = quota && (quota.usedLiters / quota.limitLiters) >= 0.9
  const borderColor =
    vehicle.status === 'UNVERIFIED' ? 'border-red-200' :
    quotaAlmost ? 'border-amber-300' : 'border-gray-200'

  return (
    <div className={`card border ${borderColor} flex flex-col gap-4 transition-shadow hover:shadow-md`}>
      {/* Header */}
      <div className="flex items-start gap-3">
        <div className="h-11 w-11 bg-brand-50 rounded-xl flex items-center justify-center flex-shrink-0">
          <Car className="h-5 w-5 text-brand-600" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="font-bold text-gray-900 font-mono text-base leading-tight truncate">
            {vehicle.registrationNumber}
          </p>
          <p className="text-sm text-gray-500 truncate">{vehicle.vehicleMake} · {vehicle.vehicleColor}</p>
        </div>
        <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
          <StatusBadge status={vehicle.status} />
          <span className="text-xs text-gray-400 bg-gray-50 border border-gray-100 rounded-full px-2 py-0.5">
            {vehicle.fuelType}
          </span>
        </div>
      </div>

      {/* Quota */}
      {vehicle.status === 'VERIFIED' && (
        <div className="border-t border-gray-50 pt-3">
          {quotaLoading
            ? <div className="flex items-center gap-2 text-xs text-gray-400"><LoadingSpinner size="sm" /> {t('dashboard.loadingQuota')}</div>
            : quota ? <QuotaBar quota={quota} /> : <p className="text-xs text-gray-400">{t('dashboard.quotaUnavailable')}</p>}
        </div>
      )}

      {vehicle.status === 'UNVERIFIED' && (
        <div className="border-t border-gray-50 pt-3">
          <div className="flex items-start gap-2 text-xs text-red-700 bg-red-50 rounded-lg px-3 py-2">
            <AlertCircle className="h-3.5 w-3.5 mt-0.5 flex-shrink-0" />
            {t('dashboard.brta_pending')}
          </div>
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-2 border-t border-gray-50 pt-3 mt-auto">
        {vehicle.status === 'VERIFIED' && (
          <button onClick={() => onShowQr(vehicle)} className="btn-primary flex-1 text-sm py-2 gap-1.5">
            <QrCode className="h-4 w-4" /> {t('dashboard.getQr')}
          </button>
        )}
        <Link
          to={`/transactions?vehicleId=${vehicle.id}`}
          className={`btn-secondary text-sm py-2 gap-1.5 ${vehicle.status === 'VERIFIED' ? 'px-3' : 'flex-1'}`}
        >
          <History className="h-4 w-4" />
          {vehicle.status !== 'VERIFIED' && <span>{t('nav.transactions')}</span>}
        </Link>
      </div>
    </div>
  )
}

// ── Dashboard ──────────────────────────────────────────────────────────────────
export default function CustomerDashboardPage() {
  const { user } = useAuth()
  const { t } = useTranslation()
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [qrVehicle, setQrVehicle] = useState<Vehicle | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    Promise.all([
      getMyVehicles({ page: 0, size: 100 }).then((r) => r.content ?? []).catch(() => [] as Vehicle[]),
      getMyTransactions({ page: 0, size: 6 }).catch(() => ({ content: [] as Transaction[] })),
    ])
      .then(([v, t]) => {
        setVehicles(v)
        setTransactions(t.content)
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  if (loading)
    return <div className="flex items-center justify-center h-64"><LoadingSpinner size="lg" /></div>

  const activeVehicles = vehicles.filter((v) => v.status !== 'DEREGISTERED')
  const verifiedVehicles = vehicles.filter((v) => v.status === 'VERIFIED')

  const prioritisedVehicles = [
    ...vehicles.filter((v) => v.status === 'VERIFIED'),
    ...vehicles.filter((v) => v.status === 'UNVERIFIED'),
  ].slice(0, DASHBOARD_VEHICLE_LIMIT)

  const hiddenCount = activeVehicles.length - prioritisedVehicles.length

  return (
    <div className="space-y-8">
      {/* Welcome */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {t('dashboard.welcomeBack', { name: user?.name?.split(' ')[0] })}
          </h1>
          <p className="text-gray-500 text-sm mt-0.5">
            {t('dashboard.manageVehicles')}
          </p>
        </div>
        <Link to="/vehicles" className="btn-primary gap-2 text-sm flex-shrink-0">
          <Plus className="h-4 w-4" /> {t('dashboard.addVehicle')}
        </Link>
      </div>

      {/* Summary pills */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        <div className="bg-white border border-gray-200 rounded-xl px-4 py-3 flex items-center gap-3">
          <div className="h-9 w-9 bg-brand-50 rounded-lg flex items-center justify-center flex-shrink-0">
            <Car className="h-5 w-5 text-brand-600" />
          </div>
          <div>
            <p className="text-xl font-bold text-gray-900">{vehicles.length}</p>
            <p className="text-xs text-gray-500">{t('dashboard.totalVehicles')}</p>
          </div>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl px-4 py-3 flex items-center gap-3">
          <div className="h-9 w-9 bg-emerald-50 rounded-lg flex items-center justify-center flex-shrink-0">
            <CheckCircle2 className="h-5 w-5 text-emerald-600" />
          </div>
          <div>
            <p className="text-xl font-bold text-gray-900">{verifiedVehicles.length}</p>
            <p className="text-xs text-gray-500">{t('dashboard.activeQuotas')}</p>
          </div>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl px-4 py-3 flex items-center gap-3 col-span-2 sm:col-span-1">
          <div className="h-9 w-9 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0">
            <Fuel className="h-5 w-5 text-blue-600" />
          </div>
          <div>
            <p className="text-xl font-bold text-gray-900">{transactions.length}</p>
            <p className="text-xs text-gray-500">{t('dashboard.recentTransactions')}</p>
          </div>
        </div>
      </div>

      {/* Fleet Spotlight */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Droplets className="h-5 w-5 text-brand-600" />
            <h2 className="font-semibold text-gray-900 text-lg">
              {t('dashboard.fleetSpotlight')}
              {vehicles.length > 0 && (
                <span className="ml-2 text-sm font-normal text-gray-400">
                  ({activeVehicles.length} {t('dashboard.active')})
                </span>
              )}
            </h2>
          </div>
          <Link to="/vehicles" className="text-sm text-brand-600 hover:underline flex items-center gap-1">
            {t('dashboard.manageAll')} <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        {vehicles.length === 0 ? (
          <div className="card text-center py-16 border border-dashed border-gray-300 bg-gray-50">
            <Car className="h-12 w-12 mx-auto mb-3 text-gray-300" />
            <p className="font-semibold text-gray-600 text-lg">{t('dashboard.noVehiclesYet')}</p>
            <p className="text-sm text-gray-400 mt-1 mb-5">{t('dashboard.noVehiclesDesc')}</p>
            <Link to="/vehicles" className="btn-primary gap-2 inline-flex">
              <Plus className="h-4 w-4" /> {t('dashboard.registerVehicle')}
            </Link>
          </div>
        ) : (
          <>
            <div className="grid sm:grid-cols-2 xl:grid-cols-3 gap-4">
              {prioritisedVehicles.map((v) => (
                <VehicleSpotlightCard key={v.id} vehicle={v} onShowQr={setQrVehicle} />
              ))}
            </div>

            {(hiddenCount > 0 || vehicles.filter((v) => v.status === 'DEREGISTERED').length > 0) && (
              <Link
                to="/vehicles"
                className="mt-4 flex items-center justify-between w-full bg-gray-50 hover:bg-brand-50 border border-gray-200 hover:border-brand-200 rounded-xl px-5 py-3.5 transition-colors group"
              >
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 bg-white border border-gray-200 rounded-lg flex items-center justify-center group-hover:border-brand-200">
                    <Car className="h-4 w-4 text-gray-400 group-hover:text-brand-500" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-gray-700 group-hover:text-brand-700">
                      {hiddenCount > 0
                        ? t('dashboard.moreVehiclesNotShown', { count: hiddenCount })
                        : t('dashboard.viewFullFleet')}
                    </p>
                    <p className="text-xs text-gray-400">
                      {t('dashboard.totalWithQuotas', { total: vehicles.length, active: verifiedVehicles.length })}
                    </p>
                  </div>
                </div>
                <ChevronRight className="h-5 w-5 text-gray-400 group-hover:text-brand-500" />
              </Link>
            )}
          </>
        )}
      </section>

      {/* Recent Activity */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <History className="h-5 w-5 text-brand-600" />
            <h2 className="font-semibold text-gray-900 text-lg">{t('dashboard.recentActivity')}</h2>
          </div>
          <Link to="/transactions" className="text-sm text-brand-600 hover:underline flex items-center gap-1">
            {t('dashboard.allTransactions')} <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        <div className="card p-0 overflow-hidden">
          {transactions.length === 0 ? (
            <div className="text-center py-12 text-gray-400">
              <History className="h-10 w-10 mx-auto mb-2 opacity-30" />
              <p className="text-sm">{t('dashboard.noTransactionsYet')}</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-50">
              {transactions.map((tx) => (
                <div key={tx.id} className="flex items-center gap-4 px-5 py-3.5 hover:bg-gray-50 transition-colors">
                  <div className="h-9 w-9 bg-brand-50 rounded-xl flex items-center justify-center flex-shrink-0">
                    <Fuel className="h-4 w-4 text-brand-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-900 text-sm truncate">{tx.stationName}</p>
                    <div className="flex items-center gap-2 mt-0.5 flex-wrap">
                      <span className="text-xs text-gray-400">{formatDateTime(tx.transactionTimestamp)}</span>
                      {tx.registrationNumber && (
                        <span className="text-xs font-mono bg-gray-100 text-gray-600 rounded px-1.5 py-0.5">
                          {tx.registrationNumber}
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="font-bold text-gray-900 text-sm">−{formatLitres(tx.amountDispensedLiters)}</p>
                    <p className="text-xs text-gray-400 mt-0.5">{formatLitres(tx.remainingQuotaAfter)} {t('dashboard.left')}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* QR Modal */}
      {qrVehicle && <VehicleQrModal vehicle={qrVehicle} onClose={() => setQrVehicle(null)} />}
    </div>
  )
}
