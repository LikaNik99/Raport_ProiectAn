import { createContext, useContext, useEffect, useState } from "react";
import { jwtDecode } from "jwt-decode";

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(localStorage.getItem("token"));
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!token) {
            setUser(null);
            setLoading(false);
            return;
        }
        try {
            const decoded = jwtDecode(token);

            // 🔒 ЖЁСТКАЯ ВАЛИДАЦИЯ JWT
            if (!decoded.sub || !decoded.exp || !decoded.role) {
                throw new Error("JWT missing required claims");
            }

            // ⏱️ ПРОВЕРКА EXP
            const now = Date.now() / 1000;
            if (decoded.exp < now) {
                throw new Error("JWT expired");
            }

            setUser({
                login: decoded.sub,
                role: decoded.role
            });
        } catch (e) {
            console.error("Invalid JWT, logging out:", e);
            logout();
        } finally {
            setLoading(false);
        }
    }, [token]);

    const login = (jwt) => {
        localStorage.setItem("token", jwt);
        setToken(jwt);
    };

    const logout = () => {
        localStorage.removeItem("token");
        setToken(null);
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ token, user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
