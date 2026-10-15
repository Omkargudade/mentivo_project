import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { useSocket } from '../context/SocketContext';
import { useCall } from '../context/CallContext';
import { Button } from '../components/common/Button';
import Badge, { BookingBadge } from '../components/common/Badge';
import api from '../api/client';
import { Compass, Calendar, MessageSquare, Users, Video, Clock, Power } from 'lucide-react';

export const LearnerDashboardPage = () => {
    const { user, refreshUser } = useAuth();
    const { showToast } = useNotification();
    const { subscribeToUserStatus } = useSocket();
    const { startCall } = useCall();
    const navigate = useNavigate();
    const [upcomingBookings, setUpcomingBookings] = useState([]);
    const [connections, setConnections] = useState([]);
    const [conversations, setConversations] = useState([]);
    const [recommendedMentors, setRecommendedMentors] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isOnline, setIsOnline] = useState(user?.onlineStatus === 'ONLINE');
    const [statusUpdating, setStatusUpdating] = useState(false);

    useEffect(() => {
        if (user) {
            setIsOnline(user.onlineStatus === 'ONLINE');
        }
    }, [user?.onlineStatus]);

    // Live sync for learner user status
    useEffect(() => {
        if (!user?.id) return;
        const unsubscribe = subscribeToUserStatus((statusUpdate) => {
            if (statusUpdate && statusUpdate.userId === user.id) {
                setIsOnline(statusUpdate.onlineStatus === 'ONLINE');
            }
        });
        return () => {
            if (unsubscribe) unsubscribe();
        };
    }, [user?.id, subscribeToUserStatus]);

    const toggleOnlineStatus = async () => {
        if (statusUpdating) return;
        try {
            setStatusUpdating(true);
            const nextStatus = isOnline ? 'OFFLINE' : 'ONLINE';
            const res = await api.put('/users/me/status', { status: nextStatus });
            if (res.data?.success) {
                const confirmedStatus = res.data.data?.onlineStatus || nextStatus;
                const newIsOnline = confirmedStatus === 'ONLINE';
                setIsOnline(newIsOnline);
                if (refreshUser) await refreshUser();
                showToast(
                    newIsOnline ? 'You are now Online and available.' : 'You are now Offline.',
                    'success'
                );
            } else {
                throw new Error(res.data?.message || 'Failed to update status');
            }
        } catch (err) {
            console.error('Failed to toggle learner online status', err);
            const msg = err.response?.data?.message || err.message || 'Failed to update status. Please try again.';
            showToast(msg, 'error');
        } finally {
            setStatusUpdating(false);
        }
    };

    useEffect(() => {
        const loadDashboard = async () => {
            setIsLoading(true);
            try {
                const [bookRes, connRes, convRes, recRes] = await Promise.all([
                    api.get('/bookings?role=learner&size=4'),
                    api.get('/connections?size=4'),
                    api.get('/conversations?size=4'),
                    api.get('/mentors?size=3&sortBy=rating'),
                ]);
                if (bookRes.data?.success)
                    setUpcomingBookings(bookRes.data.data.content || []);
                if (connRes.data?.success)
                    setConnections(connRes.data.data.content || []);
                if (convRes.data?.success)
                    setConversations(convRes.data.data.content || []);
                if (recRes.data?.success)
                    setRecommendedMentors(recRes.data.data.content || []);
            }
            catch {
                // Handled
            }
            finally {
                setIsLoading(false);
            }
        };
        loadDashboard();
    }, []);
    return (<div className="min-h-screen bg-slate-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        {/* Welcome Header */}
        <div className="bg-white rounded-3xl border border-slate-200/80 p-6 sm:p-8 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
                Welcome back, {user?.fullName?.split(' ')[0] || 'Learner'}! 👋
              </h1>
              <Badge variant={isOnline ? 'online' : 'offline'}>
                {isOnline ? 'Active Online' : 'Offline'}
              </Badge>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Find someone who knows what you need, book sessions, and level up your skills.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Button
              variant={isOnline ? 'success' : 'secondary'}
              onClick={toggleOnlineStatus}
              isLoading={statusUpdating}
              disabled={statusUpdating}
              className="flex items-center gap-2 cursor-pointer"
            >
              <Power className="w-4 h-4"/>
              {statusUpdating ? 'Updating...' : (isOnline ? 'Go Offline' : 'Go Online')}
            </Button>

            <Link to="/mentors">
              <Button size="md" variant="primary" className="gap-2 shadow-sm shadow-indigo-200">
                <Compass className="w-4 h-4"/>
                <span>Find a Mentor</span>
              </Button>
            </Link>
          </div>
        </div>

        {/* 3-Column Quick Metrics */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-400">Upcoming Sessions</p>
              <h3 className="text-2xl font-black text-slate-900 mt-1">
                {upcomingBookings.filter((b) => b.status === 'CONFIRMED' || b.status === 'PENDING').length}
              </h3>
            </div>
            <div className="w-11 h-11 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <Calendar className="w-5 h-5"/>
            </div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-400">My Connections</p>
              <h3 className="text-2xl font-black text-slate-900 mt-1">{connections.length}</h3>
            </div>
            <div className="w-11 h-11 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <Users className="w-5 h-5"/>
            </div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-400">Conversations</p>
              <h3 className="text-2xl font-black text-slate-900 mt-1">{conversations.length}</h3>
            </div>
            <div className="w-11 h-11 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
              <MessageSquare className="w-5 h-5"/>
            </div>
          </div>
        </div>

        {/* Upcoming Sessions & Recent Connections */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Upcoming Sessions (2 Cols) */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-bold text-slate-900">Upcoming & Recent Sessions</h2>
              <Link to="/sessions?tab=bookings" className="text-xs font-bold text-indigo-600 hover:text-indigo-700">
                View all
              </Link>
            </div>

            {upcomingBookings.length === 0 ? (<div className="bg-white rounded-2xl border border-slate-200 p-8 text-center">
                <Calendar className="w-8 h-8 text-slate-300 mx-auto mb-2"/>
                <h4 className="text-sm font-bold text-slate-800">No sessions booked yet</h4>
                <p className="text-xs text-slate-400 mt-1 mb-4">
                  Find an available mentor and schedule a 1-on-1 session to get help.
                </p>
                <Link to="/mentors">
                  <Button variant="outline" size="sm">Browse Mentors</Button>
                </Link>
              </div>) : (<div className="space-y-3">
                {upcomingBookings.map((booking) => (<div key={booking.id} className="bg-white rounded-2xl border border-slate-200 p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-2xs">
                    <div className="flex items-center gap-3">
                      <img src={booking.mentor?.profileImage || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=80'} alt={booking.mentor?.name} className="w-11 h-11 rounded-xl object-cover border border-slate-200"/>
                      <div>
                        <h4 className="text-sm font-bold text-slate-900">
                          {booking.sessionTitle || booking.topic?.name || 'Mentoring Session'} with {booking.mentor?.name}
                        </h4>
                        <div className="flex items-center gap-2 text-xs text-slate-500 mt-0.5">
                          <span className="flex items-center gap-1 font-mono">
                            <Clock className="w-3.5 h-3.5 text-slate-400"/>
                            {booking.bookingDate} at {booking.startTime?.substring(0, 5)}
                          </span>
                          <span>•</span>
                          <span>{booking.durationMinutes} mins</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-center">
                      <BookingBadge status={booking.status}/>
                      {(booking.status === 'CONFIRMED' || booking.status === 'IN_PROGRESS') && (
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={() => {
                            const targetUserId = booking.mentorUserId || booking.mentor?.userId || booking.mentor?.user?.id;
                            const targetName = booking.mentor?.name || booking.mentor?.user?.fullName || 'Mentor';
                            const targetImage = booking.mentor?.profileImage || booking.mentor?.user?.profileImage;
                            const sessionTitle = booking.sessionTitle || booking.topic?.name || 'Mentorship Session';
                            if (targetUserId) {
                              startCall(targetUserId, targetName, targetImage, {
                                bookingId: booking.id,
                                sessionId: booking.sessionId,
                                sessionTitle
                              });
                            }
                          }}
                          className="gap-1 text-xs cursor-pointer shadow-xs"
                        >
                          <Video className="w-3.5 h-3.5"/>
                          <span>Join Call</span>
                        </Button>
                      )}
                    </div>
                  </div>))}
              </div>)}
          </div>

          {/* Right Column: Recent Connections & Chat shortcut */}
          <div className="space-y-6">
            <div className="bg-white rounded-3xl border border-slate-200/80 p-5 shadow-xs">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-bold text-slate-900">My Mentors & Connections</h3>
                <Link to="/connections" className="text-xs font-semibold text-indigo-600">
                  See all
                </Link>
              </div>

              {connections.length === 0 ? (<p className="text-xs text-slate-400 text-center py-4">No connections yet.</p>) : (<div className="space-y-3">
                  {connections.map((conn) => {
                const peer = conn.sender.id === user?.id ? conn.receiver : conn.sender;
                return (<div key={conn.id} className="flex items-center justify-between text-xs">
                        <div className="flex items-center gap-2.5">
                          <img src={peer.profileImage || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=60'} alt={peer.fullName} className="w-8 h-8 rounded-full object-cover border border-slate-200"/>
                          <div>
                            <p className="font-bold text-slate-800">{peer.fullName}</p>
                            <p className="text-[11px] text-slate-400 truncate max-w-[130px]">
                              {peer.headline || 'Mentor'}
                            </p>
                          </div>
                        </div>

                        <Link to={`/messages`}>
                          <Button size="sm" variant="ghost" className="p-1.5 text-slate-400 hover:text-indigo-600">
                            <MessageSquare className="w-4 h-4"/>
                          </Button>
                        </Link>
                      </div>);
            })}
                </div>)}
            </div>

            {/* Recommended Mentors */}
            <div className="bg-white rounded-3xl border border-slate-200/80 p-5 shadow-xs">
              <h3 className="text-sm font-bold text-slate-900 mb-3">Recommended for You</h3>
              <div className="space-y-3">
                {recommendedMentors.map((m) => (<div key={m.id} className="flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2">
                      <img src={m.profileImage || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=60'} alt={m.name} className="w-8 h-8 rounded-xl object-cover border"/>
                      <div>
                        <Link to={`/mentors/${m.id}`} className="font-bold text-slate-800 hover:underline">
                          {m.name}
                        </Link>
                        <p className="text-[10px] text-slate-400">{Number(m.hourlyRate) === 0 ? 'Free' : `₹${m.hourlyRate}/hr`}</p>
                      </div>
                    </div>
                    <Link to={`/mentors/${m.id}`}>
                      <Button size="sm" variant="outline" className="text-[11px] px-2 py-1">
                        View
                      </Button>
                    </Link>
                  </div>))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>);
};
export default LearnerDashboardPage;
