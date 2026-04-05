import { useState, useEffect } from 'react'
import { Plus, Pencil, Trash2, Users } from 'lucide-react'
import { getAllPumpReps, createPumpRep, updatePumpRep, deletePumpRep } from '@/api/pumpRepApi'
import { getAllStations } from '@/api/stationApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Pagination from '@/components/common/Pagination'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import type { PumpRepresentative, PumpRepFormData, FuelStation } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'

const emptyForm: PumpRepFormData = {
  stationId: '', name: '', mobileNumber: '', email: '', employeeId: '', username: '', password: '',
}

export default function AdminPumpRepsPage() {
  const [reps, setReps] = useState<PumpRepresentative[]>([])
  const [stations, setStations] = useState<FuelStation[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<PumpRepresentative | null>(null)
  const [form, setForm] = useState<PumpRepFormData>(emptyForm)
  const [saving, setSaving] = useState(false)

  const fetchData = async () => {
    setLoading(true)
    try {
      const [repsData, stationsData] = await Promise.all([
        getAllPumpReps({ page, size: DEFAULT_PAGE_SIZE }),
        getAllStations({ page: 0, size: 1000 })
      ])
      setReps(repsData.content)
      setTotalPages(repsData.totalPages)
      setTotalElements(repsData.totalElements)
      setStations(stationsData.content)
    } catch {
      toast.error('Failed to load data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [page])

  const openCreate = () => { setEditing(null); setForm(emptyForm); setModalOpen(true) }
  const openEdit = (rep: PumpRepresentative) => {
    setEditing(rep)
    setForm({ stationId: rep.stationId, name: rep.name, mobileNumber: rep.mobileNumber,
      email: rep.email, employeeId: rep.employeeId, username: rep.username, password: '' })
    setModalOpen(true)
  }

  const set = (k: keyof PumpRepFormData) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  const handleSave = async () => {
    if (!form.stationId || !form.name || !form.mobileNumber || !form.email || !form.username) {
      toast.error('Please fill required fields'); return
    }
    if (!editing && !form.password) {
      toast.error('Password is required for new representatives'); return
    }
    setSaving(true)
    try {
      if (editing) { await updatePumpRep(editing.id, form); toast.success('Representative updated') }
      else { await createPumpRep(form); toast.success('Representative created') }
      setModalOpen(false); fetchData()
    } catch { toast.error('Failed to save') }
    finally { setSaving(false) }
  }

  const handleDelete = async (rep: PumpRepresentative) => {
    if (!confirm(`Delete representative "${rep.name}"?`)) return
    try { await deletePumpRep(rep.id); toast.success('Representative deleted'); fetchData() }
    catch { toast.error('Failed to delete') }
  }

  const getStationName = (stationId: string) =>
    stations.find(s => s.id === stationId)?.stationName ?? 'Unknown Station'

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Pump Representatives</h1>
          <p className="text-sm text-gray-500 mt-0.5">{totalElements} representatives</p>
        </div>
        <button onClick={openCreate} className="btn-primary gap-2 text-sm">
          <Plus className="h-4 w-4" /> Add Representative
        </button>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Representative</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden sm:table-cell">Station</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden md:table-cell">Employee ID</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden lg:table-cell">Last Login</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Status</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} className="py-12 text-center"><LoadingSpinner className="mx-auto" /></td></tr>
              ) : reps.length === 0 ? (
                <tr><td colSpan={6} className="py-12 text-center text-gray-400">No representatives found</td></tr>
              ) : reps.map((rep) => (
                <tr key={rep.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <Users className="h-4 w-4 text-brand-500 flex-shrink-0" />
                      <div>
                        <p className="font-medium text-gray-900">{rep.name}</p>
                        <p className="text-xs text-gray-400">{rep.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 hidden sm:table-cell text-gray-600">{getStationName(rep.stationId)}</td>
                  <td className="px-4 py-3 hidden md:table-cell font-mono text-xs text-gray-600">{rep.employeeId}</td>
                  <td className="px-4 py-3 hidden lg:table-cell text-gray-500 text-xs">
                    {rep.lastLoginTimestamp ? new Date(rep.lastLoginTimestamp).toLocaleDateString() : 'Never'}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={rep.status} /></td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-1.5">
                      <button onClick={() => openEdit(rep)} className="p-1.5 text-gray-500 hover:bg-gray-100 rounded-lg transition-colors">
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button onClick={() => handleDelete(rep)} className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg transition-colors">
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
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Representative' : 'Add Pump Representative'} size="lg">
        <div className="grid sm:grid-cols-2 gap-4">
          <div><label className="label">Station *</label>
            <select className="input-field" value={form.stationId} onChange={set('stationId')}>
              <option value="">Select Station</option>
              {stations.map((s) => <option key={s.id} value={s.id}>{s.stationName} ({s.stationCode})</option>)}
            </select>
          </div>
          <div><label className="label">Full Name *</label><input className="input-field" value={form.name} onChange={set('name')} placeholder="John Doe" /></div>
          <div><label className="label">Mobile Number *</label><input className="input-field" value={form.mobileNumber} onChange={set('mobileNumber')} placeholder="01711123456" /></div>
          <div><label className="label">Email *</label><input className="input-field" type="email" value={form.email} onChange={set('email')} placeholder="john@example.com" /></div>
          <div><label className="label">Employee ID *</label><input className="input-field" value={form.employeeId} onChange={set('employeeId')} placeholder="EMP-001" /></div>
          <div><label className="label">Username *</label><input className="input-field" value={form.username} onChange={set('username')} placeholder="john.doe" /></div>
          <div className="sm:col-span-2"><label className="label">Password {editing ? '(leave empty to keep current)' : '*'}</label>
            <input className="input-field" type="password" value={form.password} onChange={set('password')} placeholder={editing ? 'New password' : 'Password'} />
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button>
          <button onClick={handleSave} disabled={saving} className="btn-primary gap-2">
            {saving && <LoadingSpinner size="sm" />}
            {editing ? 'Update Representative' : 'Create Representative'}
          </button>
        </div>
      </Modal>
    </div>
  )
}
