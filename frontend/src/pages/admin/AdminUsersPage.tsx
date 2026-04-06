import { useState, useEffect, useCallback } from 'react'
import { Search, UserX, UserCheck, UserCog, Shield, User, UserPlus, Calendar, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { getAdminUsers, updateUserStatus, createAdminUser } from '@/api/adminUsersApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Pagination from '@/components/common/Pagination'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import { formatDate } from '@/utils/formatters'
import type { AppUser } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'
import { useAuth } from '@/context/AuthContext'

export default function AdminUsersPage() {
  const { t } = useTranslation()
  const { user: currentUser } = useAuth()

  const [users, setUsers] = useState<AppUser[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [createdAtFrom, setCreatedAtFrom] = useState('')
  const [createdAtTo, setCreatedAtTo] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionTarget, setActionTarget] = useState<AppUser | null>(null)
  const [actionType, setActionType] = useState<'suspend' | 'activate' | null>(null)
  const [suspendReason, setSuspendReason] = useState('')
  const [saving, setSaving] = useState(false)

  // Create admin modal state
  const [showCreateAdmin, setShowCreateAdmin] = useState(false)
  const [newAdminName, setNewAdminName] = useState('')
  const [newAdminEmail, setNewAdminEmail] = useState('')
  const [newAdminPassword, setNewAdminPassword] = useState('')
  const [creatingAdmin, setCreatingAdmin] = useState(false)

  const fetchUsers = useCallback(() => {
    setLoading(true)
    getAdminUsers({
      page,
      size: DEFAULT_PAGE_SIZE,
      search,
      role: roleFilter,
      status: statusFilter,
      createdAtFrom: createdAtFrom ? createdAtFrom + 'T00:00:00' : undefined,
      createdAtTo: createdAtTo ? createdAtTo + 'T23:59:59' : undefined,
    })
      .then((d) => {
        setUsers(d.content)
        setTotalPages(d.totalPages)
        setTotalElements(d.totalElements)
      })
      .catch(() => toast.error(t('errors.loadFailed')))
      .finally(() => setLoading(false))
  }, [page, search, roleFilter, statusFilter, createdAtFrom, createdAtTo, t])

  useEffect(() => { fetchUsers() }, [fetchUsers])

  const openSuspend = (user: AppUser) => {
    setActionTarget(user)
    setActionType('suspend')
    setSuspendReason('')
  }

  const openActivate = (user: AppUser) => {
    setActionTarget(user)
    setActionType('activate')
    setSuspendReason('')
  }

  const handleAction = async () => {
    if (!actionTarget || !actionType) return
    if (actionType === 'suspend' && !suspendReason.trim()) {
      toast.error(t('errors.fillAllFields'))
      return
    }
    setSaving(true)
    try {
      await updateUserStatus(actionTarget.id, {
        status: actionType === 'suspend' ? 'SUSPENDED' : 'ACTIVE',
        reason: suspendReason || undefined,
      })
      toast.success(actionType === 'suspend' ? t('adminUsers.suspendSuccess') : t('adminUsers.activateSuccess'))
      setActionTarget(null)
      setActionType(null)
      fetchUsers()
    } catch (err: any) {
      toast.error(err?.response?.data?.message || t('errors.saveFailed'))
    } finally {
      setSaving(false)
    }
  }

  const handleCreateAdmin = async () => {
    if (!newAdminName.trim() || !newAdminEmail.trim() || newAdminPassword.length < 8) {
      toast.error(t('adminUsers.createAdminValidation'))
      return
    }
    setCreatingAdmin(true)
    try {
      await createAdminUser({ name: newAdminName, email: newAdminEmail, password: newAdminPassword })
      toast.success(t('adminUsers.adminCreated'))
      setShowCreateAdmin(false)
      setNewAdminName('')
      setNewAdminEmail('')
      setNewAdminPassword('')
      fetchUsers()
    } catch (err: any) {
      toast.error(err?.response?.data?.message || t('errors.saveFailed'))
    } finally {
      setCreatingAdmin(false)
    }
  }

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('adminUsers.title')}</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
            {t('adminUsers.subtitle')} · {totalElements} {t('adminUsers.userCount_other')}
          </p>
        </div>
        <button
          onClick={() => setShowCreateAdmin(true)}
          className="flex items-center gap-2 btn-primary text-sm"
        >
          <UserPlus className="h-4 w-4" />
          {t('adminUsers.createAdmin')}
        </button>
      </div>

      {/* Filters */}
      <div className="card py-4 space-y-3">
        <div className="flex flex-wrap gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <input
              className="input-field pl-9 text-sm"
              placeholder={t('adminUsers.searchPlaceholder')}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0) }}
            />
            {search && (
              <button onClick={() => { setSearch(''); setPage(0) }} className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-gray-100 rounded">
                <X className="h-3 w-3 text-gray-400" />
              </button>
            )}
          </div>
          <select className="input-field text-sm w-auto" value={roleFilter}
            onChange={(e) => { setRoleFilter(e.target.value); setPage(0) }}>
            <option value="">{t('adminUsers.allRoles')}</option>
            <option value="CUSTOMER">{t('adminUsers.customer')}</option>
            <option value="ADMIN">{t('adminUsers.admin')}</option>
          </select>
          <select className="input-field text-sm w-auto" value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}>
            <option value="">{t('adminUsers.allStatuses')}</option>
            <option value="ACTIVE">{t('status.ACTIVE')}</option>
            <option value="SUSPENDED">{t('status.SUSPENDED')}</option>
            <option value="INACTIVE">{t('status.INACTIVE')}</option>
          </select>
        </div>
        {/* Date range */}
        <div className="flex flex-wrap gap-3 items-center">
          <Calendar className="h-4 w-4 text-gray-400 flex-shrink-0" />
          <div className="flex items-center gap-2">
            <label className="text-xs text-gray-500 dark:text-gray-400">{t('adminAuditLogs.startDate')}</label>
            <input type="date" className="input-field text-sm" value={createdAtFrom}
              onChange={(e) => { setCreatedAtFrom(e.target.value); setPage(0) }} />
          </div>
          <div className="flex items-center gap-2">
            <label className="text-xs text-gray-500 dark:text-gray-400">{t('adminAuditLogs.endDate')}</label>
            <input type="date" className="input-field text-sm" value={createdAtTo}
              onChange={(e) => { setCreatedAtTo(e.target.value); setPage(0) }} />
          </div>
          {(createdAtFrom || createdAtTo) && (
            <button onClick={() => { setCreatedAtFrom(''); setCreatedAtTo(''); setPage(0) }}
              className="text-xs text-red-600 hover:underline">{t('common.reset')}</button>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 dark:bg-gray-800/60 border-b border-gray-100 dark:border-gray-700">
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">{t('adminUsers.name')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400 hidden sm:table-cell">{t('adminUsers.mobile')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400 hidden md:table-cell">{t('adminUsers.role')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400 hidden lg:table-cell">{t('adminUsers.registeredOn')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400 hidden xl:table-cell">{t('adminUsers.lastLogin')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">{t('common.status')}</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={7} className="py-12 text-center"><LoadingSpinner className="mx-auto" /></td></tr>
              ) : users.length === 0 ? (
                <tr><td colSpan={7} className="py-12 text-center text-gray-400">{t('adminUsers.noUsers')}</td></tr>
              ) : users.map((u) => (
                <tr key={u.id} className="border-b border-gray-50 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <div className={`h-8 w-8 rounded-full flex items-center justify-center flex-shrink-0 ${u.role === 'ADMIN' ? 'bg-purple-100 dark:bg-purple-900/30' : 'bg-brand-100 dark:bg-brand-900/30'}`}>
                        {u.role === 'ADMIN' ? <Shield className="h-4 w-4 text-purple-600 dark:text-purple-400" /> : <User className="h-4 w-4 text-brand-600 dark:text-brand-400" />}
                      </div>
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">{u.name}</p>
                        <p className="text-xs text-gray-400">{u.email ?? u.mobileNumber}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 hidden sm:table-cell font-mono text-xs text-gray-600 dark:text-gray-400">{u.mobileNumber}</td>
                  <td className="px-4 py-3 hidden md:table-cell">
                    <span className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ${u.role === 'ADMIN' ? 'bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300' : 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'}`}>
                      {u.role === 'ADMIN' ? <Shield className="h-3 w-3" /> : <User className="h-3 w-3" />}
                      {u.role === 'ADMIN' ? t('adminUsers.admin') : t('adminUsers.customer')}
                    </span>
                  </td>
                  <td className="px-4 py-3 hidden lg:table-cell text-gray-500 dark:text-gray-400 text-xs">
                    {u.createdAt ? formatDate(u.createdAt) : t('common.na')}
                  </td>
                  <td className="px-4 py-3 hidden xl:table-cell text-gray-500 dark:text-gray-400 text-xs">
                    {u.lastLoginTimestamp ? formatDate(u.lastLoginTimestamp) : t('adminUsers.never')}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={u.status} /></td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-1.5">
                      {u.id === currentUser?.id ? (
                        <span className="text-xs text-gray-400 italic">{t('adminUsers.selfNote')}</span>
                      ) : u.status === 'ACTIVE' ? (
                        <button onClick={() => openSuspend(u)}
                          className="flex items-center gap-1 text-xs text-orange-600 dark:text-orange-400 hover:bg-orange-50 dark:hover:bg-orange-900/20 px-2 py-1.5 rounded-lg transition-colors border border-orange-200 dark:border-orange-800"
                          title={t('adminUsers.suspendUser')}>
                          <UserX className="h-3.5 w-3.5" />
                          <span className="hidden sm:inline">{t('adminUsers.suspendUser')}</span>
                        </button>
                      ) : u.status === 'SUSPENDED' ? (
                        <button onClick={() => openActivate(u)}
                          className="flex items-center gap-1 text-xs text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/20 px-2 py-1.5 rounded-lg transition-colors border border-green-200 dark:border-green-800"
                          title={t('adminUsers.activateUser')}>
                          <UserCheck className="h-3.5 w-3.5" />
                          <span className="hidden sm:inline">{t('adminUsers.activateUser')}</span>
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="px-4 pb-4">
          <Pagination page={page} totalPages={totalPages} onPageChange={(p) => { setPage(p); window.scrollTo({ top: 0, behavior: 'smooth' }) }} />
        </div>
      </div>

      {/* Suspend / Activate Modal */}
      <Modal isOpen={!!actionTarget} onClose={() => { setActionTarget(null); setActionType(null) }}
        title={actionType === 'suspend' ? t('adminUsers.suspendUser') : t('adminUsers.activateUser')}>
        {actionTarget && (
          <div className="space-y-4">
            <div className="flex items-center gap-3 bg-gray-50 dark:bg-gray-800 rounded-xl px-4 py-3">
              <UserCog className="h-5 w-5 text-gray-500 flex-shrink-0" />
              <div>
                <p className="font-semibold text-gray-900 dark:text-white">{actionTarget.name}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">{actionTarget.mobileNumber}</p>
              </div>
            </div>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {actionType === 'suspend'
                ? t('adminUsers.confirmSuspend', { name: actionTarget.name })
                : t('adminUsers.confirmActivate', { name: actionTarget.name })}
            </p>
            {actionType === 'suspend' && (
              <div>
                <label className="label">{t('adminUsers.suspendReason')} *</label>
                <textarea className="input-field resize-none" rows={3}
                  placeholder={t('adminUsers.suspendReasonPlaceholder')}
                  value={suspendReason} onChange={(e) => setSuspendReason(e.target.value)} />
              </div>
            )}
            <div className="flex justify-end gap-3">
              <button onClick={() => { setActionTarget(null); setActionType(null) }} className="btn-secondary">{t('common.cancel')}</button>
              <button onClick={handleAction} disabled={saving}
                className={actionType === 'suspend' ? 'btn-danger gap-2' : 'btn-success gap-2'}>
                {saving && <LoadingSpinner size="sm" />}
                {actionType === 'suspend' ? t('adminUsers.suspendBtn') : t('adminUsers.activateBtn')}
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* Create Admin Modal */}
      <Modal isOpen={showCreateAdmin} onClose={() => setShowCreateAdmin(false)} title={t('adminUsers.createAdminTitle')}>
        <div className="space-y-4">
          <div>
            <label className="label">{t('common.name')} *</label>
            <input className="input-field" placeholder={t('adminUsers.adminNamePlaceholder')}
              value={newAdminName} onChange={(e) => setNewAdminName(e.target.value)} />
          </div>
          <div>
            <label className="label">{t('common.email')} *</label>
            <input type="email" className="input-field" placeholder="admin@fuelquota.gov"
              value={newAdminEmail} onChange={(e) => setNewAdminEmail(e.target.value)} />
          </div>
          <div>
            <label className="label">{t('common.password')} *</label>
            <input type="password" className="input-field" placeholder={t('adminUsers.passwordHint')}
              value={newAdminPassword} onChange={(e) => setNewAdminPassword(e.target.value)} />
          </div>
          <div className="flex justify-end gap-3">
            <button onClick={() => { setShowCreateAdmin(false); setNewAdminName(''); setNewAdminEmail(''); setNewAdminPassword('') }}
              className="btn-secondary">{t('common.cancel')}</button>
            <button onClick={handleCreateAdmin} disabled={creatingAdmin} className="btn-primary gap-2">
              {creatingAdmin && <LoadingSpinner size="sm" />}
              {t('adminUsers.createAdmin')}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

