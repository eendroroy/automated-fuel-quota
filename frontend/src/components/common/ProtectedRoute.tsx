import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import type { UserRole } from '@/types'

interface ProtectedRouteProps {
  requiredRole: UserRole
  redirectTo?: string
}

export default function ProtectedRoute({ requiredRole, redirectTo }: ProtectedRouteProps) {
  const { isAuthenticated, role } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to={redirectTo ?? (requiredRole === 'ADMIN' ? '/admin/login' : '/login')} replace />
  }

  if (role !== requiredRole) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}

