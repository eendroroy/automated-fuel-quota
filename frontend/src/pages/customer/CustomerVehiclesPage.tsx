import { useEffect, useState, useCallback } from 'react'
import { Plus, Trash2, QrCode, Car, X, Droplets, Clock } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getMyVehicles, addMyVehicle, removeMyVehicle, getQrTokenForVehicle, regenerateQrTokenForVehicle } from '@/api/vehicleApi'
import { getVehicleQuota } from '@/api/quotaApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Modal from '@/components/common/Modal'
import RegistrationNumberInput from '@/components/common/RegistrationNumberInput'
import { downloadQrAsPng } from '@/utils/qrHelpers'
import { formatDate, formatDateTime, formatLitres } from '@/utils/formatters'
import toast from 'react-hot-toast'
import QRCode from 'react-qr-code'
import type { Vehicle, Quota, AddVehicleRequest } from '@/types'
import { FUEL_TYPES } from '@/config/constants'

const emptyForm: AddVehicleRequest = {
  brtaOfficeCode: '',
  vehicleRegistrationCode: '',
  serialPart1: '',
  serialPart2: '',
  vehicleMake: '',
  vehicleColor: '',
  fuelType: FUEL_TYPES[0],
  engineDisplacement: undefined,
  registrationDate: '',
}

// ── Quota bar (reusable) ─────────────────────────────────────────────────────
function VehicleQuotaSection({ vehicleId }: { vehicleId: string }) {
  const [quota, setQuota] = useState<Quota | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getVehicleQuota(vehicleId)
      .then(setQuota)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [vehicleId])

  if (loading)
    return (
      <div className="flex items-center gap-2 text-xs text-gray-400 py-1">
        <LoadingSpinner size="sm" /> Loading quota…
      </div>
    )

  if (!quota) return null

  const pct = Math.min((quota.usedLiters / quota.limitLiters) * 100, 100)
  const barColor = pct >= 90 ? 'bg-red-500' : pct >= 60 ? 'bg-amber-400' : 'bg-emerald-500'
  const periodLabel = quota.period.charAt(0) + quota.period.slice(1).toLowerCase()

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between text-xs text-gray-500">
        <span className="flex items-center gap-1"><Droplets className="h-3 w-3" />{periodLabel} Quota</span>
        <span className={pct >= 90 ? 'text-red-600 font-semibold' : ''}>{Math.round(pct)}% used</span>
      </div>
      <div className="h-2 rounded-full bg-gray-100 overflow-hidden">
        <div className={`h-full rounded-full ${barColor} transition-all duration-500`} style={{ width: `${pct}%` }} />
      </div>
      <div className="flex items-center justify-between text-xs">
        <span className="font-semibold text-gray-700">{formatLitres(quota.remainingLiters)} remaining</span>
        <span className="text-gray-400">of {quota.limitLiters}L</span>
      </div>
      <div className="flex items-center gap-1 text-xs text-gray-400">
        <Clock className="h-3 w-3" />
        Resets {formatDateTime(quota.resetTimestamp)}
      </div>
    </div>
  )
}

// ── Inline QR Modal ───────────────────────────────────────────────────────────
function VehicleQrModal({ vehicle, onClose }: { vehicle: Vehicle; onClose: () => void }) {
  const [token, setToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [regenerating, setRegenerating] = useState(false)

  useEffect(() => {
    setLoading(true)
    getQrTokenForVehicle(vehicle.id)
      .then((r) => setToken(r.token))
      .catch(() => toast.error('Could not load QR code'))
      .finally(() => setLoading(false))
  }, [vehicle.id])

  const handleRegenerate = async () => {
    setRegenerating(true)
    try {
      const r = await regenerateQrTokenForVehicle(vehicle.id)
      setToken(r.token)
      toast.success('QR code regenerated')
    } catch {
      toast.error('Could not regenerate QR code')
    } finally {
      setRegenerating(false)
    }
  }

  const handleDownload = () => {
    const svg = document.querySelector<SVGSVGElement>(`#qr-vp-${vehicle.id} svg`)
    if (!svg) return toast.error('QR not ready')
    downloadQrAsPng(svg, `fuel-quota-${vehicle.registrationNumber}.png`)
  }

  return (
    <Modal isOpen onClose={onClose} title="Fuel Quota QR Code">
      <div className="space-y-4">
        <div className="flex items-center gap-3 bg-gray-50 rounded-xl px-4 py-3">
          <Car className="h-5 w-5 text-brand-600 flex-shrink-0" />
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
            <div id={`qr-vp-${vehicle.id}`} className="p-5 bg-white border-2 border-gray-200 rounded-2xl shadow-inner">
              <QRCode value={token} size={200} />
            </div>
          ) : (
            <div className="h-56 flex items-center justify-center text-gray-400">QR code unavailable</div>
          )}
        </div>
        <p className="text-center text-xs text-gray-400">Show this to the pump representative. Valid for 1 hour.</p>
        <div className="flex gap-3">
          <button onClick={handleDownload} disabled={!token || loading} className="btn-secondary flex-1 gap-2 text-sm py-2.5">
            Download
          </button>
          <button onClick={handleRegenerate} disabled={regenerating || !token || loading} className="btn-primary flex-1 gap-2 text-sm py-2.5">
            {regenerating ? <LoadingSpinner size="sm" /> : null} Regenerate
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default function CustomerVehiclesPage() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [loading, setLoading] = useState(true)
  const [addModalOpen, setAddModalOpen] = useState(false)
  const [removeTarget, setRemoveTarget] = useState<Vehicle | null>(null)
  const [qrTarget, setQrTarget] = useState<Vehicle | null>(null)
  const [form, setForm] = useState<AddVehicleRequest>(emptyForm)
  const [submitting, setSubmitting] = useState(false)
  const [removing, setRemoving] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    getMyVehicles()
      .then(setVehicles)
      .catch(() => toast.error('Failed to load vehicles'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const set = (k: keyof AddVehicleRequest) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm((f) => ({ ...f, [k]: e.target.value }))

  const handleAdd = async () => {
    if (!form.brtaOfficeCode || !form.vehicleRegistrationCode ||
        !/^\d{2}$/.test(form.serialPart1) || !/^\d{4}$/.test(form.serialPart2)) {
      toast.error('Please complete the registration number')
      return
    }
    if (!form.vehicleMake || !form.vehicleColor || !form.registrationDate) {
      toast.error('Please fill in all required fields')
      return
    }
    setSubmitting(true)
    try {
      await addMyVehicle({ ...form, engineDisplacement: form.engineDisplacement ? Number(form.engineDisplacement) : undefined })
      toast.success('Vehicle added! Your fuel quota is now active.')
      setAddModalOpen(false)
      setForm(emptyForm)
      load()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? 'Failed to add vehicle')
    } finally {
      setSubmitting(false)
    }
  }

  const handleRemove = async () => {
    if (!removeTarget) return
    setRemoving(true)
    try {
      await removeMyVehicle(removeTarget.id)
      toast.success(`${removeTarget.registrationNumber} removed from your account`)
      setRemoveTarget(null)
      load()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? 'Failed to remove vehicle')
    } finally {
      setRemoving(false)
    }
  }

  if (loading)
    return <div className="flex items-center justify-center h-64"><LoadingSpinner size="lg" /></div>

  const active = vehicles.filter((v) => v.status !== 'DEREGISTERED')
  const deregistered = vehicles.filter((v) => v.status === 'DEREGISTERED')

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Vehicles</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            {active.length} active · {deregistered.length} deregistered
          </p>
        </div>
        <button onClick={() => { setForm(emptyForm); setAddModalOpen(true) }} className="btn-primary gap-2">
          <Plus className="h-4 w-4" /> Add Vehicle
        </button>
      </div>

      {/* Empty state */}
      {vehicles.length === 0 && (
        <div className="card text-center py-16 border border-dashed border-gray-300 bg-gray-50">
          <Car className="h-12 w-12 mx-auto mb-3 text-gray-300" />
          <p className="font-semibold text-gray-600 text-lg">No vehicles registered yet</p>
          <p className="text-sm text-gray-400 mt-1 mb-5">Add your first vehicle to start managing fuel quotas.</p>
          <button onClick={() => setAddModalOpen(true)} className="btn-primary gap-2 inline-flex">
            <Plus className="h-4 w-4" /> Add Vehicle
          </button>
        </div>
      )}

      {/* Active / Unverified vehicles */}
      {active.length > 0 && (
        <div className="grid sm:grid-cols-2 gap-4">
          {active.map((v) => (
            <div key={v.id} className={`card border flex flex-col gap-4 transition-shadow hover:shadow-md ${
              v.status === 'UNVERIFIED' ? 'border-red-200' : 'border-gray-200'
            }`}>
              {/* Card header */}
              <div className="flex items-start gap-3">
                <div className="h-11 w-11 bg-brand-50 rounded-xl flex items-center justify-center flex-shrink-0">
                  <Car className="h-5 w-5 text-brand-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-bold text-gray-900 font-mono text-base leading-tight">{v.registrationNumber}</p>
                  <p className="text-sm text-gray-500">{v.vehicleMake} · {v.vehicleColor}</p>
                </div>
                <StatusBadge status={v.status} />
              </div>

              {/* Meta grid */}
              <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm border-t border-gray-50 pt-3">
                <div>
                  <p className="text-xs text-gray-400">Class</p>
                  <p className="font-medium text-gray-700">{v.vehicleClass}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400">Fuel Type</p>
                  <p className="font-medium text-gray-700">{v.fuelType}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400">Registered</p>
                  <p className="font-medium text-gray-700">{formatDate(v.registrationDate)}</p>
                </div>
                {v.engineDisplacement && (
                  <div>
                    <p className="text-xs text-gray-400">Engine</p>
                    <p className="font-medium text-gray-700">{v.engineDisplacement} cc</p>
                  </div>
                )}
              </div>

              {/* Quota bar (VERIFIED only) */}
              {v.status === 'VERIFIED' && (
                <div className="border-t border-gray-50 pt-3">
                  <VehicleQuotaSection vehicleId={v.id} />
                </div>
              )}

              {/* Unverified warning */}
              {v.status === 'UNVERIFIED' && (
                <div className="border-t border-gray-50 pt-3">
                  <div className="flex items-start gap-2 text-xs text-red-700 bg-red-50 rounded-lg px-3 py-2">
                    <span className="mt-0.5">⚠</span>
                    BRTA verification failed. Contact support or wait for re-verification.
                  </div>
                </div>
              )}

              {/* Actions */}
              <div className="flex gap-2 border-t border-gray-50 pt-3 mt-auto">
                {v.status === 'VERIFIED' && (
                  <button onClick={() => setQrTarget(v)} className="btn-primary flex-1 text-sm py-2 gap-1.5">
                    <QrCode className="h-4 w-4" /> Get QR
                  </button>
                )}
                <Link to={`/transactions?vehicleId=${v.id}`} className="btn-secondary text-sm py-2 px-3 gap-1.5">
                  History
                </Link>
                <button onClick={() => setRemoveTarget(v)}
                  className="flex items-center gap-1.5 text-xs text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg px-3 py-2 transition-colors border border-red-200 ml-auto">
                  <Trash2 className="h-3.5 w-3.5" />
                  {v.status === 'VERIFIED' ? 'Deregister' : 'Remove'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Deregistered vehicles (collapsed appearance) */}
      {deregistered.length > 0 && (
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-3">Deregistered</p>
          <div className="grid sm:grid-cols-2 gap-3">
            {deregistered.map((v) => (
              <div key={v.id} className="card border border-gray-200 opacity-60 flex items-center gap-3 py-3">
                <div className="h-9 w-9 bg-gray-100 rounded-xl flex items-center justify-center flex-shrink-0">
                  <Car className="h-4 w-4 text-gray-400" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-bold text-gray-600 font-mono text-sm">{v.registrationNumber}</p>
                  <p className="text-xs text-gray-400">{v.vehicleMake} · {v.vehicleColor} · {formatDate(v.registrationDate)}</p>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge status={v.status} />
                  <Link to={`/transactions?vehicleId=${v.id}`} className="text-xs text-brand-600 hover:underline whitespace-nowrap">
                    History
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Add Vehicle Modal */}
      <Modal isOpen={addModalOpen} onClose={() => setAddModalOpen(false)} title="Add New Vehicle">
        <div className="space-y-4">
          <p className="text-sm text-gray-500">
            Register a new vehicle. It will be immediately verified and an active fuel quota will be created.
          </p>
          <div>
            <label className="label">Registration Number *</label>
            <RegistrationNumberInput
              value={{ brtaOfficeCode: form.brtaOfficeCode, vehicleRegistrationCode: form.vehicleRegistrationCode, serialPart1: form.serialPart1, serialPart2: form.serialPart2 }}
              onChange={(val) => setForm((f) => ({ ...f, ...val }))}
            />
          </div>
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="label">Vehicle Make *</label>
              <input className="input-field" placeholder="Toyota, Honda, etc." value={form.vehicleMake} onChange={set('vehicleMake')} />
            </div>
            <div>
              <label className="label">Vehicle Color *</label>
              <input className="input-field" placeholder="Silver, Black, etc." value={form.vehicleColor} onChange={set('vehicleColor')} />
            </div>
          </div>
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="label">Fuel Type</label>
              <select className="input-field" value={form.fuelType} onChange={set('fuelType')}>
                {FUEL_TYPES.map((f) => <option key={f}>{f}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Registration Date *</label>
              <input className="input-field" type="date" value={form.registrationDate} onChange={set('registrationDate')} />
            </div>
          </div>
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="label">Engine Displacement <span className="text-gray-400 font-normal">(optional)</span></label>
              <input className="input-field" type="number" placeholder="e.g. 1500" min={50} max={10000}
                value={form.engineDisplacement ?? ''}
                onChange={(e) => setForm((f) => ({ ...f, engineDisplacement: e.target.value ? Number(e.target.value) : undefined }))} />
            </div>
          </div>
          <div className="flex gap-3 justify-end pt-2">
            <button onClick={() => setAddModalOpen(false)} className="btn-secondary">Cancel</button>
            <button onClick={handleAdd} disabled={submitting} className="btn-primary gap-2">
              {submitting ? <LoadingSpinner size="sm" /> : <Plus className="h-4 w-4" />} Add Vehicle
            </button>
          </div>
        </div>
      </Modal>

      {/* Deregister/Remove Confirmation */}
      <Modal isOpen={!!removeTarget} onClose={() => setRemoveTarget(null)}
        title={removeTarget?.status === 'VERIFIED' ? 'Deregister Vehicle' : 'Remove Vehicle'}>
        {removeTarget && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              {removeTarget.status === 'VERIFIED' ? (
                <>You are about to <strong>deregister</strong>{' '}
                  <span className="font-semibold font-mono">{removeTarget.registrationNumber}</span>.
                  The vehicle will be marked as deregistered and its quota suspended. Transaction history is preserved.</>
              ) : (
                <>Remove <span className="font-semibold font-mono">{removeTarget.registrationNumber}</span>{' '}
                  from your account? This action cannot be undone.</>
              )}
            </p>
            <div className="flex gap-3 justify-end">
              <button onClick={() => setRemoveTarget(null)} className="btn-secondary">Cancel</button>
              <button onClick={handleRemove} disabled={removing} className="btn-danger gap-2">
                {removing ? <LoadingSpinner size="sm" /> : <X className="h-4 w-4" />}
                {removeTarget.status === 'VERIFIED' ? 'Confirm Deregister' : 'Remove Vehicle'}
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* QR Modal */}
      {qrTarget && <VehicleQrModal vehicle={qrTarget} onClose={() => setQrTarget(null)} />}
    </div>
  )
}
