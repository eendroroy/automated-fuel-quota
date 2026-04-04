import { useState } from 'react'
import { UserPlus, UserMinus, X } from 'lucide-react'
import { assignDriver, removeDriver } from '@/api/vehicleApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import toast from 'react-hot-toast'
import type { Vehicle } from '@/types'

interface DriverManagementModalProps {
  vehicle: Vehicle
  onClose: () => void
  onSuccess: () => void
}

export default function DriverManagementModal({ vehicle, onClose, onSuccess }: DriverManagementModalProps) {
  const [driverEmail, setDriverEmail] = useState('')
  const [loading, setLoading] = useState(false)

  const handleAssign = async () => {
    if (!driverEmail.trim()) {
      toast.error('Please enter driver email')
      return
    }

    setLoading(true)
    try {
      await assignDriver(vehicle.id, { driverEmail: driverEmail.trim() })
      toast.success('Driver assigned successfully')
      onSuccess()
      onClose()
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Failed to assign driver'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  const handleRemove = async () => {
    if (!confirm(`Remove ${vehicle.driverName} as driver for this vehicle?`)) return

    setLoading(true)
    try {
      await removeDriver(vehicle.id)
      toast.success('Driver removed successfully')
      onSuccess()
      onClose()
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Failed to remove driver'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-md">
        <div className="flex items-center justify-between p-6 border-b border-gray-100">
          <h3 className="text-lg font-semibold text-gray-900">Driver Management</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-6 space-y-4">
        <div className="bg-gray-50 rounded-lg p-4">
          <p className="text-sm text-gray-600 mb-2">
            <strong>Vehicle:</strong> {vehicle.registrationNumber}
          </p>
          <p className="text-xs text-gray-500">
            {vehicle.vehicleMake} • {vehicle.vehicleColor} • {vehicle.fuelType}
          </p>
        </div>

        {vehicle.driverId ? (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm font-medium text-blue-900 mb-1">Current Driver</p>
            <p className="text-sm text-blue-700">
              <strong>{vehicle.driverName}</strong>
            </p>
            <p className="text-xs text-blue-600 mt-1">{vehicle.driverEmail}</p>
            <button
              onClick={handleRemove}
              disabled={loading}
              className="mt-3 flex items-center gap-2 text-sm text-red-600 hover:text-red-700 font-medium"
            >
              {loading ? <LoadingSpinner size="sm" /> : <UserMinus className="h-4 w-4" />}
              Remove Driver
            </button>
          </div>
        ) : (
          <div>
            <label className="label">Assign Driver by Email</label>
            <input
              type="email"
              className="input-field mb-2"
              placeholder="driver@example.com"
              value={driverEmail}
              onChange={(e) => setDriverEmail(e.target.value)}
              disabled={loading}
            />
            <p className="text-xs text-gray-500 mb-4">
              The driver must have a registered customer account. They will be able to generate QR codes and use this vehicle for fuel dispensing.
            </p>
            <button
              onClick={handleAssign}
              disabled={loading || !driverEmail.trim()}
              className="btn-primary gap-2 w-full"
            >
              {loading ? <LoadingSpinner size="sm" /> : <UserPlus className="h-4 w-4" />}
              Assign Driver
            </button>
          </div>
        )}
      </div>
    </div>
    </div>
  )
}

