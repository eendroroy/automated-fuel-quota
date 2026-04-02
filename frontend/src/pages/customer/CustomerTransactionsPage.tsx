import { useEffect, useState } from 'react'
import { History, MapPin, Fuel } from 'lucide-react'
import { getMyTransactions } from '@/api/transactionApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Pagination from '@/components/common/Pagination'
import { formatDateTime, formatLitres } from '@/utils/formatters'
import type { Transaction } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'

export default function CustomerTransactionsPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getMyTransactions({ page, size: DEFAULT_PAGE_SIZE })
      .then((data) => {
        setTransactions(data.content)
        setTotalPages(data.totalPages)
        setTotalElements(data.totalElements)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [page])

  const totalLitres = transactions.reduce((s, t) => s + (t.status === 'COMPLETED' ? t.amountDispensedLiters : 0), 0)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Transaction History</h1>
        <p className="text-gray-500 text-sm mt-0.5">All your fuel dispensing records.</p>
      </div>

      {/* Summary */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <div className="card py-4">
          <p className="text-xs text-gray-500">Total Transactions</p>
          <p className="text-2xl font-bold text-gray-900">{totalElements}</p>
        </div>
        <div className="card py-4">
          <p className="text-xs text-gray-500">This Page · Total Litres</p>
          <p className="text-2xl font-bold text-brand-700">{formatLitres(totalLitres)}</p>
        </div>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center gap-2">
          <History className="h-5 w-5 text-brand-600" />
          <h2 className="font-semibold text-gray-900">All Transactions</h2>
        </div>

        {loading ? (
          <div className="flex items-center justify-center h-40"><LoadingSpinner /></div>
        ) : transactions.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <History className="h-10 w-10 mx-auto mb-2 opacity-40" />
            <p>No transactions found</p>
          </div>
        ) : (
          <>
            <div className="divide-y divide-gray-50">
              {transactions.map((tx) => (
                <div key={tx.id} className="flex items-center gap-4 px-6 py-4 hover:bg-gray-50 transition-colors">
                  <div className="h-10 w-10 bg-brand-50 rounded-xl flex items-center justify-center flex-shrink-0">
                    <Fuel className="h-5 w-5 text-brand-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-900 truncate">{tx.stationName}</p>
                    <div className="flex items-center gap-1.5 text-xs text-gray-400 mt-0.5">
                      <MapPin className="h-3 w-3" />
                      {formatDateTime(tx.transactionTimestamp)}
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="font-bold text-gray-900">−{formatLitres(tx.amountDispensedLiters)}</p>
                    <div className="mt-0.5">
                      <StatusBadge status={tx.status} />
                    </div>
                  </div>
                  <div className="text-right hidden sm:block flex-shrink-0 text-xs text-gray-400">
                    <p>Remaining after</p>
                    <p className="font-medium text-gray-700">{formatLitres(tx.remainingQuotaAfter)}</p>
                  </div>
                </div>
              ))}
            </div>
            <div className="px-6 pb-4">
              <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}

