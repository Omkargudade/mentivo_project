import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';
const AuthContext = createContext(undefined);
export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(() => {
        const saved = localStorage.getItem('mentivo_user');
        return saved ? JSON.parse(saved) : null;
    });
    const [token, setToken] = useState(() => localStorage.getItem('mentivo_token'));
    const [currentMode, setCurrentMode] = useState(() => {
        const saved = localStorage.getItem('mentivo_user');
        if (saved) {
            return JSON.parse(saved).currentMode || 'LEARNER';
        }
        return 'LEARNER';
    });
    const [isLoading, setIsLoading] = useState(true);
    useEffect(() => {
        if (token) {
            refreshUser().finally(() => setIsLoading(false));
        }
        else {
            setIsLoading(false);
        }
    }, [token]);
    const refreshUser = async () => {
        try {
            const res = await api.get('/users/me');
            if (res.data?.success && res.data.data) {
                const u = res.data.data;
                setUser(u);
                setCurrentMode(u.currentMode || 'LEARNER');
                localStorage.setItem('mentivo_user', JSON.stringify(u));
            }
        }
        catch {
            // If token invalid, keep cached or clear
        }
    };
    const login = async (email, password) => {
        const res = await api.post('/auth/login', { email, password });
        if (res.data?.success && res.data.data) {
            const data = res.data.data;
            const { token: jwt, ...userData } = data;
            if (jwt) {
                localStorage.setItem('mentivo_token', jwt);
                localStorage.setItem('mentivo_user', JSON.stringify(userData));
                setToken(jwt);
                setUser(userData);
                setCurrentMode(userData.currentMode || 'LEARNER');
            }
            return data;
        }
    };
    const register = async (data) => {
        const res = await api.post('/auth/register', data);
        if (res.data?.success && res.data.data) {
            return res.data.data;
        }
    };
    const logout = () => {
        localStorage.removeItem('mentivo_token');
        localStorage.removeItem('mentivo_user');
        setToken(null);
        setUser(null);
    };
    const switchMode = async (mode) => {
        try {
            const res = await api.post('/auth/switch-mode', { mode });
            if (res.data?.success && res.data.data) {
                const updated = res.data.data;
                setUser(updated);
                setCurrentMode(updated.currentMode || mode);
                localStorage.setItem('mentivo_user', JSON.stringify(updated));
            }
        }
        catch {
            // Optimistic update
            setCurrentMode(mode);
            if (user) {
                const u = { ...user, currentMode: mode };
                setUser(u);
                localStorage.setItem('mentivo_user', JSON.stringify(u));
            }
        }
    };
    const forgotPassword = async (email) => {
        const res = await api.post('/auth/forgot-password', { email });
        return res.data;
    };
    const resetPassword = async (token, newPassword, confirmPassword) => {
        const res = await api.post('/auth/reset-password', { token, newPassword, confirmPassword });
        return res.data;
    };
    return (<AuthContext.Provider value={{
            user,
            token,
            currentMode,
            isLoading,
            login,
            register,
            forgotPassword,
            resetPassword,
            logout,
            switchMode,
            refreshUser,
        }}>
      {children}
    </AuthContext.Provider>);
};
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context)
        throw new Error('useAuth must be used within an AuthProvider');
    return context;
};
