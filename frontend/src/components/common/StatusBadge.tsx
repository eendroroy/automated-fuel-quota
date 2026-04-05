import type { VehicleStatus, TransactionStatus, StationStatus, RepStatus, QuotaStatus, UserStatus } from '@/types'

type BadgeStatus = VehicleStatus | TransactionStatus | StationStatus | RepStatus | QuotaStatus | UserStatus

const variants: Partial<Record<string, string>> = {
  VERIFIED: 'bg-green-100 dark:bg-green-900/40 text-green-800 dark:text-green-300',
  UNVERIFIED: 'bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-300',
  DEREGISTERED: 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400',
  ACTIVE: 'bg-green-100 dark:bg-green-900/40 text-green-800 dark:text-green-300',
  SUSPENDED: 'bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-300',
  COMPLETED: 'bg-green-100 dark:bg-green-900/40 text-green-800 dark:text-green-300',
  CANCELLED: 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300',
  FAILED: 'bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-300',
  INACTIVE: 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300',
  EXPIRED: 'bg-orange-100 dark:bg-orange-900/40 text-orange-800 dark:text-orange-300',
}

interface BadgeProps {
  status: BadgeStatus
  label?: string
  className?: string
}

export default function StatusBadge({ status, label, className = '' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${variants[status] ?? 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300'} ${className}`}
    >
      {label ?? status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  )
}
