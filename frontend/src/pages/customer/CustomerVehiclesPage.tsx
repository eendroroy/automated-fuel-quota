import { useEffect, useState, useCallback } from 'react'
import { Plus, Trash2, QrCode, Car, X, Droplets, Clock, UserPlus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { getMyVehicles, addMyVehicle, removeMyVehicle, getQrTokenForVehicle, regenerateQrTokenForVehicle, getVehiclesAsDriver } from '@/api/vehicleApi'
import { getVehicleQuota } from '@/api/quotaApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Modal from '@/components/common/Modal'
import Pagination from '@/components/common/Pagination'
import RegistrationNumberInput from '@/components/common/RegistrationNumberInput'
import DriverManagementModal from '@/components/customer/DriverManagementModal'
import { downloadQrAsPng } from '@/utils/qrHelpers'
import { formatDate, formatDateTime, formatLitres } from '@/utils/formatters'
import toast from 'react-hot-toast'
import QRCode from 'react-qr-code'
import type { Vehicle, Quota, AddVehicleRequest, PagedResponse } from '@/types'
import { FUEL_TYPES } from '@/config/constants'

const VEHICLES_PER_PAGE = 20

const emptyForm: AddVehicleRequest = {
  brtaOfficeCode: '',
  vehicleRegistrationCode: '',
  serialPart1: '',
  serialPart2: '',
  vehicleMake: '',
  vehicleColor: '',
  fuelType: FUEL_TYPES[0],
  secondaryFuelTypes: [],
  engineDisplacement: undefined,
  registrationDate: '',
}
// ── Quota bar ─────────────────────────────────────────────────────────────────
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
        <LoadingSpinner size="sm" /> <span>Loading…</span>
      </div>
    )
  if (!quota) return null
  const pct = Math.min((quota.usedLiters / quota.limitLiters) * 100, 100)
  const barColor = pct >= 90 ? 'bg-red-500' : pct >= 60 ? 'bg-amber-400' : 'bg-emerald-500'
  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
        <span className="flex items-center gap-1"><Droplets className="h-3 w-3" />{quota.period.charAt(0) + quota.period.slice(1).toLowerCase()} Quota</span>
        <span className={pct >= 90 ? 'text-red-600 font-semibold' : ''}>{Math.round(pct)}% used</span>
      </div>
      <div className="h-1.5 rounded-full bg-gray-100 dark:bg-gray-700 overflow-hidden">
        <div className={`h-full rounded-full ${barColor} transition-all duration-500`} style={{ width: `${pct}%` }} />
      </div>
      <div className="flex items-center justify-between text-xs">
        <span className="font-semibold text-gray-700 dark:text-gray-300">{formatLitres(quota.remainingLiters)} left</span>
        <span className="text-gray-400 flex items-center gap-1"><Clock className="h-3 w-3" />{formatDate(quota.resetTimestamp)}</span>
      </div>
    </div>
  )
}

// ── QR Modal ──────────────────────────────────────────────────────────────────
function VehicleQrModal({ vehicle, onClose }: { vehicle: Vehicle; onClose: () => void }) {
  const { t } = useTranslation()
  const [token, setToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [regenerating, setRegenerating] = useState(false)
  const [selectedFuelType, setSelectedFuelType] = useState(vehicle.fuelType)

  const availableFuelTypes = [vehicle.fuelType, ...(vehicle.secondaryFuelTypes ?? [])]

  const loadQr = useCallback((fuelType: string) => {
    setLoading(true)
    getQrTokenForVehicle(vehicle.id, fuelType !== vehicle.fuelType ? fuelType : undefined)
      .then((r) => setToken(r.token))
      .catch(() => toast.error(t('errors.qrLoadFailed')))
      .finally(() => setLoading(false))
  }, [vehicle.id, vehicle.fuelType, t])

  useEffect(() => { loadQr(selectedFuelType) }, [vehicle.id])  // load on mount only

  const handleFuelTypeChange = (ft: string) => {
    setSelectedFuelType(ft)
    setToken(null)
    loadQr(ft)
  }

  const handleRegenerate = async () => {
    setRegenerating(true)
    try {
      const r = await regenerateQrTokenForVehicle(vehicle.id, selectedFuelType !== vehicle.fuelType ? selectedFuelType : undefined)
      setToken(r.token)
      toast.success(t('qrCode.qrRegenerated'))
    } catch {
      toast.error(t('errors.qrRegenFailed'))
    } finally {
      setRegenerating(false)
    }
  }

  const handleDownload = () => {
    const svg = document.querySelector<SVGSVGElement>(`#qr-vp-${vehicle.id} svg`)
    if (!svg) return toast.error(t('errors.qrLoadFailed'))
    downloadQrAsPng(svg, `fuel-quota-${vehicle.registrationNumber}.png`)
  }

  return (
    <Modal isOpen onClose={onClose} title={t('dashboard.qrModalTitle')}>
      <div className="space-y-3">
        <div className="flex items-center gap-3 bg-gray-50 dark:bg-gray-800 rounded-xl px-3 py-2.5">
          <Car className="h-5 w-5 text-brand-600 flex-shrink-0" />
          <div className="flex-1 min-w-0">
            <p className="font-bold text-gray-900 dark:text-white font-mono text-sm">{vehicle.registrationNumber}</p>
            <p className="text-xs text-gray-500 dark:text-gray-400">{vehicle.vehicleMake} · {vehicle.vehicleColor}</p>
          </div>
          <StatusBadge status={vehicle.status} />
        </div>

        {/* Fuel type selector */}
        {availableFuelTypes.length > 1 && (
          <div>
            <p className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1.5">{t('qrCode.selectFuelType')}</p>
            <div className="flex flex-wrap gap-2">
              {availableFuelTypes.map((ft) => (
                <button
                  key={ft}
                  onClick={() => handleFuelTypeChange(ft)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
                    selectedFuelType === ft
                      ? 'bg-brand-600 text-white border-brand-600'
                      : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border-gray-300 dark:border-gray-600'
                  }`}
                >
                  {ft} {ft === vehicle.fuelType && <span className="opacity-60">(primary)</span>}
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="flex flex-col items-center">
          {loading ? (
            <div className="h-52 flex items-center justify-center"><LoadingSpinner size="lg" /></div>
          ) : token ? (
            <div id={`qr-vp-${vehicle.id}`} className="p-4 bg-white border-2 border-gray-200 rounded-2xl shadow-inner w-full max-w-[240px]">
              <QRCode value={token} size={208} style={{ width: '100%', height: 'auto' }} viewBox="0 0 256 256" />
            </div>
          ) : (
            <div className="h-52 flex items-center justify-center text-gray-400 text-sm">{t('qrCode.noVerifiedVehicles')}</div>
          )}
        </div>
        <p className="text-center text-xs text-gray-400">{t('dashboard.qrShowToRep')}</p>
        <div className="flex gap-2">
          <button onClick={handleDownload} disabled={!token || loading} className="btn-secondary flex-1 gap-2 text-sm py-2">
            {t('qrCode.download')}
          </button>
          <button onClick={handleRegenerate} disabled={regenerating || !token || loading} className="btn-primary flex-1 gap-2 text-sm py-2">
            {regenerating ? <LoadingSpinner size="sm" /> : null} {t('qrCode.regenerate')}
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default function CustomerVehiclesPage() {
  const { t } = useTranslation()
  const [vehiclesData, setVehiclesData] = useState<PagedResponse<Vehicle> | null>(null)
  const [vehiclesAsDriver, setVehiclesAsDriver] = useState<Vehicle[]>([])
  const [loading, setLoading] = useState(true)
  const [addModalOpen, setAddModalOpen] = useState(false)
  const [removeTarget, setRemoveTarget] = useState<Vehicle | null>(null)
  const [qrTarget, setQrTarget] = useState<Vehicle | null>(null)
  const [driverTarget, setDriverTarget] = useState<Vehicle | null>(null)
  const [form, setForm] = useState<AddVehicleRequest>(emptyForm)
  const [submitting, setSubmitting] = useState(false)
  const [removing, setRemoving] = useState(false)
  const [page, setPage] = useState(0)

  const load = useCallback((pageNum: number = 0) => {
    setLoading(true)
    Promise.all([
      getMyVehicles({ page: pageNum, size: VEHICLES_PER_PAGE }),
      getVehiclesAsDriver()
    ])
      .then(([vehicles, asDriver]) => {
        setVehiclesData(vehicles)
        setVehiclesAsDriver(asDriver)
        setPage(pageNum)
      })
      .catch(() => toast.error(t('errors.loadFailed')))
      .finally(() => setLoading(false))
  }, [t])

  useEffect(() => { load(0) }, [load])

  const set = (k: keyof AddVehicleRequest) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm((f) => ({ ...f, [k]: e.target.value }))

  const toggleSecondaryFuel = (ft: string) => {
    setForm((f) => {
      const current = f.secondaryFuelTypes ?? []
      return {
        ...f,
        secondaryFuelTypes: current.includes(ft)
          ? current.filter((x) => x !== ft)
          : [...current, ft],
      }
    })
  }

  const handleAdd = async () => {
    if (!form.brtaOfficeCode || !form.vehicleRegistrationCode ||
        !/^\d{2}$/.test(form.serialPart1) || !/^\d{4}$/.test(form.serialPart2)) {
      toast.error(t('errors.fillAllFields'))
      return
    }
    if (!form.vehicleMake || !form.vehicleColor || !form.registrationDate) {
      toast.error(t('errors.fillAllFields'))
      return
    }
    setSubmitting(true)
    try {
      await addMyVehicle({ ...form, engineDisplacement: form.engineDisplacement ? Number(form.engineDisplacement) : undefined })
      toast.success(t('vehicles.vehicleAddedSuccess'))
      setAddModalOpen(false)
      setForm(emptyForm)
      load(0)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? t('errors.saveFailed'))
    } finally {
      setSubmitting(false)
    }
  }

  const handleRemove = async () => {
    if (!removeTarget) return
    setRemoving(true)
    try {
      await removeMyVehicle(removeTarget.id)
      toast.success(`${removeTarget.registrationNumber} removed`)
      setRemoveTarget(null)
      load(page)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? t('errors.saveFailed'))
    } finally {
      setRemoving(false)
    }
  }

  if (loading)
    return <div className="flex items-center justify-center h-64"><LoadingSpinner size="lg" /></div>

  const vehicles = vehiclesData?.content || []
  const totalElements = vehiclesData?.totalElements || 0
  const totalPages = vehiclesData?.totalPages || 0
  const active = vehicles.filter((v) => v.status !== 'DEREGISTERED')
  const deregistered = vehicles.filter((v) => v.status === 'DEREGISTERED')
  const secondaryFuelOptions = FUEL_TYPES.filter((f) => f !== form.fuelType)

  return (
    <div className="space-y-5 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900 dark:text-white">{t('vehicles.title')}</h1>
          <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
            {totalElements} {totalElements === 1 ? 'vehicle' : 'vehicles'}
          </p>
        </div>
        <button onClick={() => { setForm(emptyForm); setAddModalOpen(true) }} className="btn-primary gap-2 text-sm py-2">
          <Plus className="h-4 w-4" /> {t('vehicles.addVehicle')}
        </button>
      </div>

      {/* Empty state */}
      {totalElements === 0 && (
        <div className="card text-center py-12 border border-dashed border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
          <Car className="h-10 w-10 mx-auto mb-3 text-gray-300 dark:text-gray-600" />
          <p className="font-semibold text-gray-600 dark:text-gray-400">{t('vehicles.noVehiclesYet')}</p>
          <p className="text-sm text-gray-400 mt-1 mb-4">{t('vehicles.noVehiclesYetDesc')}</p>
          <button onClick={() => setAddModalOpen(true)} className="btn-primary gap-2 inline-flex text-sm">
            <Plus className="h-4 w-4" /> {t('vehicles.addVehicle')}
          </button>
        </div>
      )}

      {/* Active / Unverified vehicles — compact grid */}
      {active.length > 0 && (
        <div className="space-y-3">
          <div className="grid sm:grid-cols-2 gap-3">
            {active.map((v) => (
              <div key={v.id} className={`card p-4 flex flex-col gap-3 transition-shadow hover:shadow-md ${
                v.status === 'UNVERIFIED' ? 'border-red-200 dark:border-red-800' : 'border-gray-200 dark:border-gray-700'
              }`}>
                {/* Card header */}
                <div className="flex items-start gap-2.5">
                  <div className="h-9 w-9 bg-brand-50 dark:bg-brand-900/30 rounded-lg flex items-center justify-center flex-shrink-0">
                    <Car className="h-4.5 w-4.5 text-brand-600" style={{ width: '18px', height: '18px' }} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-gray-900 dark:text-white font-mono text-sm leading-tight">{v.registrationNumber}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{v.vehicleMake} · {v.vehicleColor}</p>
                  </div>
                  <StatusBadge status={v.status} />
                </div>

                {/* Meta row */}
                <div className="flex items-center gap-3 text-xs flex-wrap">
                  <span className="flex items-center gap-1 bg-brand-50 dark:bg-brand-900/20 text-brand-700 dark:text-brand-300 px-2 py-0.5 rounded-full font-medium">
                    <Droplets className="h-3 w-3" /> {v.fuelType}
                  </span>
                  {v.secondaryFuelTypes?.map((sf) => (
                    <span key={sf} className="flex items-center gap-1 bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded-full">
                      {sf}
                    </span>
                  ))}
                  <span className="text-gray-400 ml-auto">{formatDate(v.registrationDate)}</span>
                </div>

                {/* Quota bar */}
                {v.status === 'VERIFIED' && (
                  <div className="border-t border-gray-100 dark:border-gray-700/50 pt-2">
                    <VehicleQuotaSection vehicleId={v.id} />
                  </div>
                )}

                {/* Driver info */}
                {v.driverId && (
                  <div className="text-xs text-gray-500 dark:text-gray-400 border-t border-gray-100 dark:border-gray-700/50 pt-2">
                    <span className="font-medium">{t('vehicles.driver')}:</span> {v.driverName} · {v.driverMobile}
                  </div>
                )}

                {/* Unverified warning */}
                {v.status === 'UNVERIFIED' && (
                  <div className="flex items-start gap-2 text-xs text-red-700 dark:text-red-300 bg-red-50 dark:bg-red-900/20 rounded-lg px-2.5 py-2">
                    <span>⚠</span> {t('vehicles.unverifiedWarning')}
                  </div>
                )}

                {/* Actions */}
                <div className="flex gap-1.5 border-t border-gray-100 dark:border-gray-700/50 pt-2 mt-auto">
                  {v.status === 'VERIFIED' && (
                    <>
                      <button onClick={() => setQrTarget(v)} className="btn-primary flex-1 text-xs py-1.5 gap-1">
                        <QrCode className="h-3.5 w-3.5" /> {t('vehicles.getQR')}
                      </button>
                      <button onClick={() => setDriverTarget(v)} className="btn-secondary text-xs py-1.5 px-2.5" title={t('vehicles.driverManagement')}>
                        <UserPlus className="h-3.5 w-3.5" />
                      </button>
                    </>
                  )}
                  <Link to={`/transactions?vehicleId=${v.id}`} className="btn-secondary text-xs py-1.5 px-2.5">
                    {t('vehicles.history')}
                  </Link>
                  <button
                    onClick={() => setRemoveTarget(v)}
                    className="flex items-center gap-1 text-xs text-red-500 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg px-2 py-1.5 transition-colors ml-auto"
                  >
                    <Trash2 className="h-3 w-3" />
                  </button>
                </div>
              </div>
            ))}
          </div>
          {totalPages > 1 && (
            <Pagination
              page={page}
              totalPages={totalPages}
              onPageChange={(p) => { load(p); window.scrollTo({ top: 0, behavior: 'smooth' }) }}
            />
          )}
        </div>
      )}

      {/* Deregistered vehicles */}
      {deregistered.length > 0 && (
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500 mb-2">
            {t('vehicles.deregistered')} ({deregistered.length})
          </p>
          <div className="grid sm:grid-cols-2 gap-2">
            {deregistered.map((v) => (
              <div key={v.id} className="card p-3 border border-gray-200 dark:border-gray-700 opacity-60 flex items-center gap-2.5">
                <Car className="h-4 w-4 text-gray-400 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="font-bold text-gray-600 dark:text-gray-400 font-mono text-sm">{v.registrationNumber}</p>
                  <p className="text-xs text-gray-400">{v.vehicleMake} · {formatDate(v.registrationDate)}</p>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge status={v.status} />
                  <Link to={`/transactions?vehicleId=${v.id}`} className="text-xs text-brand-600 hover:underline whitespace-nowrap">
                    {t('vehicles.history')}
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Add Vehicle Modal */}
      <Modal isOpen={addModalOpen} onClose={() => setAddModalOpen(false)} title={t('vehicles.addVehicleTitle')}>
        <div className="space-y-4">
          <div>
            <label className="label">{t('vehicles.registrationNumber')} *</label>
            <RegistrationNumberInput
              value={{ brtaOfficeCode: form.brtaOfficeCode, vehicleRegistrationCode: form.vehicleRegistrationCode, serialPart1: form.serialPart1, serialPart2: form.serialPart2 }}
              onChange={(val) => setForm((f) => ({ ...f, ...val }))}
            />
          </div>
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="label">{t('vehicles.vehicleMake')} *</label>
              <input className="input-field" placeholder="Toyota, Honda…" value={form.vehicleMake} onChange={set('vehicleMake')} />
            </div>
            <div>
              <label className="label">{t('vehicles.vehicleColor')} *</label>
              <input className="input-field" placeholder="Silver, Black…" value={form.vehicleColor} onChange={set('vehicleColor')} />
            </div>
          </div>
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="label">{t('vehicles.primaryFuelType')}</label>
              <select className="input-field" value={form.fuelType} onChange={set('fuelType')}>
                {FUEL_TYPES.map((f) => <option key={f}>{f}</option>)}
              </select>
            </div>
            <div>
              <label className="label">{t('vehicles.registrationDate')} *</label>
              <input className="input-field" type="date" value={form.registrationDate} onChange={set('registrationDate')} />
            </div>
          </div>
          {/* Secondary fuel types */}
          <div>
            <label className="label">{t('vehicles.secondaryFuelTypes')} <span className="text-gray-400 font-normal text-xs">({t('common.optional')})</span></label>
            <p className="text-xs text-gray-400 dark:text-gray-500 mb-2">{t('vehicles.secondaryFuelTypesHint')}</p>
            <div className="flex flex-wrap gap-2">
              {secondaryFuelOptions.map((ft) => {
                const selected = (form.secondaryFuelTypes ?? []).includes(ft)
                return (
                  <button
                    key={ft}
                    type="button"
                    onClick={() => toggleSecondaryFuel(ft)}
                    className={`px-3 py-1.5 rounded-lg text-sm border transition-all ${
                      selected
                        ? 'bg-brand-100 dark:bg-brand-900/30 border-brand-400 text-brand-700 dark:text-brand-300'
                        : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300'
                    }`}
                  >
                    {ft}
                  </button>
                )
              })}
            </div>
          </div>
          <div>
            <label className="label">{t('vehicles.engineDisplacement')} <span className="text-gray-400 font-normal">({t('common.optional')})</span></label>
            <input className="input-field" type="number" placeholder="e.g. 1500" min={50} max={10000}
              value={form.engineDisplacement ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, engineDisplacement: e.target.value ? Number(e.target.value) : undefined }))} />
          </div>
          <div className="flex gap-3 justify-end pt-2">
            <button onClick={() => setAddModalOpen(false)} className="btn-secondary">{t('common.cancel')}</button>
            <button onClick={handleAdd} disabled={submitting} className="btn-primary gap-2">
              {submitting ? <LoadingSpinner size="sm" /> : <Plus className="h-4 w-4" />} {t('vehicles.addVehicle')}
            </button>
          </div>
        </div>
      </Modal>

      {/* Remove/Deregister Confirmation */}
      <Modal
        isOpen={!!removeTarget}
        onClose={() => setRemoveTarget(null)}
        title={removeTarget?.status === 'VERIFIED' ? t('vehicles.confirmDeregister') : t('vehicles.confirmRemoveVehicle')}
      >
        {removeTarget && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {removeTarget.status === 'VERIFIED'
                ? t('vehicles.deregisterWarning', { reg: removeTarget.registrationNumber })
                : t('vehicles.removeWarning', { reg: removeTarget.registrationNumber })}
            </p>
            <div className="flex gap-3 justify-end">
              <button onClick={() => setRemoveTarget(null)} className="btn-secondary">{t('common.cancel')}</button>
              <button onClick={handleRemove} disabled={removing} className="btn-danger gap-2">
                {removing ? <LoadingSpinner size="sm" /> : <X className="h-4 w-4" />}
                {removeTarget.status === 'VERIFIED' ? t('vehicles.confirmDeregisterBtn') : t('vehicles.removeVehicleBtn')}
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* Driver Management Modal */}
      {driverTarget && (
        <DriverManagementModal
          vehicle={driverTarget}
          onClose={() => setDriverTarget(null)}
          onSuccess={() => load(page)}
        />
      )}

      {/* QR Modal */}
      {qrTarget && <VehicleQrModal vehicle={qrTarget} onClose={() => setQrTarget(null)} />}

      {/* Vehicles Where User is Driver */}
      {vehiclesAsDriver.length > 0 && (
        <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
          <h2 className="text-base font-bold text-gray-900 dark:text-white mb-1">{t('vehicles.vehiclesIDrive')}</h2>
          <p className="text-xs text-gray-500 dark:text-gray-400 mb-3">{t('vehicles.iDriveDesc')}</p>
          <div className="grid sm:grid-cols-2 gap-3">
            {vehiclesAsDriver.map((v) => (
              <div key={v.id} className="card p-3 border border-blue-200 dark:border-blue-800 bg-blue-50/30 dark:bg-blue-900/10">
                <div className="flex items-start gap-2.5 mb-2.5">
                  <div className="h-9 w-9 bg-blue-100 dark:bg-blue-900/30 rounded-lg flex items-center justify-center flex-shrink-0">
                    <Car className="h-4 w-4 text-blue-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-gray-900 dark:text-white font-mono text-sm leading-tight">{v.registrationNumber}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{v.vehicleMake} · {v.vehicleColor}</p>
                  </div>
                  <StatusBadge status={v.status} />
                </div>
                <div className="text-xs space-y-0.5 mb-2.5 bg-white/60 dark:bg-gray-800/60 rounded-lg p-2">
                  <p className="text-gray-500 dark:text-gray-400">{t('vehicles.owner')}</p>
                  <p className="font-medium text-gray-900 dark:text-white">{v.ownerName}</p>
                </div>
                {v.status === 'VERIFIED' && (
                  <button onClick={() => setQrTarget(v)} className="btn-primary w-full text-xs py-2 gap-1.5">
                    <QrCode className="h-3.5 w-3.5" /> {t('dashboard.getQr')}
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

