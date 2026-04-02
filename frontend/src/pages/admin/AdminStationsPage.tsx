import { useEffect, useState, useCallback } from 'react'
import { Plus, Pencil, Trash2, MapPin } from 'lucide-react'
import { getAllStations, createStation, updateStation, deleteStation } from '@/api/stationApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Pagination from '@/components/common/Pagination'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import type { FuelStation, StationFormData } from '@/types'
import { DISTRICTS } from '@/config/constants'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'

const emptyForm: StationFormData = {
  stationName: '', stationCode: '', latitude: '', longitude: '',
  geofenceRadiusMeters: '100', phoneNumber: '', managerName: '', managerEmail: '', district: DISTRICTS[0], status: 'ACTIVE',
}

export default function AdminStationsPage() {
  const [stations, setStations] = useState<FuelStation[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<FuelStation | null>(null)
  const [form, setForm] = useState<StationFormData>(emptyForm)
  const [saving, setSaving] = useState(false)

  const fetchStations = useCallback(() => {
    setLoading(true)
    getAllStations({ page, size: DEFAULT_PAGE_SIZE })
      .then((d) => { setStations(d.content); setTotalPages(d.totalPages); setTotalElements(d.totalElements) })
      .catch(() => toast.error('Failed to load stations'))
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => { fetchStations() }, [fetchStations])

  const openCreate = () => { setEditing(null); setForm(emptyForm); setModalOpen(true) }
  const openEdit = (s: FuelStation) => {
    setEditing(s)
    setForm({ stationName: s.stationName, stationCode: s.stationCode, latitude: String(s.latitude),
      longitude: String(s.longitude), geofenceRadiusMeters: String(s.geofenceRadiusMeters),
      phoneNumber: s.phoneNumber, managerName: s.managerName, managerEmail: s.managerEmail,
      district: s.district, status: s.status })
    setModalOpen(true)
  }

  const set = (k: keyof StationFormData) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  const handleSave = async () => {
    if (!form.stationName || !form.stationCode || !form.latitude || !form.longitude) {
      toast.error('Please fill required fields'); return
    }
    setSaving(true)
    try {
      if (editing) { await updateStation(editing.id, form); toast.success('Station updated') }
      else { await createStation(form); toast.success('Station created') }
      setModalOpen(false); fetchStations()
    } catch { toast.error('Failed to save station') }
    finally { setSaving(false) }
  }

  const handleDelete = async (s: FuelStation) => {
    if (!confirm(`Delete station "${s.stationName}"?`)) return
    try { await deleteStation(s.id); toast.success('Station deleted'); fetchStations() }
    catch { toast.error('Failed to delete') }
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Fuel Stations</h1>
          <p className="text-sm text-gray-500 mt-0.5">{totalElements} registered stations</p>
        </div>
        <button onClick={openCreate} className="btn-primary gap-2 text-sm">
          <Plus className="h-4 w-4" /> Add Station
        </button>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Station</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden sm:table-cell">Code</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden md:table-cell">District</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden lg:table-cell">Manager</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Status</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} className="py-12 text-center"><LoadingSpinner className="mx-auto" /></td></tr>
              ) : stations.length === 0 ? (
                <tr><td colSpan={6} className="py-12 text-center text-gray-400">No stations found</td></tr>
              ) : stations.map((s) => (
                <tr key={s.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <MapPin className="h-4 w-4 text-brand-500 flex-shrink-0" />
                      <div>
                        <p className="font-medium text-gray-900">{s.stationName}</p>
                        <p className="text-xs text-gray-400">{s.latitude}, {s.longitude}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 hidden sm:table-cell font-mono text-xs text-gray-600">{s.stationCode}</td>
                  <td className="px-4 py-3 hidden md:table-cell text-gray-600">{s.district}</td>
                  <td className="px-4 py-3 hidden lg:table-cell">
                    <p className="text-gray-800">{s.managerName}</p>
                    <p className="text-xs text-gray-400">{s.managerEmail}</p>
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={s.status} /></td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-1.5">
                      <button onClick={() => openEdit(s)} className="p-1.5 text-gray-500 hover:bg-gray-100 rounded-lg transition-colors">
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button onClick={() => handleDelete(s)} className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg transition-colors">
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="px-4 pb-4"><Pagination page={page} totalPages={totalPages} onPageChange={setPage} /></div>
      </div>

      {/* Create/Edit Modal */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Station' : 'Add Fuel Station'} size="lg">
        <div className="grid sm:grid-cols-2 gap-4">
          <div><label className="label">Station Name *</label><input className="input-field" value={form.stationName} onChange={set('stationName')} placeholder="ABC Fuel Station" /></div>
          <div><label className="label">Station Code *</label><input className="input-field" value={form.stationCode} onChange={set('stationCode')} placeholder="ABC-001" /></div>
          <div><label className="label">Latitude *</label><input className="input-field" type="number" step="any" value={form.latitude} onChange={set('latitude')} placeholder="23.8103" /></div>
          <div><label className="label">Longitude *</label><input className="input-field" type="number" step="any" value={form.longitude} onChange={set('longitude')} placeholder="90.4125" /></div>
          <div><label className="label">Geofence Radius (m)</label><input className="input-field" type="number" value={form.geofenceRadiusMeters} onChange={set('geofenceRadiusMeters')} /></div>
          <div><label className="label">District</label>
            <select className="input-field" value={form.district} onChange={set('district')}>
              {DISTRICTS.map((d) => <option key={d}>{d}</option>)}
            </select>
          </div>
          <div><label className="label">Phone Number</label><input className="input-field" value={form.phoneNumber} onChange={set('phoneNumber')} placeholder="+880 1XXX-XXXXXX" /></div>
          <div><label className="label">Manager Name</label><input className="input-field" value={form.managerName} onChange={set('managerName')} /></div>
          <div className="sm:col-span-2"><label className="label">Manager Email</label><input className="input-field" type="email" value={form.managerEmail} onChange={set('managerEmail')} /></div>
          <div><label className="label">Status</label>
            <select className="input-field" value={form.status} onChange={set('status')}>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="SUSPENDED">Suspended</option>
            </select>
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button>
          <button onClick={handleSave} disabled={saving} className="btn-primary gap-2">
            {saving && <LoadingSpinner size="sm" />}
            {editing ? 'Update Station' : 'Create Station'}
          </button>
        </div>
      </Modal>
    </div>
  )
}

