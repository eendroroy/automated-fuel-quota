import { useEffect, useState, useCallback } from 'react'
import { Plus, Trash2, QrCode, Car, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getMyVehicles, addMyVehicle, removeMyVehicle } from '@/api/vehicleApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Modal from '@/components/common/Modal'
import RegistrationNumberInput from '@/components/common/RegistrationNumberInput'
import toast from 'react-hot-toast'
import type { Vehicle, AddVehicleRequest, VehicleStatus } from '@/types'
import { FUEL_TYPES } from '@/config/constants'
import { formatDate } from '@/utils/formatters'

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

const STATUS_COLORS: Record<VehicleStatus, string> = {
  VERIFIED: 'bg-green-50 border-green-200',
  UNVERIFIED: 'bg-red-50 border-red-200',
  DEREGISTERED: 'bg-gray-50 border-gray-300',
}

export default function CustomerVehiclesPage() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [loading, setLoading] = useState(true)
  const [addModalOpen, setAddModalOpen] = useState(false)
  const [removeTarget, setRemoveTarget] = useState<Vehicle | null>(null)
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
      await addMyVehicle({
        ...form,
        engineDisplacement: form.engineDisplacement ? Number(form.engineDisplacement) : undefined,
      })
      toast.success('Vehicle added successfully! Your fuel quota is now active.')
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

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <LoadingSpinner size="lg" />
    </div>
  )

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Vehicles</h1>
          <p className="text-sm text-gray-500 mt-0.5">{vehicles.length} vehicle{vehicles.length !== 1 ? 's' : ''} registered</p>
        </div>
        <button
          onClick={() => { setForm(emptyForm); setAddModalOpen(true) }}
          className="btn-primary gap-2"
        >
          <Plus className="h-4 w-4" /> Add Vehicle
        </button>
      </div>

      {/* Vehicle cards */}
      {vehicles.length === 0 ? (
        <div className="card text-center py-16 text-gray-400">
          <Car className="h-12 w-12 mx-auto mb-3 opacity-30" />
          <p className="font-medium">No vehicles registered yet</p>
          <p className="text-sm mt-1">Click "Add Vehicle" to register your first vehicle.</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {vehicles.map((v) => (
            <div
              key={v.id}
              className={`card border ${STATUS_COLORS[v.status] ?? 'bg-white border-gray-200'}`}
            >
              <div className="flex items-start justify-between gap-4">
                {/* Vehicle details */}
                <div className="flex-1 grid sm:grid-cols-3 gap-x-6 gap-y-2 text-sm">
                  <div>
                    <p className="text-xs text-gray-400">Registration</p>
                    <p className="font-bold text-gray-900 font-mono text-base">{v.registrationNumber}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Vehicle</p>
                    <p className="font-medium text-gray-700">{v.vehicleMake} · {v.vehicleColor}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Class / Fuel</p>
                    <p className="font-medium text-gray-700">{v.vehicleClass} · {v.fuelType}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Reg. Date</p>
                    <p className="font-medium text-gray-700">{formatDate(v.registrationDate)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Status</p>
                    <StatusBadge status={v.status} />
                  </div>
                  {v.status === 'UNVERIFIED' && (
                    <div className="sm:col-span-3">
                      <p className="text-xs text-red-700 bg-red-50 border border-red-200 rounded-lg px-3 py-1.5 inline-block">
                        ⚠ BRTA verification failed. Contact support or wait for re-verification.
                      </p>
                    </div>
                  )}
                </div>

                {/* Actions */}
                <div className="flex flex-col gap-2 flex-shrink-0">
                  {v.status === 'VERIFIED' && (
                    <Link
                      to={`/qr-code?vehicleId=${v.id}`}
                      className="btn-secondary text-xs py-1.5 px-3 gap-1.5"
                    >
                      <QrCode className="h-3.5 w-3.5" /> QR Code
                    </Link>
                  )}
                  {v.status !== 'DEREGISTERED' && (
                    <button
                      onClick={() => setRemoveTarget(v)}
                      className="flex items-center gap-1.5 text-xs text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg px-3 py-1.5 transition-colors border border-red-200"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                      {v.status === 'VERIFIED' ? 'Deregister' : 'Remove'}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
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
              value={{
                brtaOfficeCode: form.brtaOfficeCode,
                vehicleRegistrationCode: form.vehicleRegistrationCode,
                serialPart1: form.serialPart1,
                serialPart2: form.serialPart2,
              }}
              onChange={(val) => setForm((f) => ({ ...f, ...val }))}
            />
            <p className="text-xs text-gray-400 mt-1">
              Select your BRTA region, vehicle category code, then enter the 2-digit and 4-digit serial numbers.
            </p>
          </div>
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="label">Vehicle Make *</label>
              <input className="input-field" placeholder="Toyota, Honda, etc." value={form.vehicleMake}
                onChange={set('vehicleMake')} />
            </div>
            <div>
              <label className="label">Vehicle Color *</label>
              <input className="input-field" placeholder="Silver, Black, etc." value={form.vehicleColor}
                onChange={set('vehicleColor')} />
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
              <input className="input-field" type="date" value={form.registrationDate}
                onChange={set('registrationDate')} />
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
              {submitting ? <LoadingSpinner size="sm" /> : <Plus className="h-4 w-4" />}
              Add Vehicle
            </button>
          </div>
        </div>
      </Modal>

      {/* Remove / Deregister Confirmation Modal */}
      <Modal
        isOpen={!!removeTarget}
        onClose={() => setRemoveTarget(null)}
        title={removeTarget?.status === 'VERIFIED' ? 'Deregister Vehicle' : 'Remove Vehicle'}
      >
        {removeTarget && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              {removeTarget.status === 'VERIFIED' ? (
                <>
                  You are about to <strong>deregister</strong>{' '}
                  <span className="font-semibold font-mono">{removeTarget.registrationNumber}</span>.
                  The vehicle will be marked as deregistered and its fuel quota will be suspended.
                  Transaction history is preserved.
                </>
              ) : (
                <>
                  Remove <span className="font-semibold font-mono">{removeTarget.registrationNumber}</span>{' '}
                  from your account? This action cannot be undone.
                </>
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
    </div>
  )
}

