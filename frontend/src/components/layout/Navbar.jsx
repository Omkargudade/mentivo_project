import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useNotification } from '../../context/NotificationContext';
import api from '../../api/client';
import { Button } from '../common/Button';
import { Compass, MessageSquare, Calendar, Bell, User as UserIcon, LogOut, Shield, Menu, X, Repeat, Sparkles, Users } from 'lucide-react';
export const Navbar = () => {
    const { user, currentMode, logout, switchMode } = useAuth();
    const { notifications, unreadCount, markAsRead, markAllAsRead } = useNotification();
    const [unreadMsgCount, setUnreadMsgCount] = useState(0);
    const [showProfileMenu, setShowProfileMenu] = useState(false);
    const [showNotifMenu, setShowNotifMenu] = useState(false);
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const profileMenuRef = useRef(null);
    const notifMenuRef = useRef(null);
    const navigate = useNavigate();
    const location = useLocation();
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (profileMenuRef.current && !profileMenuRef.current.contains(event.target)) {
                setShowProfileMenu(false);
            }
            if (notifMenuRef.current && !notifMenuRef.current.contains(event.target)) {
                setShowNotifMenu(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);
    // Close mobile drawer on route change
    useEffect(() => {
        setMobileMenuOpen(false);
    }, [location.pathname]);
    // Fetch unread messages count
    useEffect(() => {
        if (!user) {
            setUnreadMsgCount(0);
            return;
        }
        const fetchUnread = async () => {
            try {
                const res = await api.get('/conversations/unread-count');
                if (res.data?.success && res.data.data?.unreadCount !== undefined) {
                    setUnreadMsgCount(res.data.data.unreadCount);
                }
            }
            catch {
                // Ignored
            }
        };
        fetchUnread();
        const interval = setInterval(fetchUnread, 15000);
        const handleReadEvent = () => fetchUnread();
        window.addEventListener('mentivo_messages_read', handleReadEvent);
        return () => {
            clearInterval(interval);
            window.removeEventListener('mentivo_messages_read', handleReadEvent);
        };
    }, [user, location.pathname]);
    const handleModeToggle = async () => {
        const nextMode = currentMode === 'LEARNER' ? 'MENTOR' : 'LEARNER';
        await switchMode(nextMode);
        if (nextMode === 'MENTOR') {
            navigate('/mentor/dashboard');
        }
        else {
            navigate('/dashboard');
        }
    };
    const isAdmin = user?.primaryRole === 'ADMIN';
    const isBoth = user?.primaryRole === 'BOTH';
    return (<nav className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand Logo & Tagline */}
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-2 group">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 to-indigo-500 flex items-center justify-center text-white shadow-md shadow-indigo-100 group-hover:scale-105 transition-transform">
                <Sparkles className="w-5 h-5"/>
              </div>
              <div>
                <span className="text-xl font-black tracking-tight text-slate-900">
                  MENTIVO
                </span>
                <span className="hidden sm:block text-[10px] text-indigo-600 font-medium tracking-wide uppercase">
                  Knowledge Marketplace
                </span>
              </div>
            </Link>

            {/* Desktop Navigation Links */}
            <div className="hidden md:flex items-center gap-1">
              {(!user || currentMode === 'LEARNER') ? (
                <>
                  <Link to="/mentors" className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${location.pathname === '/mentors'
                ? 'text-indigo-600 bg-indigo-50/60'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'}`}>
                    Find a Mentor
                  </Link>
                  <Link to="/sessions" className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${location.pathname === '/sessions' && !location.search.includes('tab=bookings')
                ? 'text-indigo-600 bg-indigo-50/60'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'}`}>
                    Sessions
                  </Link>
                  {user && (
                    <Link to="/sessions?tab=bookings" className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${location.pathname === '/bookings' || location.pathname === '/my-bookings' || (location.pathname === '/sessions' && location.search.includes('tab=bookings'))
                  ? 'text-indigo-600 bg-indigo-50/60'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'}`}>
                      My Bookings
                    </Link>
                  )}
                  <Link to="/categories" className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${location.pathname === '/categories'
                ? 'text-indigo-600 bg-indigo-50/60'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'}`}>
                    Categories
                  </Link>
                </>
              ) : (
                /* In Mentor Mode: Dashboard button moves into the exact position previously occupied by Categories */
                <Link to="/mentor/dashboard" className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${location.pathname === '/mentor/dashboard'
                ? 'text-indigo-600 bg-indigo-50/60'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'}`}>
                  Dashboard
                </Link>
              )}
              <Link to="/how-it-works" className="px-3 py-2 rounded-lg text-sm font-medium text-slate-600 hover:text-slate-900 hover:bg-slate-50 transition-colors">
                How It Works
              </Link>
            </div>
          </div>

          {/* Right Header Section */}
          <div className="flex items-center gap-3">
            {user ? (<>
                {/* Mode Switcher Pill (for BOTH users or mentors) */}
                {(isBoth || user.primaryRole === 'MENTOR') && (<button onClick={handleModeToggle} className="hidden sm:inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200 hover:bg-indigo-100 hover:border-indigo-300 transition-all cursor-pointer shadow-2xs" title="Click to switch modes">
                    <Repeat className="w-3.5 h-3.5 text-indigo-600"/>
                    <span>{currentMode === 'LEARNER' ? 'Learning Mode' : 'Mentoring Mode'}</span>
                  </button>)}

                {/* Dashboard Shortcut Link: Only displayed on right for Learner Mode since Mentor Mode has it in the main navigation */}
                {currentMode !== 'MENTOR' && (
                  <Link to="/dashboard" className="hidden md:inline-flex items-center gap-1.5 text-xs font-semibold text-slate-700 hover:text-indigo-600 px-3 py-1.5 rounded-lg hover:bg-slate-100 transition-colors">
                    <Compass className="w-4 h-4"/>
                    <span>Dashboard</span>
                  </Link>
                )}

                {/* Connections Link */}
                <Link to="/connections" className="p-2 text-slate-500 hover:text-indigo-600 hover:bg-slate-100 rounded-lg transition-colors relative" title="My Connections">
                  <Users className="w-5 h-5"/>
                </Link>

                {/* Messages Link */}
                <Link to="/messages" className="p-2 text-slate-500 hover:text-indigo-600 hover:bg-slate-100 rounded-lg transition-colors relative" title="Messages">
                  <MessageSquare className="w-5 h-5"/>
                  {unreadMsgCount > 0 && (<span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-indigo-600 text-white text-[10px] font-bold flex items-center justify-center shadow-xs">
                      {unreadMsgCount > 9 ? '9+' : unreadMsgCount}
                    </span>)}
                </Link>

                {/* Sessions Link */}
                <Link to="/sessions" className="p-2 text-slate-500 hover:text-indigo-600 hover:bg-slate-100 rounded-lg transition-colors" title="My Sessions">
                  <Calendar className="w-5 h-5"/>
                </Link>

                {/* Notification Bell Dropdown */}
                <div className="relative" ref={notifMenuRef}>
                  <button onClick={() => setShowNotifMenu(!showNotifMenu)} className="p-2 text-slate-500 hover:text-indigo-600 hover:bg-slate-100 rounded-lg transition-colors relative cursor-pointer">
                    <Bell className="w-5 h-5"/>
                    {unreadCount > 0 && (<span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center">
                        {unreadCount > 9 ? '9+' : unreadCount}
                      </span>)}
                  </button>

                  {showNotifMenu && (<div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden z-50 animate-in fade-in zoom-in-95 duration-150">
                      <div className="p-4 border-b border-slate-100 flex items-center justify-between">
                        <h4 className="text-sm font-bold text-slate-900">Notifications</h4>
                        {unreadCount > 0 && (<button onClick={markAllAsRead} className="text-xs text-indigo-600 hover:underline font-medium">
                            Mark all as read
                          </button>)}
                      </div>
                      <div className="max-h-80 overflow-y-auto divide-y divide-slate-50">
                        {notifications.length === 0 ? (<div className="p-6 text-center text-xs text-slate-400">
                            No notifications yet
                          </div>) : (notifications.map((n) => (<div key={n.id} onClick={() => markAsRead(n.id)} className={`p-3 text-xs hover:bg-slate-50 cursor-pointer transition-colors ${!n.read ? 'bg-indigo-50/40 font-medium' : ''}`}>
                              <p className="font-semibold text-slate-800">{n.title}</p>
                              <p className="text-slate-600 mt-0.5">{n.message}</p>
                            </div>)))}
                      </div>
                    </div>)}
                </div>

                {/* Profile Dropdown */}
                <div className="relative" ref={profileMenuRef}>
                  <button onClick={() => setShowProfileMenu(!showProfileMenu)} className="flex items-center gap-2 p-1 rounded-full hover:ring-2 hover:ring-indigo-300 transition-all cursor-pointer">
                    <div className="relative">
                      <img src={user.profileImage || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100'} alt={user.fullName} className="w-8 h-8 rounded-full object-cover border border-slate-200"/>
                      <span className={`absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full ring-2 ring-white ${user.onlineStatus === 'ONLINE' ? 'bg-emerald-500' : 'bg-slate-300'}`} />
                    </div>
                  </button>

                  {showProfileMenu && (<div className="absolute right-0 mt-2 w-56 rounded-2xl bg-white shadow-2xl border border-slate-100 p-2 z-50 animate-in fade-in zoom-in-95 duration-150">
                      <div className="p-3 border-b border-slate-100 mb-1">
                        <div className="flex items-center justify-between gap-1 mb-0.5">
                          <p className="text-xs font-bold text-slate-900 truncate">{user.fullName}</p>
                          <span className={`inline-flex items-center gap-1 text-[9px] font-medium px-1.5 py-0.5 rounded-full shrink-0 ${user.onlineStatus === 'ONLINE' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-100 text-slate-600'}`}>
                            <span className={`w-1.5 h-1.5 rounded-full ${user.onlineStatus === 'ONLINE' ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                            {user.onlineStatus === 'ONLINE' ? 'Online' : 'Offline'}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 truncate">{user.email}</p>
                        <span className="inline-block mt-1 text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded bg-indigo-50 text-indigo-700">
                          {user.primaryRole}
                        </span>
                      </div>

                      {(isBoth || user.primaryRole === 'MENTOR') && (<button onClick={handleModeToggle} className="w-full flex items-center gap-2 px-3 py-2 text-xs text-indigo-700 font-medium hover:bg-indigo-50 rounded-xl transition-colors cursor-pointer">
                          <Repeat className="w-4 h-4 text-indigo-600"/>
                          <span>Switch to {currentMode === 'LEARNER' ? 'Mentor' : 'Learner'} Mode</span>
                        </button>)}

                      {currentMode === 'LEARNER' && (
                        <Link to="/sessions?tab=bookings" onClick={() => setShowProfileMenu(false)} className="flex items-center gap-2 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50 rounded-xl transition-colors">
                          <Calendar className="w-4 h-4 text-slate-400"/>
                          <span>My Bookings</span>
                        </Link>
                      )}

                      <Link to="/profile" onClick={() => setShowProfileMenu(false)} className="flex items-center gap-2 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50 rounded-xl transition-colors">
                        <UserIcon className="w-4 h-4 text-slate-400"/>
                        <span>Profile & Public Settings</span>
                      </Link>

                      {currentMode === 'MENTOR' && (<Link to="/mentor/availability" onClick={() => setShowProfileMenu(false)} className="flex items-center gap-2 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50 rounded-xl transition-colors">
                          <Calendar className="w-4 h-4 text-slate-400"/>
                          <span>Availability Hours</span>
                        </Link>)}

                      {isAdmin && (<Link to="/admin" onClick={() => setShowProfileMenu(false)} className="flex items-center gap-2 px-3 py-2 text-xs text-purple-700 font-semibold hover:bg-purple-50 rounded-xl transition-colors">
                          <Shield className="w-4 h-4 text-purple-600"/>
                          <span>Admin Portal</span>
                        </Link>)}

                      <div className="border-t border-slate-100 my-1"></div>

                      <button onClick={() => {
                    setShowProfileMenu(false);
                    logout();
                    navigate('/');
                }} className="w-full flex items-center gap-2 px-3 py-2 text-xs text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer">
                        <LogOut className="w-4 h-4"/>
                        <span>Log Out</span>
                      </button>
                    </div>)}
                </div>
              </>) : (<div className="flex items-center gap-2">
                <Link to="/login">
                  <Button variant="ghost" size="sm">Log In</Button>
                </Link>
                <Link to="/register">
                  <Button variant="primary" size="sm">Get Started</Button>
                </Link>
              </div>)}

            {/* Mobile Hamburger Toggle */}
            <button onClick={() => setMobileMenuOpen(!mobileMenuOpen)} className="md:hidden p-2 text-slate-600 hover:bg-slate-100 rounded-lg cursor-pointer">
              {mobileMenuOpen ? <X className="w-6 h-6"/> : <Menu className="w-6 h-6"/>}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Drawer Menu */}
      {mobileMenuOpen && (<div className="md:hidden border-t border-slate-200 bg-white p-4 space-y-3 animate-in slide-in-from-top-2 duration-150">
          {(!user || currentMode === 'LEARNER') ? (
            <>
              <Link to="/mentors" className="block px-3 py-2 rounded-lg text-sm font-semibold text-slate-700 hover:bg-slate-50">
                Find a Mentor
              </Link>
              <Link to="/sessions" className="block px-3 py-2 rounded-lg text-sm font-semibold text-slate-700 hover:bg-slate-50">
                Sessions
              </Link>
              <Link to="/categories" className="block px-3 py-2 rounded-lg text-sm font-semibold text-slate-700 hover:bg-slate-50">
                Explore Categories
              </Link>
            </>
          ) : (
            <Link to="/mentor/dashboard" className="block px-3 py-2 rounded-lg text-sm font-semibold text-slate-700 hover:bg-slate-50">
              Dashboard
            </Link>
          )}
          <Link to="/how-it-works" className="block px-3 py-2 rounded-lg text-sm font-semibold text-slate-700 hover:bg-slate-50">
            How It Works
          </Link>

          {user && (<div className="pt-2 border-t border-slate-100 space-y-2">
              {(isBoth || user.primaryRole === 'MENTOR') && (<button onClick={handleModeToggle} className="w-full flex items-center justify-between px-3 py-2 text-xs font-semibold rounded-lg bg-indigo-50 text-indigo-700">
                  <span>Active Mode: {currentMode}</span>
                  <Repeat className="w-4 h-4"/>
                </button>)}
              {currentMode !== 'MENTOR' && (
                <Link to="/dashboard" className="block px-3 py-2 rounded-lg text-sm text-slate-700">
                  Dashboard
                </Link>
              )}
              <Link to="/connections" className="block px-3 py-2 rounded-lg text-sm text-slate-700">
                My Connections
              </Link>
              <Link to="/messages" className="flex items-center justify-between px-3 py-2 rounded-lg text-sm text-slate-700 hover:bg-slate-50">
                <span>Messages</span>
                {unreadMsgCount > 0 && (<span className="px-2 py-0.5 rounded-full bg-indigo-600 text-white text-[10px] font-bold">
                    {unreadMsgCount}
                  </span>)}
              </Link>
              <Link to="/sessions" className="block px-3 py-2 rounded-lg text-sm text-slate-700">
                My Sessions
              </Link>
            </div>)}
        </div>)}
    </nav>);
};
export default Navbar;
