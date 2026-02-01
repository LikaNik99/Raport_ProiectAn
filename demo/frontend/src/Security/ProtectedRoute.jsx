import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";

export default function ProtectedRoute(){
    const { token, user, loading } = useAuth();

    if (loading) return null; // или Loader

    if (!token || !user) {
        return <Navigate to="/login-register" replace />;
    }
    return <Outlet />;
}