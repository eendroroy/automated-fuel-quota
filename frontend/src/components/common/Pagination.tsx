import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PaginationProps {
  page: number          // 0-indexed
  totalPages: number
  onPageChange: (page: number) => void
}

export default function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null

  const pages = Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
    if (totalPages <= 7) return i
    if (page < 4) return i
    if (page > totalPages - 4) return totalPages - 7 + i
    return page - 3 + i
  })

  return (
    <div className="flex items-center justify-end gap-1 mt-4">
      <button
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
        className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
      >
        <ChevronLeft className="h-4 w-4" />
      </button>

      {pages.map((p) => (
        <button
          key={p}
          onClick={() => onPageChange(p)}
          className={`w-8 h-8 rounded-lg text-sm font-medium transition-colors ${
            p === page
              ? 'bg-brand-600 text-white'
              : 'text-gray-600 hover:bg-gray-100'
          }`}
        >
          {p + 1}
        </button>
      ))}

      <button
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </div>
  )
}

