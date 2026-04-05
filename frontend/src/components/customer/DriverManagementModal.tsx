import { useState } from 'react'
import { UserPlus, UserMinus, X } from 'lucide-react'
import { assignDriver, removeDriver } from '@/api/vehicleApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import toast from 'react-hot-toast'
import { useTranslation } from 'react-i18next'
import type { Vehicle } from '@/types'

interface DriverManagementModalProps {
  vehicle: Vehicle
  onClose: () => void
  onSuccess: () => void
}

export default function DriverManagementModal({ vehicle, onClose, onSuccess }: DriverManagementModalProps) {
  const { t } = useTranslation()
  const [driverMobile, setDriverMobile] = useState('')
  const [loading, setLoading] = useState(false)

  const handleAssign = async () => {
    if (!driverMobile.trim()) {
      toast.error(t('errors.mobileRequired'))
      return
    }
    if (!/^01[3-9]\d{8}$/.test(driverMobile.trim())) {
      toast.error(t('errors.mobileInvalid'))
      return
    }

    setLoading(true)
    try {
      await assignDriver(vehicle.id, { driverMobile: driverMobile.trim() })
      toast.success(t('vehicles.driverAssigned'))
      onSuccess()
      onClose()
    } catch (err: any) {
      const msg = err?.response?.data?.message || t('errors.saveFailed')
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  const handleRemove = async () => {
    if (!confirm(t('vehicles.confirmRemoveDriver'))) return

    setLoading(true)
    try {
      await removeDriver(vehicle.id)
      toast.success(t('vehicles.driverRemoved'))
      onSuccess()
      onClose()
    } catch (err: any) {
      const msg = err?.response?.data?.message || t('errors.saveFailed')
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white dark:bg-gray-900 rounded-xl shadow-xl w-full max-w-md">
        <div className="flex items-center justify-between p-6 border-b border-gray-100 dark:border-gray-700">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">{t('vehicles.driverManagement')}</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-6 space-y-4">
          <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
            <p className="text-sm text-gray-600 dark:text-gray-300 mb-2">
              <strong>{t('vehicles.registrationNumber')}:</strong> {vehicle.registrationNumber}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {vehicle.vehicleMake} • {vehicle.vehicleColor} • {vehicle.fuelType}
            </p>
          </div>

          {vehicle.driverId ? (
            <div className="bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
              <p className="text-sm font-medium text-blue-900 dark:text-blue-300 mb-1">{t('vehicles.currentDriver')}</p>
              <p className="text-sm text-blue-700 dark:text-blue-400">
                <strong>{vehicle.driverName}</strong>
              </p>
              {vehicle.driverMobile && (
                <p className="text-xs text-blue-600 dark:text-blue-500 mt-1">{vehicle.driverMobile}</p>
              )}
              <button
                onClick={handleRemove}
                disabled={loading}
                className="mt-3 flex items-center gap-2 text-sm text-red-600 hover:text-red-700 font-medium"
              >
                {loading ? <LoadingSpinner size="sm" /> : <UserMinus className="h-4 w-4" />}
                {t('vehicles.removeDriver')}
              </button>
            </div>
          ) : (
            <div>
              <label className="label">{t('vehicles.assignDriverByMobile')}</label>
              <input
                type="tel"
                className="input-field mb-2"
                placeholder={t('vehicles.driverMobilePlaceholder')}
                value={driverMobile}
                onChange={(e) => setDriverMobile(e.target.value)}
                disabled={loading}
                autoComplete="tel"
                inputMode="numeric"
                maxLength={11}
              />
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
                The driver must have a registered customer account. They will be able to generate QR codes and use this vehicle for fuel dispensing.
              </p>
              <button
                onClick={handleAssign}
                disabled={loading || !driverMobile.trim()}
                className="btn-primary gap-2 w-full"
              >
                {loading ? <LoadingSpinner size="sm" /> : <UserPlus className="h-4 w-4" />}
                {t('vehicles.assignDriver')}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
