import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Download, RefreshCw, Clock, ChevronDown } from 'lucide-react'
import QRCode from 'react-qr-code'
import toast from 'react-hot-toast'
import {
  getQrToken, regenerateQrToken,
  getMyVehicles,
  getQrTokenForVehicle, regenerateQrTokenForVehicle,
} from '@/api/vehicleApi'
import { downloadQrAsPng } from '@/utils/qrHelpers'
import { formatDateTime } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import type { Vehicle } from '@/types'

export default function CustomerQRCodePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialVehicleId = searchParams.get('vehicleId')

  const [activeVehicles, setActiveVehicles] = useState<Vehicle[]>([])
  const [selectedVehicleId, setSelectedVehicleId] = useState<string | null>(initialVehicleId)
  const [vehicle, setVehicle] = useState<Vehicle | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [regenerating, setRegenerating] = useState(false)
  const [lastUpdated, setLastUpdated] = useState<string | null>(null)

  // Load all active vehicles first
  useEffect(() => {
    getMyVehicles({ page: 0, size: 100 }) // Fetch a large page for dropdown
      .then((response) => {
        const active = response.content.filter((v) => v.status === 'VERIFIED')
        setActiveVehicles(active)
        // If no vehicleId param, default to first active
        if (!initialVehicleId && active.length > 0) {
          setSelectedVehicleId(active[0].id)
        }
      })
      .catch(() => {})
  }, [initialVehicleId])

  // Load QR token whenever selectedVehicleId changes
  useEffect(() => {
    if (activeVehicles.length === 0) return
    setLoading(true)
    const fetchQr = selectedVehicleId
      ? Promise.all([
          getQrTokenForVehicle(selectedVehicleId),
          Promise.resolve(activeVehicles.find((v) => v.id === selectedVehicleId) ?? null),
        ])
      : Promise.all([getQrToken(), Promise.resolve(activeVehicles[0] ?? null)])

    fetchQr
      .then(([qr, v]) => {
        setToken(qr.token)
        setVehicle(v)
        setLastUpdated(new Date().toISOString())
      })
      .catch(() => toast.error('Could not load QR code'))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedVehicleId, activeVehicles])

  const handleVehicleChange = (id: string) => {
    setSelectedVehicleId(id)
    setSearchParams(id ? { vehicleId: id } : {})
  }

  const handleRegenerate = async () => {
    setRegenerating(true)
    try {
      const qr = selectedVehicleId
        ? await regenerateQrTokenForVehicle(selectedVehicleId)
        : await regenerateQrToken()
      setToken(qr.token)
      setLastUpdated(new Date().toISOString())
      toast.success('QR code regenerated successfully')
    } catch {
      toast.error('Could not regenerate QR code')
    } finally {
      setRegenerating(false)
    }
  }

  const handleDownload = () => {
    const svg = document.querySelector<SVGSVGElement>('#vehicle-qr-code svg')
    if (!svg) return toast.error('QR not ready')
    downloadQrAsPng(svg, `fuel-quota-${vehicle?.registrationNumber ?? 'qr'}.png`)
  }

  if (loading && activeVehicles.length === 0)
    return <div className="flex items-center justify-center h-64"><LoadingSpinner size="lg" /></div>

  if (activeVehicles.length === 0)
    return (
      <div className="max-w-lg mx-auto text-center py-20 text-gray-400">
        <p className="font-medium text-gray-600 text-lg">No active vehicles</p>
        <p className="text-sm mt-1">You need at least one verified vehicle to generate a QR code.</p>
      </div>
    )

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Your Fuel Quota QR Code</h1>
        <p className="text-gray-500 text-sm mt-1">Show this QR code to the pump representative when refueling.</p>
      </div>

      {/* Vehicle selector (only shown when multiple active vehicles) */}
      {activeVehicles.length > 1 && (
        <div>
          <label className="label">Select Vehicle</label>
          <div className="relative">
            <select
              className="input-field appearance-none pr-8"
              value={selectedVehicleId ?? ''}
              onChange={(e) => handleVehicleChange(e.target.value)}
            >
              {activeVehicles.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.registrationNumber} — {v.vehicleMake} {v.vehicleColor}
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          </div>
        </div>
      )}

      {/* Vehicle info */}
      {vehicle && (
        <div className="card flex items-center gap-4">
          <div className="flex-1 grid grid-cols-2 gap-y-2 text-sm">
            <div>
              <p className="text-gray-400 text-xs">Registration</p>
              <p className="font-bold text-gray-900 text-base">{vehicle.registrationNumber}</p>
            </div>
            <div>
              <p className="text-gray-400 text-xs">Vehicle</p>
              <p className="font-medium text-gray-700">{vehicle.vehicleMake} · {vehicle.vehicleColor}</p>
            </div>
            <div>
              <p className="text-gray-400 text-xs">Class</p>
              <p className="font-medium text-gray-700">{vehicle.vehicleClass}</p>
            </div>
            <div>
              <p className="text-gray-400 text-xs">Status</p>
              <StatusBadge status={vehicle.status} />
            </div>
          </div>
        </div>
      )}

      {/* QR Code */}
      <div className="card flex flex-col items-center py-8 gap-4">
        {loading ? (
          <div className="h-60 flex items-center justify-center">
            <LoadingSpinner size="lg" />
          </div>
        ) : token ? (
          <div id="vehicle-qr-code" className="p-4 bg-white border-2 border-gray-200 rounded-2xl shadow-md">
            <QRCode value={token} size={220} />
          </div>
        ) : (
          <div className="h-60 flex items-center justify-center text-gray-400">
            QR code unavailable
          </div>
        )}

        {lastUpdated && (
          <div className="flex items-center gap-1.5 text-xs text-gray-400">
            <Clock className="h-3.5 w-3.5" />
            Last updated: {formatDateTime(lastUpdated)}
          </div>
        )}

        <div className="flex gap-3 w-full">
          <button onClick={handleDownload} disabled={!token || loading} className="btn-secondary flex-1 gap-2 text-sm py-2.5">
            <Download className="h-4 w-4" /> Download PNG
          </button>
          <button onClick={handleRegenerate} disabled={regenerating || !token || loading} className="btn-primary flex-1 gap-2 text-sm py-2.5">
            {regenerating ? <LoadingSpinner size="sm" /> : <RefreshCw className="h-4 w-4" />}
            Regenerate
          </button>
        </div>

        <p className="text-xs text-gray-400 text-center max-w-xs">
          QR code contains an encrypted token tied to your vehicle. Regenerating will invalidate the previous code.
        </p>
      </div>
    </div>
  )
}
