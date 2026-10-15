import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from './AuthContext';
const NotificationContext = createContext(undefined);
export const NotificationProvider = ({ children }) => {
    const { user } = useAuth();
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [toasts, setToasts] = useState([]);
    useEffect(() => {
        if (user) {
            refreshNotifications();
            const interval = setInterval(refreshNotifications, 15000); // Poll every 15s
            return () => clearInterval(interval);
        }
        else {
            setNotifications([]);
            setUnreadCount(0);
        }
    }, [user]);
    const refreshNotifications = async () => {
        try {
            const [notifRes, unreadRes] = await Promise.all([
                api.get('/notifications?size=15'),
                api.get('/notifications/unread-count')
            ]);
            if (notifRes.data?.success && notifRes.data.data?.content) {
                setNotifications(notifRes.data.data.content);
            }
            if (unreadRes.data?.success && unreadRes.data.data?.unreadCount !== undefined) {
                setUnreadCount(unreadRes.data.data.unreadCount);
            }
        }
        catch {
            // Ignored
        }
    };
    const showToast = (message, type = 'info', title) => {
        const id = Math.random().toString(36).substring(2, 9);
        setToasts((prev) => [...prev, { id, title, message, type }]);
        setTimeout(() => {
            dismissToast(id);
        }, 4000);
    };
    const dismissToast = (id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    };
    const markAsRead = async (id) => {
        try {
            await api.put(`/notifications/${id}/read`);
            setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
            setUnreadCount((c) => Math.max(0, c - 1));
        }
        catch {
            // Ignored
        }
    };
    const markAllAsRead = async () => {
        try {
            await api.put('/notifications/read-all');
            setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
            setUnreadCount(0);
        }
        catch {
            // Ignored
        }
    };
    return (<NotificationContext.Provider value={{
            notifications,
            unreadCount,
            toasts,
            showToast,
            dismissToast,
            markAsRead,
            markAllAsRead,
            refreshNotifications,
        }}>
      {children}
      {/* Global Toasts Container */}
      <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
        {toasts.map((toast) => (<div key={toast.id} className={`pointer-events-auto p-4 rounded-xl shadow-lg border text-sm transition-all duration-300 transform translate-y-0 ${toast.type === 'success'
                ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                : toast.type === 'error'
                    ? 'bg-rose-50 border-rose-200 text-rose-900'
                    : 'bg-indigo-50 border-indigo-200 text-indigo-900'}`}>
            <div className="flex items-start justify-between gap-2">
              <div>
                {toast.title && <h5 className="font-semibold">{toast.title}</h5>}
                <p>{toast.message}</p>
              </div>
              <button onClick={() => dismissToast(toast.id)} className="text-slate-400 hover:text-slate-600 font-bold ml-2">
                ×
              </button>
            </div>
          </div>))}
      </div>
    </NotificationContext.Provider>);
};
export const useNotification = () => {
    const context = useContext(NotificationContext);
    if (!context)
        throw new Error('useNotification must be used within a NotificationProvider');
    return context;
};
