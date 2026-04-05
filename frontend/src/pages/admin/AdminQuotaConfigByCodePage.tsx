import { Navigate } from 'react-router-dom'

// This page has been merged into AdminQuotaConfigPage.
// Config sets (with multiple registration codes per set) now live at /admin/quota-config.
export default function AdminQuotaConfigByCodePage() {
  return <Navigate to="/admin/quota-config" replace />
}
