import { Routes, Route, Navigate } from 'react-router-dom'

// Layouts
import PublicLayout from '@/layouts/PublicLayout'
import CustomerLayout from '@/layouts/CustomerLayout'
import AdminLayout from '@/layouts/AdminLayout'

// Components
import ProtectedRoute from '@/components/common/ProtectedRoute'

// Public Pages
import LandingPage from '@/pages/LandingPage'
import NotFoundPage from '@/pages/NotFoundPage'

// Customer Pages
import CustomerLoginPage from '@/pages/customer/CustomerLoginPage'
import CustomerRegisterPage from '@/pages/customer/CustomerRegisterPage'
import CustomerDashboardPage from '@/pages/customer/CustomerDashboardPage'
import CustomerQRCodePage from '@/pages/customer/CustomerQRCodePage'
import CustomerTransactionsPage from '@/pages/customer/CustomerTransactionsPage'
import CustomerVehiclesPage from '@/pages/customer/CustomerVehiclesPage'
import CustomerClaimsPage from '@/pages/customer/CustomerClaimsPage'

// Admin Pages
import AdminLoginPage from '@/pages/admin/AdminLoginPage'
import AdminDashboardPage from '@/pages/admin/AdminDashboardPage'
import AdminVehiclesPage from '@/pages/admin/AdminVehiclesPage'
import AdminStationsPage from '@/pages/admin/AdminStationsPage'
import AdminQuotasPage from '@/pages/admin/AdminQuotasPage'
import AdminQuotaConfigPage from '@/pages/admin/AdminQuotaConfigPage'
import AdminPumpRepsPage from '@/pages/admin/AdminPumpRepsPage'
import AdminAuditLogsPage from '@/pages/admin/AdminAuditLogsPage'
import AdminVehicleClaimsPage from '@/pages/admin/AdminVehicleClaimsPage'

export default function App() {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<PublicLayout />}>
        <Route index element={<LandingPage />} />
        <Route path="login" element={<CustomerLoginPage />} />
        <Route path="register" element={<CustomerRegisterPage />} />
        <Route path="admin/login" element={<AdminLoginPage />} />
      </Route>

      {/* Customer Protected Routes */}
      <Route path="/" element={<ProtectedRoute requiredRole="CUSTOMER" />}>
        <Route path="/" element={<CustomerLayout />}>
            <Route path="dashboard" element={<CustomerDashboardPage />} />
            <Route path="vehicles" element={<CustomerVehiclesPage />} />
            <Route path="claims" element={<CustomerClaimsPage />} />
            <Route path="qr-code" element={<CustomerQRCodePage />} />
            <Route path="transactions" element={<CustomerTransactionsPage />} />
          </Route>
      </Route>

      {/* Admin Protected Routes */}
      <Route path="/admin" element={<ProtectedRoute requiredRole="ADMIN" />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboardPage />} />
          <Route path="vehicles" element={<AdminVehiclesPage />} />
          <Route path="vehicle-claims" element={<AdminVehicleClaimsPage />} />
          <Route path="stations" element={<AdminStationsPage />} />
          <Route path="quotas" element={<AdminQuotasPage />} />
          <Route path="quota-config" element={<AdminQuotaConfigPage />} />
          <Route path="pump-reps" element={<AdminPumpRepsPage />} />
          <Route path="audit-logs" element={<AdminAuditLogsPage />} />
        </Route>
      </Route>

      {/* Catch all - 404 */}
      <Route path="*" element={<PublicLayout />}>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
