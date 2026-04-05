import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { History, MapPin, Fuel, Car, X } from 'lucide-react'
import { getMyTransactions, getVehicleTransactions } from '@/api/transactionApi'
import { getMyVehicles } from '@/api/vehicleApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Pagination from '@/components/common/Pagination'
import { formatDateTime, formatLitres } from '@/utils/formatters'
import type { Transaction, Vehicle } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'

export default function CustomerTransactionsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const vehicleIdParam = searchParams.get('vehicleId')

  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [selectedVehicle, setSelectedVehicle] = useState<Vehicle | null>(null)

  // Load vehicle list for filter picker
  useEffect(() => {
    getMyVehicles({ page: 0, size: 100 }) // Fetch a large page for filter dropdown
      .then((response) => setVehicles(response.content))
      .catch(() => {})
  }, [])

  // Sync selectedVehicle from URL param + vehicle list
  useEffect(() => {
    if (!vehicleIdParam) {
      setSelectedVehicle(null)
      return
    }
    const v = vehicles.find((v) => v.id === vehicleIdParam) ?? null
    setSelectedVehicle(v)
  }, [vehicleIdParam, vehicles])

  // Load transactions
  useEffect(() => {
    setLoading(true)
    const fetch = vehicleIdParam
      ? getVehicleTransactions(vehicleIdParam, { page, size: DEFAULT_PAGE_SIZE })
      : getMyTransactions({ page, size: DEFAULT_PAGE_SIZE })

    fetch
      .then((data) => {
        setTransactions(data.content)
        setTotalPages(data.totalPages)
        setTotalElements(data.totalElements)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [page, vehicleIdParam])

  const clearFilter = () => {
    setSearchParams({})
    setPage(0)
  }

  const totalLitres = transactions.reduce(
    (s, t) => s + (t.status === 'COMPLETED' ? t.amountDispensedLiters : 0), 0
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Transaction History</h1>
        <p className="text-gray-500 dark:text-gray-400 text-sm mt-0.5">All your fuel dispensing records across all vehicles.</p>
      </div>

      {/* Vehicle filter — dropdown for large fleets, pills for small ones */}
      {vehicles.length > 1 && (
        vehicles.length > 6 ? (
          <div className="flex items-center gap-3">
            <label htmlFor="vehicle-filter" className="text-sm text-gray-500 dark:text-gray-400 font-medium whitespace-nowrap">
              Filter by vehicle:
            </label>
            <select
              id="vehicle-filter"
              value={vehicleIdParam ?? ''}
              onChange={(e) => {
                if (e.target.value) {
                  setSearchParams({ vehicleId: e.target.value })
                } else {
                  clearFilter()
                }
                setPage(0)
              }}
              className="input-field py-1.5 text-sm max-w-xs"
            >
              <option value="">All Vehicles ({vehicles.length})</option>
              {vehicles.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.registrationNumber} — {v.vehicleMake} {v.vehicleColor}
                </option>
              ))}
            </select>
          </div>
        ) : (
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs text-gray-500 dark:text-gray-400 font-medium">Filter by vehicle:</span>
            <button
              onClick={clearFilter}
              className={`text-xs px-3 py-1.5 rounded-full border transition-colors ${
                !vehicleIdParam
                  ? 'bg-brand-600 text-white border-brand-600'
                  : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:border-brand-400 hover:text-brand-600'
              }`}
            >
              All Vehicles
            </button>
            {vehicles.map((v) => (
              <button
                key={v.id}
                onClick={() => { setSearchParams({ vehicleId: v.id }); setPage(0) }}
                className={`text-xs px-3 py-1.5 rounded-full border font-mono transition-colors ${
                  vehicleIdParam === v.id
                    ? 'bg-brand-600 text-white border-brand-600'
                    : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:border-brand-400 hover:text-brand-600'
                }`}
              >
                {v.registrationNumber}
              </button>
            ))}
          </div>
        )
      )}

      {/* Active filter pill */}
      {selectedVehicle && (
        <div className="flex items-center gap-2 bg-brand-50 dark:bg-brand-900/20 border border-brand-200 dark:border-brand-800 rounded-xl px-4 py-2.5 text-sm">
          <Car className="h-4 w-4 text-brand-600" />
          <span className="font-mono font-semibold text-brand-800 dark:text-brand-300">{selectedVehicle.registrationNumber}</span>
          <span className="text-brand-600 dark:text-brand-400">— {selectedVehicle.vehicleMake} · {selectedVehicle.vehicleColor}</span>
          <button onClick={clearFilter} className="ml-auto text-brand-500 dark:text-brand-400 hover:text-brand-700 dark:hover:text-brand-300">
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Summary */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <div className="card py-4">
          <p className="text-xs text-gray-500 dark:text-gray-400">Total Transactions</p>
          <p className="text-2xl font-bold text-gray-900 dark:text-white">{totalElements}</p>
        </div>
        <div className="card py-4">
          <p className="text-xs text-gray-500 dark:text-gray-400">This Page · Total Litres</p>
          <p className="text-2xl font-bold text-brand-700">{formatLitres(totalLitres)}</p>
        </div>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center gap-2">
          <History className="h-5 w-5 text-brand-600" />
          <h2 className="font-semibold text-gray-900 dark:text-white">
            {selectedVehicle ? `${selectedVehicle.registrationNumber} Transactions` : 'All Transactions'}
          </h2>
        </div>

        {loading ? (
          <div className="flex items-center justify-center h-40"><LoadingSpinner /></div>
        ) : transactions.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <History className="h-10 w-10 mx-auto mb-2 opacity-40" />
            <p>No transactions found</p>
            {vehicleIdParam && (
              <button onClick={clearFilter} className="mt-3 text-sm text-brand-600 hover:underline">
                Clear filter to see all transactions
              </button>
            )}
          </div>
        ) : (
          <>
            <div className="divide-y divide-gray-50 dark:divide-gray-800">
              {transactions.map((tx) => (
                <div key={tx.id} className="flex items-center gap-4 px-6 py-4 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors">
                  <div className="h-10 w-10 bg-brand-50 dark:bg-brand-900/30 rounded-xl flex items-center justify-center flex-shrink-0">
                    <Fuel className="h-5 w-5 text-brand-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-900 dark:text-white truncate">{tx.stationName}</p>
                    <div className="flex items-center gap-2 mt-0.5 flex-wrap">
                      <span className="flex items-center gap-1 text-xs text-gray-400">
                        <MapPin className="h-3 w-3" />
                        {formatDateTime(tx.transactionTimestamp)}
                      </span>
                      {/* Show vehicle tag only in "all" view */}
                      {!vehicleIdParam && tx.registrationNumber && (
                        <Link
                          to={`/transactions?vehicleId=${tx.vehicleId}`}
                          className="text-xs font-mono bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 rounded px-1.5 py-0.5 hover:bg-brand-50 dark:hover:bg-brand-900/30 hover:text-brand-700 dark:hover:text-brand-300 transition-colors"
                        >
                          {tx.registrationNumber}
                        </Link>
                      )}
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="font-bold text-gray-900 dark:text-white">−{formatLitres(tx.amountDispensedLiters)}</p>
                    <div className="mt-0.5">
                      <StatusBadge status={tx.status} />
                    </div>
                  </div>
                  <div className="text-right hidden sm:block flex-shrink-0 text-xs text-gray-400 dark:text-gray-500">
                    <p>Remaining after</p>
                    <p className="font-medium text-gray-700 dark:text-gray-300">{formatLitres(tx.remainingQuotaAfter)}</p>
                  </div>
                </div>
              ))}
            </div>
            <div className="px-6 pb-4">
              <Pagination page={page} totalPages={totalPages} onPageChange={(p) => { setPage(p) }} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
