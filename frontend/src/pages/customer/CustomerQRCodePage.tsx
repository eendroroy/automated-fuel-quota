import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Download, RefreshCw, Clock, ChevronDown, Droplets, CheckCircle2 } from 'lucide-react'
import QRCode from 'react-qr-code'
import toast from 'react-hot-toast'
import { useTranslation } from 'react-i18next'
import {
  getMyVehicles,
  getQrTokenForVehicle, regenerateQrTokenForVehicle,
} from '@/api/vehicleApi'
import { downloadQrAsPng } from '@/utils/qrHelpers'
import { formatDateTime } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import type { Vehicle } from '@/types'

export default function CustomerQRCodePage() {
  const { t } = useTranslation()
  const [searchParams, setSearchParams] = useSearchParams()
  const initialVehicleId = searchParams.get('vehicleId')

  const [activeVehicles, setActiveVehicles] = useState<Vehicle[]>([])
  const [selectedVehicleId, setSelectedVehicleId] = useState<string | null>(initialVehicleId)
  const [vehicle, setVehicle] = useState<Vehicle | null>(null)
  const [selectedFuelType, setSelectedFuelType] = useState<string>('')
  const [token, setToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [vehiclesLoading, setVehiclesLoading] = useState(true)
  const [regenerating, setRegenerating] = useState(false)
  const [lastUpdated, setLastUpdated] = useState<string | null>(null)

  // Load all active vehicles first
  useEffect(() => {
    setVehiclesLoading(true)
    getMyVehicles({ page: 0, size: 100 })
      .then((response) => {
        const active = (response.content ?? []).filter((v) => v.status === 'VERIFIED')
        setActiveVehicles(active)
        if (!initialVehicleId && active.length > 0) {
          setSelectedVehicleId(active[0].id)
        }
      })
      .catch(() => {})
      .finally(() => setVehiclesLoading(false))
  }, [initialVehicleId])

  // When vehicle changes, reset fuel type to primary
  useEffect(() => {
    if (!selectedVehicleId || activeVehicles.length === 0) return
    const v = activeVehicles.find((v) => v.id === selectedVehicleId) ?? activeVehicles[0]
    setVehicle(v)
    setSelectedFuelType(v.fuelType)
  }, [selectedVehicleId, activeVehicles])

  // Load QR token whenever selectedVehicleId or selectedFuelType changes.
  // Cleanup flag prevents stale responses from overwriting newer ones (race condition fix).
  useEffect(() => {
    if (!selectedVehicleId || !selectedFuelType) return
    let cancelled = false
    setLoading(true)
    setToken(null)
    getQrTokenForVehicle(selectedVehicleId, selectedFuelType)
      .then((qr) => {
        if (!cancelled) {
          setToken(qr.token)
          setLastUpdated(new Date().toISOString())
        }
      })
      .catch(() => { if (!cancelled) toast.error(t('errors.qrLoadFailed')) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [selectedVehicleId, selectedFuelType, t])

  const handleVehicleChange = (id: string) => {
    setSelectedVehicleId(id)
    setToken(null)
    setSearchParams(id ? { vehicleId: id } : {})
  }

  const handleRegenerate = async () => {
    if (!selectedVehicleId) return
    setRegenerating(true)
    try {
      const qr = await regenerateQrTokenForVehicle(
        selectedVehicleId,
        selectedFuelType
      )
      setToken(qr.token)
      setLastUpdated(new Date().toISOString())
      toast.success(t('qrCode.qrRegenerated'))
    } catch {
      toast.error(t('errors.qrRegenFailed'))
    } finally {
      setRegenerating(false)
    }
  }

  const handleDownload = () => {
    const svg = document.querySelector<SVGSVGElement>('#vehicle-qr-code svg')
    if (!svg) return toast.error(t('errors.qrLoadFailed'))
    downloadQrAsPng(svg, `fuel-quota-${vehicle?.registrationNumber ?? 'qr'}.png`)
    toast.success(t('qrCode.downloadSuccess'))
  }

  // Available fuel types for the selected vehicle
  const availableFuelTypes = vehicle
    ? [vehicle.fuelType, ...(vehicle.secondaryFuelTypes ?? [])]
    : []

  if (vehiclesLoading)
    return <div className="flex items-center justify-center h-64"><LoadingSpinner size="lg" /></div>

  if (activeVehicles.length === 0)
    return (
      <div className="max-w-md mx-auto text-center py-20 px-4">
        <div className="h-16 w-16 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center">
          <Droplets className="h-8 w-8 text-gray-400" />
        </div>
        <p className="font-semibold text-gray-700 dark:text-gray-300 text-lg">{t('qrCode.noActiveVehicles')}</p>
        <p className="text-sm text-gray-400 mt-2">{t('qrCode.noActiveVehiclesDesc')}</p>
      </div>
    )

  const isPrimaryFuel = selectedFuelType === vehicle?.fuelType
  const isSecondaryFuel = !isPrimaryFuel && availableFuelTypes.includes(selectedFuelType)

  return (
    <div className="max-w-sm mx-auto space-y-4 pb-6">
      {/* Header */}
      <div className="text-center pt-2">
        <h1 className="text-xl font-bold text-gray-900 dark:text-white">{t('qrCode.title')}</h1>
        <p className="text-gray-500 dark:text-gray-400 text-xs mt-1">{t('qrCode.subtitle')}</p>
      </div>

      {/* Vehicle selector */}
      {activeVehicles.length > 1 && (
        <div>
          <label className="label text-xs">{t('qrCode.selectVehicle')}</label>
          <div className="relative">
            <select
              className="input-field appearance-none pr-8 text-sm"
              value={selectedVehicleId ?? ''}
              onChange={(e) => handleVehicleChange(e.target.value)}
            >
              {activeVehicles.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.registrationNumber} — {v.vehicleMake}
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          </div>
        </div>
      )}

      {/* Vehicle info strip */}
      {vehicle && (
        <div className="card p-3 flex items-center gap-3">
          <div className="h-10 w-10 bg-brand-50 dark:bg-brand-900/30 rounded-xl flex items-center justify-center flex-shrink-0">
            <Droplets className="h-5 w-5 text-brand-600" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-bold text-gray-900 dark:text-white font-mono text-sm leading-tight">{vehicle.registrationNumber}</p>
            <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{vehicle.vehicleMake} · {vehicle.vehicleColor}</p>
          </div>
          <StatusBadge status={vehicle.status} />
        </div>
      )}

      {/* Fuel type selector */}
      {vehicle && availableFuelTypes.length > 0 && (
        <div>
          <label className="label text-xs">{t('qrCode.selectFuelType')}</label>
          <div className="flex flex-wrap gap-2">
            {availableFuelTypes.map((ft) => {
              const isPrimary = ft === vehicle.fuelType
              const isSelected = selectedFuelType === ft
              return (
                <button
                  key={ft}
                  type="button"
                  onClick={() => { if (selectedFuelType !== ft) setSelectedFuelType(ft) }}
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium border transition-all ${
                    isSelected
                      ? 'bg-brand-600 text-white border-brand-600'
                      : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:border-brand-400'
                  }`}
                >
                  {isSelected && <CheckCircle2 className="h-3.5 w-3.5" />}
                  {ft}
                  {isPrimary && (
                    <span className={`text-[10px] font-normal px-1 py-0.5 rounded ${
                      isSelected ? 'bg-brand-500 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-500'
                    }`}>
                      {t('qrCode.primaryFuel')}
                    </span>
                  )}
                </button>
              )
            })}
          </div>
          {isSecondaryFuel && (
            <p className="text-xs text-amber-600 dark:text-amber-400 mt-2 flex items-center gap-1">
              ⚠ {t('qrCode.fuelTypeHint')}
            </p>
          )}
        </div>
      )}

      {/* QR Code - full width, mobile-optimised */}
      <div className="card flex flex-col items-center py-6 gap-4">
        {loading ? (
          <div className="h-48 flex items-center justify-center">
            <LoadingSpinner size="lg" />
          </div>
        ) : token ? (
          <div
            id="vehicle-qr-code"
            className="p-3 bg-white border-2 border-gray-200 dark:border-gray-300 rounded-2xl shadow-md w-full max-w-[260px] mx-auto"
          >
            <QRCode value={token} size={234} level="H" style={{ width: '100%', height: 'auto' }} viewBox="0 0 256 256" />
          </div>
        ) : (
          <div className="h-48 flex items-center justify-center text-gray-400 text-sm">
            {t('qrCode.noVerifiedVehicles')}
          </div>
        )}

        {token && (
          <div className="text-center space-y-1">
            <p className="text-xs font-medium text-green-600 dark:text-green-400 flex items-center justify-center gap-1">
              <CheckCircle2 className="h-3.5 w-3.5" /> {t('qrCode.validFor')}
            </p>
            {lastUpdated && (
              <p className="flex items-center justify-center gap-1 text-[11px] text-gray-400 dark:text-gray-500">
                <Clock className="h-3 w-3" />
                {t('qrCode.lastUpdated')}: {formatDateTime(lastUpdated)}
              </p>
            )}
          </div>
        )}

        <p className="text-[11px] text-gray-400 dark:text-gray-500 text-center max-w-[260px]">
          {t('qrCode.showToPumpRep')}
        </p>

        <div className="flex gap-2 w-full">
          <button
            onClick={handleDownload}
            disabled={!token || loading}
            className="btn-secondary flex-1 gap-1.5 text-sm py-2.5"
          >
            <Download className="h-4 w-4" />
            {t('qrCode.download')}
          </button>
          <button
            onClick={handleRegenerate}
            disabled={regenerating || !token || loading}
            className="btn-primary flex-1 gap-1.5 text-sm py-2.5"
          >
            {regenerating ? <LoadingSpinner size="sm" /> : <RefreshCw className="h-4 w-4" />}
            {t('qrCode.regenerate')}
          </button>
        </div>

        <p className="text-[10px] text-gray-400 dark:text-gray-500 text-center px-2">
          {t('qrCode.qrInfo')}
        </p>
      </div>
    </div>
  )
}
