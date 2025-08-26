import { Navigate, Outlet } from "react-router-dom"
import { useAuth } from "@/store/auth"

export function Protected() {
    const { user } = useAuth()
    return user ? <Outlet/> : <Navigate to="/signin" replace />
}

export function AdminOnly() {
    const { user, isAdmin } = useAuth()
    return user && isAdmin() ? <Outlet/> : <Navigate to="/" replace />
}
