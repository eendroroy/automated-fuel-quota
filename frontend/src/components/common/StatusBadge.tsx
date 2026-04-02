import type { VehicleStatus, TransactionStatus, StationStatus, RepStatus, QuotaStatus, ClaimStatus } from '@/types'

type BadgeStatus = VehicleStatus | TransactionStatus | StationStatus | RepStatus | QuotaStatus | ClaimStatus

const variants: Record<BadgeStatus, string> = {
  VERIFIED: 'bg-green-100 text-green-800',
  UNVERIFIED: 'bg-red-100 text-red-800',
  DEREGISTERED: 'bg-gray-100 text-gray-500',
  ACTIVE: 'bg-green-100 text-green-800',
  PENDING: 'bg-yellow-100 text-yellow-800',
  SUSPENDED: 'bg-red-100 text-red-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-gray-100 text-gray-700',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-gray-100 text-gray-700',
  FAILED: 'bg-red-100 text-red-800',
  INACTIVE: 'bg-gray-100 text-gray-700',
  EXPIRED: 'bg-orange-100 text-orange-800',
}

interface BadgeProps {
  status: BadgeStatus
  label?: string
  className?: string
}

export default function StatusBadge({ status, label, className = '' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${variants[status] ?? 'bg-gray-100 text-gray-700'} ${className}`}
    >
      {label ?? status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  )
}
