import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { DollarSign, Clock, Users, Star, Calendar, MessageSquare, CheckCircle2, XCircle, Power, Video, ArrowUpRight, Settings, AlertCircle, Globe, ExternalLink, Save, Mail, Plus, Trash2, Edit2, Layers, BookOpen, Check, X, Search, RefreshCw, UserCheck } from 'lucide-react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { useSocket } from '../context/SocketContext';
import { useCall } from '../context/CallContext';
import Button from '../components/common/Button';
import Badge, { BookingBadge } from '../components/common/Badge';
import UserAvatar from '../components/common/UserAvatar';

export const MentorDashboardPage = () => {
    const { user, refreshUser } = useAuth();
    const { showToast } = useNotification();
    const { subscribeToMentorStatus } = useSocket();
    const { startCall } = useCall();

    const handleStartSessionCall = (booking) => {
        const targetUserId = booking.learnerUserId || booking.learner?.id;
        const targetName = booking.learner?.fullName || 'Learner';
        const targetImage = booking.learner?.profileImage;
        const sessionTitle = booking.sessionTitle || booking.topic?.name || 'Mentorship Session';

        if (!targetUserId) {
            showToast('Learner details are unavailable for this session call.', 'error');
            return;
        }

        startCall(targetUserId, targetName, targetImage, {
            bookingId: booking.id,
            sessionId: booking.sessionId,
            sessionTitle
        });
    };

    const [mentorDetail, setMentorDetail] = useState(null);
    const [upcomingBookings, setUpcomingBookings] = useState([]);
    const [pendingBookings, setPendingBookings] = useState([]);
    const [reviews, setReviews] = useState([]);
    const [isOnline, setIsOnline] = useState(false);
    const [loading, setLoading] = useState(true);
    const [statusUpdating, setStatusUpdating] = useState(false);

    // Booked Learners Section & Summary State (Database Derived)
    const [bookedLearners, setBookedLearners] = useState([]);
    const [loadingBookedLearners, setLoadingBookedLearners] = useState(true);
    const [bookedLearnersError, setBookedLearnersError] = useState(null);
    const [learnerSearchQuery, setLearnerSearchQuery] = useState('');
    const [learnerStatusFilter, setLearnerStatusFilter] = useState('ALL');
    const [bookingSummary, setBookingSummary] = useState({
        totalBookings: 0,
        uniqueLearners: 0,
        upcomingSessions: 0,
        completedSessions: 0,
        totalEarnings: 0
    });

    // Published Sessions & Webinars State
    const [mySessions, setMySessions] = useState([]);
    const [categoriesList, setCategoriesList] = useState([]);
    const [topicsList, setTopicsList] = useState([]);
    const [showSessionModal, setShowSessionModal] = useState(false);
    const [editingSessionId, setEditingSessionId] = useState(null);
    const [savingSession, setSavingSession] = useState(false);
    const [sessionError, setSessionError] = useState('');
    const [sessionForm, setSessionForm] = useState({
        title: '',
        description: '',
        categoryId: '',
        topicId: '',
        sessionType: 'WEBINAR',
        sessionDate: '',
        startTime: '18:00',
        durationMinutes: 60,
        isFree: true,
        price: '0',
        externalMeetingUrl: ''
    });

    // Social & Audience Showcase State
    const [youtubeUrl, setYoutubeUrl] = useState('');
    const [instagramUrl, setInstagramUrl] = useState('');
    const [showYoutubeSubscribers, setShowYoutubeSubscribers] = useState(false);
    const [youtubeSubscribersCount, setYoutubeSubscribersCount] = useState('');
    const [showInstagramFollowers, setShowInstagramFollowers] = useState(false);
    const [instagramFollowersCount, setInstagramFollowersCount] = useState('');
    const [savingSocial, setSavingSocial] = useState(false);
    const [socialSuccessMessage, setSocialSuccessMessage] = useState('');
    const [socialErrorMessage, setSocialErrorMessage] = useState('');

    useEffect(() => {
        fetchMentorDashboardData();
        fetchBookingSummary();
    }, []);

    const fetchBookingSummary = async () => {
        try {
            const res = await api.get('/bookings/mentor/summary');
            if (res.data?.success && res.data.data) {
                setBookingSummary(res.data.data);
            }
        } catch (err) {
            console.error('Failed to load mentor booking summary', err);
        }
    };

    const fetchBookedLearners = async (query = '', status = 'ALL') => {
        try {
            setLoadingBookedLearners(true);
            setBookedLearnersError(null);
            const params = new URLSearchParams();
            if (status && status !== 'ALL') params.append('status', status);
            if (query && query.trim()) params.append('query', query.trim());
            params.append('size', '50');

            const res = await api.get(`/bookings/mentor?${params.toString()}`);
            if (res.data?.success && res.data.data) {
                setBookedLearners(res.data.data);
            }
        } catch (err) {
            console.error('Failed to load booked learners from backend', err);
            setBookedLearnersError(err.response?.data?.message || 'Failed to load booked learners from the database.');
        } finally {
            setLoadingBookedLearners(false);
        }
    };

    // Filter debounce for My Booked Learners search & status filter
    useEffect(() => {
        const timer = setTimeout(() => {
            fetchBookedLearners(learnerSearchQuery, learnerStatusFilter);
        }, 300);
        return () => clearTimeout(timer);
    }, [learnerSearchQuery, learnerStatusFilter]);

    // Live sync for mentor online status
    useEffect(() => {
        if (!user?.id) return;
        const unsubscribe = subscribeToMentorStatus((card) => {
            if (card && (card.userId === user?.id || (mentorDetail && card.id === mentorDetail.id))) {
                const updatedOnline = card.onlineStatus === 'ONLINE';
                setIsOnline(updatedOnline);
                setMentorDetail((prev) => prev ? { ...prev, onlineStatus: card.onlineStatus } : prev);
            }
        });
        return () => {
            if (unsubscribe) unsubscribe();
        };
    }, [user?.id, mentorDetail?.id, subscribeToMentorStatus]);

    const fetchMentorDashboardData = async () => {
        try {
            setLoading(true);
            // Fetch mentor's own profile
            const mentorRes = await api.get('/mentors/me');
            if (mentorRes.data?.success && mentorRes.data.data) {
                const m = mentorRes.data.data;
                setMentorDetail(m);
                setIsOnline(m.onlineStatus === 'ONLINE');
                setYoutubeUrl(m.youtubeUrl || '');
                setInstagramUrl(m.instagramUrl || '');
                setShowYoutubeSubscribers(Boolean(m.showYoutubeSubscribers));
                setYoutubeSubscribersCount(m.youtubeSubscribersCount !== null && m.youtubeSubscribersCount !== undefined ? String(m.youtubeSubscribersCount) : '');
                setShowInstagramFollowers(Boolean(m.showInstagramFollowers));
                setInstagramFollowersCount(m.instagramFollowersCount !== null && m.instagramFollowersCount !== undefined ? String(m.instagramFollowersCount) : '');
            }
            // Fetch bookings for this mentor
            const bookingsRes = await api.get('/bookings/mentor');
            if (bookingsRes.data?.success && bookingsRes.data.data) {
                const allBookings = bookingsRes.data.data;
                setUpcomingBookings(allBookings.filter(b => b.status === 'CONFIRMED' || b.status === 'IN_PROGRESS'));
                setPendingBookings(allBookings.filter(b => b.status === 'PENDING'));
            }
            // Fetch published sessions for this mentor
            try {
                const sessionsRes = await api.get('/sessions/mentor/me');
                if (sessionsRes.data?.success && sessionsRes.data.data) {
                    setMySessions(sessionsRes.data.data);
                }
            } catch (sErr) {
                console.error('Failed to load mentor sessions', sErr);
            }

            // Fetch categories and topics for session creation
            try {
                const [catsRes, topicsRes] = await Promise.all([
                    api.get('/categories'),
                    api.get('/topics')
                ]);
                if (catsRes.data?.success && catsRes.data.data) setCategoriesList(catsRes.data.data);
                if (topicsRes.data?.success && topicsRes.data.data) setTopicsList(topicsRes.data.data);
            } catch (metaErr) {
                console.error('Failed to load categories/topics', metaErr);
            }

            // Fetch reviews
            if (mentorRes.data?.data?.id) {
                const reviewsRes = await api.get(`/reviews/mentor/${mentorRes.data.data.id}`);
                if (reviewsRes.data?.success && reviewsRes.data.data) {
                    setReviews(reviewsRes.data.data.content || reviewsRes.data.data || []);
                }
            }
        }
        catch (err) {
            console.error('Failed to load mentor dashboard data', err);
        }
        finally {
            setLoading(false);
        }
    };

    const handleOpenCreateSession = () => {
        setEditingSessionId(null);
        setSessionError('');
        const defaultCatId = categoriesList.length > 0 ? categoriesList[0].id : '';
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const tomorrowStr = tomorrow.toISOString().split('T')[0];

        setSessionForm({
            title: '',
            description: '',
            categoryId: defaultCatId,
            topicId: '',
            sessionType: 'WEBINAR',
            sessionDate: tomorrowStr,
            startTime: '18:00',
            durationMinutes: 60,
            isFree: true,
            price: '0',
            externalMeetingUrl: ''
        });
        setShowSessionModal(true);
    };

    const handleOpenEditSession = (session) => {
        setEditingSessionId(session.id);
        setSessionError('');
        setSessionForm({
            title: session.title || '',
            description: session.description || '',
            categoryId: session.categoryId || '',
            topicId: session.topicId || '',
            sessionType: session.sessionType || 'WEBINAR',
            sessionDate: session.sessionDate || '',
            startTime: session.startTime ? session.startTime.slice(0, 5) : '18:00',
            durationMinutes: session.durationMinutes || 60,
            isFree: Boolean(session.isFree),
            price: session.price !== undefined && session.price !== null ? String(session.price) : '0',
            externalMeetingUrl: session.externalMeetingUrl || ''
        });
        setShowSessionModal(true);
    };

    const handleSaveSession = async (e) => {
        e.preventDefault();
        setSessionError('');
        if (!sessionForm.title.trim()) {
            setSessionError('Please provide a session or webinar title.');
            return;
        }
        if (!sessionForm.categoryId) {
            setSessionError('Please select a category for this session.');
            return;
        }
        if (!sessionForm.sessionDate) {
            setSessionError('Please select a date for the session.');
            return;
        }

        try {
            setSavingSession(true);
            const payload = {
                title: sessionForm.title.trim(),
                description: sessionForm.description ? sessionForm.description.trim() : null,
                categoryId: Number(sessionForm.categoryId),
                topicId: sessionForm.topicId ? Number(sessionForm.topicId) : null,
                sessionType: sessionForm.sessionType,
                sessionDate: sessionForm.sessionDate,
                startTime: sessionForm.startTime.length === 5 ? `${sessionForm.startTime}:00` : sessionForm.startTime,
                durationMinutes: Number(sessionForm.durationMinutes) || 60,
                isFree: Boolean(sessionForm.isFree),
                price: sessionForm.isFree ? 0 : (parseFloat(sessionForm.price) || 0),
                externalMeetingUrl: sessionForm.externalMeetingUrl ? sessionForm.externalMeetingUrl.trim() : null
            };

            if (editingSessionId) {
                await api.put(`/sessions/${editingSessionId}`, payload);
            } else {
                await api.post('/sessions', payload);
            }

            setShowSessionModal(false);
            // Refresh sessions list
            const refreshRes = await api.get('/sessions/mentor/me');
            if (refreshRes.data?.success && refreshRes.data.data) {
                setMySessions(refreshRes.data.data);
            }
        } catch (err) {
            console.error('Failed to save session', err);
            setSessionError(err.response?.data?.message || 'Failed to publish session. Please check all fields.');
        } finally {
            setSavingSession(false);
        }
    };

    const handleDeleteSession = async (sessionId) => {
        if (!window.confirm('Are you sure you want to delete this session listing?')) return;
        try {
            await api.delete(`/sessions/${sessionId}`);
            setMySessions((prev) => prev.filter((s) => s.id !== sessionId));
        } catch (err) {
            console.error('Failed to delete session', err);
            alert('Failed to delete session. Please try again.');
        }
    };
    const toggleOnlineStatus = async () => {
        if (statusUpdating) return;
        try {
            setStatusUpdating(true);
            const nextStatus = isOnline ? 'OFFLINE' : 'ONLINE';
            const res = await api.put('/mentors/me/status', { status: nextStatus });
            if (res.data?.success) {
                const confirmedStatus = res.data.data?.onlineStatus || nextStatus;
                const newIsOnline = confirmedStatus === 'ONLINE';
                setIsOnline(newIsOnline);
                setMentorDetail((prev) => prev ? { ...prev, onlineStatus: confirmedStatus } : prev);
                if (refreshUser) {
                    await refreshUser();
                }
                showToast(
                    newIsOnline ? 'You are now Online and available for sessions.' : 'You are now Offline.',
                    'success'
                );
            } else {
                throw new Error(res.data?.message || 'Failed to update online status');
            }
        }
        catch (err) {
            console.error('Failed to toggle online status', err);
            const msg = err.response?.data?.message || err.message || 'Failed to update online status. Please try again.';
            showToast(msg, 'error');
        }
        finally {
            setStatusUpdating(false);
        }
    };
    const handleBookingAction = async (bookingId, action) => {
        try {
            if (action === 'CONFIRMED') {
                await api.patch(`/bookings/${bookingId}/confirm`);
            }
            else {
                await api.patch(`/bookings/${bookingId}/cancel`, { reason: 'Declined by mentor' });
            }
            // Refresh bookings and booked learners
            fetchMentorDashboardData();
            fetchBookingSummary();
            fetchBookedLearners(learnerSearchQuery, learnerStatusFilter);
        }
        catch (err) {
            console.error(`Failed to ${action} booking`, err);
        }
    };

    const handleSaveSocialSettings = async (e) => {
        if (e) e.preventDefault();
        try {
            setSavingSocial(true);
            setSocialSuccessMessage('');
            setSocialErrorMessage('');

            const payload = {
                hourlyRate: mentorDetail?.hourlyRate !== undefined ? mentorDetail.hourlyRate : 0,
                experienceYears: mentorDetail?.experienceYears || 1,
                responseTimeMinutes: mentorDetail?.responseTimeMinutes || 15,
                bioSummary: mentorDetail?.bioSummary || '',
                youtubeUrl: youtubeUrl.trim(),
                instagramUrl: instagramUrl.trim(),
                showYoutubeSubscribers: showYoutubeSubscribers,
                youtubeSubscribersCount: youtubeSubscribersCount === '' ? null : Math.max(0, parseInt(youtubeSubscribersCount, 10) || 0),
                showInstagramFollowers: showInstagramFollowers,
                instagramFollowersCount: instagramFollowersCount === '' ? null : Math.max(0, parseInt(instagramFollowersCount, 10) || 0),
            };

            const res = await api.put('/mentors/me', payload);
            if (res.data?.success && res.data.data) {
                const updated = res.data.data;
                setMentorDetail(updated);
                setYoutubeUrl(updated.youtubeUrl || '');
                setInstagramUrl(updated.instagramUrl || '');
                setShowYoutubeSubscribers(Boolean(updated.showYoutubeSubscribers));
                setYoutubeSubscribersCount(updated.youtubeSubscribersCount !== null && updated.youtubeSubscribersCount !== undefined ? String(updated.youtubeSubscribersCount) : '');
                setShowInstagramFollowers(Boolean(updated.showInstagramFollowers));
                setInstagramFollowersCount(updated.instagramFollowersCount !== null && updated.instagramFollowersCount !== undefined ? String(updated.instagramFollowersCount) : '');
                setSocialSuccessMessage('Social media & audience showcase settings saved successfully!');
                setTimeout(() => setSocialSuccessMessage(''), 5000);
            }
        } catch (err) {
            setSocialErrorMessage(err.response?.data?.message || 'Failed to save social settings. Please ensure URLs are valid.');
        } finally {
            setSavingSocial(false);
        }
    };
    if (loading) {
        return (<div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>);
    }
    const totalEarnings = upcomingBookings.reduce((sum, b) => sum + (b.mentorEarnings || 0), 0) +
        (mentorDetail ? mentorDetail.sessionsCompleted * (mentorDetail.hourlyRate * 0.8) : 0);
    return (<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Welcome Header with Online Status Toggle */}
      <div className="bg-white rounded-2xl p-6 sm:p-8 shadow-xs border border-gray-100 mb-8 flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div className="flex items-center gap-4">
          <img src={user?.profileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${user?.fullName || 'Mentor'}`} alt={user?.fullName} className="w-16 h-16 rounded-full border-2 border-indigo-100 object-cover"/>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold text-gray-900">Welcome back, {user?.fullName}!</h1>
              <Badge variant={isOnline ? 'online' : 'offline'}>
                {isOnline ? 'Active Online' : 'Offline'}
              </Badge>
            </div>
            <p className="text-sm text-gray-500 mt-1">
              Mentoring Mode • Rate: <span className="font-semibold text-gray-900">{mentorDetail && Number(mentorDetail.hourlyRate) === 0 ? 'Free (₹0/hr)' : `₹${mentorDetail?.hourlyRate ?? 800}/hr`}</span> • <span className="font-semibold text-indigo-600">{mentorDetail?.learnerFollowerCount ?? 0} Website Followers</span>
            </p>
          </div>
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

          <Link to="/profile">
            <Button variant="outline" className="flex items-center gap-2">
              <Settings className="w-4 h-4"/>
              Edit Profile
            </Button>
          </Link>
        </div>
      </div>

      {/* Accurate Backend-Derived Booking Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-6">
        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-xs flex items-center gap-4 hover:border-blue-200 transition-colors">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
            <BookOpen className="w-6 h-6"/>
          </div>
          <div>
            <p className="text-xs text-gray-500 font-semibold tracking-wider">TOTAL BOOKINGS</p>
            <h3 className="text-2xl font-black text-gray-900">{bookingSummary.totalBookings}</h3>
            <p className="text-xs text-blue-600 font-medium mt-0.5">All-time bookings</p>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-xs flex items-center gap-4 hover:border-indigo-200 transition-colors">
          <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
            <Users className="w-6 h-6"/>
          </div>
          <div>
            <p className="text-xs text-gray-500 font-semibold tracking-wider">UNIQUE LEARNERS</p>
            <h3 className="text-2xl font-black text-gray-900">{bookingSummary.uniqueLearners}</h3>
            <p className="text-xs text-indigo-600 font-medium mt-0.5">Distinct learner clients</p>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-xs flex items-center gap-4 hover:border-amber-200 transition-colors">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
            <Clock className="w-6 h-6"/>
          </div>
          <div>
            <p className="text-xs text-gray-500 font-semibold tracking-wider">UPCOMING SESSIONS</p>
            <h3 className="text-2xl font-black text-gray-900">{bookingSummary.upcomingSessions}</h3>
            <p className="text-xs text-amber-600 font-medium mt-0.5">Scheduled & active</p>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-xs flex items-center gap-4 hover:border-emerald-200 transition-colors">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
            <CheckCircle2 className="w-6 h-6"/>
          </div>
          <div>
            <p className="text-xs text-gray-500 font-semibold tracking-wider">COMPLETED SESSIONS</p>
            <h3 className="text-2xl font-black text-gray-900">{bookingSummary.completedSessions}</h3>
            <p className="text-xs text-emerald-600 font-medium mt-0.5">Completed successfully</p>
          </div>
        </div>
      </div>

      {/* Secondary Performance Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <div className="bg-white p-4 rounded-xl border border-emerald-100/80 shadow-2xs flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <DollarSign className="w-5 h-5"/>
            </div>
            <div>
              <p className="text-[11px] text-gray-500 font-semibold uppercase tracking-wider">Estimated Earnings</p>
              <h4 className="text-lg font-black text-gray-900">₹{Math.round(bookingSummary.totalEarnings || totalEarnings).toLocaleString('en-IN')}</h4>
            </div>
          </div>
          <span className="text-xs bg-emerald-50 text-emerald-700 font-bold px-2 py-0.5 rounded-full border border-emerald-100">
            80% Payout Rate
          </span>
        </div>

        <div className="bg-white p-4 rounded-xl border border-amber-100/80 shadow-2xs flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <Star className="w-5 h-5 fill-amber-400 text-amber-400"/>
            </div>
            <div>
              <p className="text-[11px] text-gray-500 font-semibold uppercase tracking-wider">Client Rating</p>
              <h4 className="text-lg font-black text-gray-900">{(mentorDetail?.averageRating || 5.0).toFixed(1)} / 5.0</h4>
            </div>
          </div>
          <span className="text-xs bg-amber-50 text-amber-800 font-bold px-2 py-0.5 rounded-full border border-amber-100">
            {mentorDetail?.totalReviews || 0} reviews
          </span>
        </div>

        <div className="bg-white p-4 rounded-xl border border-blue-100/80 shadow-2xs flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <Clock className="w-5 h-5"/>
            </div>
            <div>
              <p className="text-[11px] text-gray-500 font-semibold uppercase tracking-wider">Response Time</p>
              <h4 className="text-lg font-black text-gray-900">~{mentorDetail?.responseTimeMinutes || 15}m</h4>
            </div>
          </div>
          <span className="text-xs bg-blue-50 text-blue-700 font-bold px-2 py-0.5 rounded-full border border-blue-100">
            High Responsiveness
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: My Booked Learners, Requests & Upcoming Sessions */}
        <div className="lg:col-span-2 space-y-8">
          {/* Section: My Booked Learners */}
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
                    <Users className="w-5 h-5 text-indigo-600" />
                    My Booked Learners
                  </h2>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                    {bookedLearners.length} {bookedLearners.length === 1 ? 'learner' : 'learners'}
                  </span>
                </div>
                <p className="text-xs text-gray-500 mt-1">
                  Database records of learners who booked sessions with you, with profile pictures, contact details, and session status
                </p>
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchBookedLearners(learnerSearchQuery, learnerStatusFilter)}
                isLoading={loadingBookedLearners}
                className="flex items-center gap-1.5 text-xs text-gray-700 self-start sm:self-auto cursor-pointer"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loadingBookedLearners ? 'animate-spin' : ''}`} />
                <span>Refresh</span>
              </Button>
            </div>

            {/* Filter and Search Bar */}
            <div className="flex flex-col md:flex-row items-stretch md:items-center gap-3 mb-6 bg-gray-50/70 p-3 rounded-xl border border-gray-200/60">
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Search by learner name, email, or session title..."
                  value={learnerSearchQuery}
                  onChange={(e) => setLearnerSearchQuery(e.target.value)}
                  className="w-full text-xs pl-9 pr-7 py-2 bg-white rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                {learnerSearchQuery && (
                  <button
                    onClick={() => setLearnerSearchQuery('')}
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-xs cursor-pointer font-bold"
                  >
                    ×
                  </button>
                )}
              </div>

              {/* Status Filter Tabs */}
              <div className="flex items-center gap-1 overflow-x-auto pb-1 md:pb-0">
                {['ALL', 'CONFIRMED', 'PENDING', 'COMPLETED', 'CANCELLED'].map((status) => (
                  <button
                    key={status}
                    type="button"
                    onClick={() => setLearnerStatusFilter(status)}
                    className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors shrink-0 cursor-pointer ${
                      learnerStatusFilter === status
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
                    }`}
                  >
                    {status === 'ALL' ? 'All' : status.charAt(0) + status.slice(1).toLowerCase()}
                  </button>
                ))}
              </div>
            </div>

            {/* Error State */}
            {bookedLearnersError && (
              <div className="p-4 mb-4 text-xs bg-red-50 border border-red-200 text-red-800 rounded-xl flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
                  <span>{bookedLearnersError}</span>
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => fetchBookedLearners(learnerSearchQuery, learnerStatusFilter)}
                  className="text-xs text-red-700 border-red-300 hover:bg-red-100 shrink-0 cursor-pointer"
                >
                  Retry
                </Button>
              </div>
            )}

            {/* Loading State */}
            {loadingBookedLearners ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="p-4 rounded-xl border border-gray-100 bg-gray-50/50 animate-pulse flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-full bg-gray-200" />
                      <div className="space-y-2">
                        <div className="w-32 h-4 bg-gray-200 rounded" />
                        <div className="w-48 h-3 bg-gray-200 rounded" />
                        <div className="w-40 h-3 bg-gray-200 rounded" />
                      </div>
                    </div>
                    <div className="w-24 h-8 bg-gray-200 rounded-lg" />
                  </div>
                ))}
              </div>
            ) : bookedLearners.length === 0 ? (
              /* Empty State */
              <div className="text-center py-12 px-4 rounded-xl border border-dashed border-gray-200 bg-gray-50/50">
                <Users className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <h4 className="text-gray-900 font-bold text-sm">No booked learners found</h4>
                <p className="text-xs text-gray-500 mt-1 max-w-md mx-auto mb-4">
                  {learnerSearchQuery || learnerStatusFilter !== 'ALL'
                    ? 'No learners match your current search query or status filter. Try clearing the filters.'
                    : 'When learners book mentorship sessions with you, their database records, session details, and contact options will be displayed here.'}
                </p>
                {(learnerSearchQuery || learnerStatusFilter !== 'ALL') && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => {
                      setLearnerSearchQuery('');
                      setLearnerStatusFilter('ALL');
                    }}
                    className="text-xs cursor-pointer"
                  >
                    Clear Filters
                  </Button>
                )}
              </div>
            ) : (
              /* Database Records List */
              <div className="space-y-3">
                {bookedLearners.map((booking) => (
                  <div
                    key={booking.id}
                    className="p-4 rounded-xl border border-gray-100 hover:border-indigo-200 bg-gray-50/40 hover:bg-white transition-all flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-2xs"
                  >
                    {/* Learner Info & Avatar */}
                    <div className="flex items-start sm:items-center gap-3.5">
                      <UserAvatar
                        src={booking.learner?.profileImage}
                        name={booking.learner?.fullName || 'Learner'}
                        size="lg"
                        className="border-2 border-indigo-100 shrink-0"
                      />
                      <div className="space-y-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <h4 className="font-bold text-gray-900 text-sm">
                            {booking.learner?.fullName || 'Anonymous Learner'}
                          </h4>
                          <BookingBadge status={booking.status} />
                          <span className="text-[10px] font-mono bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded border border-slate-200">
                            {booking.bookingRef}
                          </span>
                        </div>

                        {/* Learner Email */}
                        <div className="flex items-center gap-1.5 text-xs text-gray-500">
                          <Mail className="w-3.5 h-3.5 text-indigo-500 shrink-0" />
                          {booking.learner?.email ? (
                            <a
                              href={`mailto:${booking.learner.email}?subject=Mentivo Mentorship Session: ${booking.sessionTitle || '1-on-1 Mentorship'} (Ref: ${booking.bookingRef})`}
                              className="text-indigo-600 hover:underline font-medium"
                              title="Click to email learner"
                            >
                              {booking.learner.email}
                            </a>
                          ) : (
                            <span className="text-gray-400 italic">Email unavailable</span>
                          )}
                        </div>

                        {/* Session Title */}
                        <div className="text-xs font-semibold text-slate-800 flex items-center gap-1">
                          <BookOpen className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                          <span>Session: {booking.sessionTitle || booking.topic?.name || '1-on-1 Mentorship'}</span>
                        </div>

                        {/* Date and Time */}
                        <p className="text-xs text-gray-500">
                          📅 {booking.bookingDate || 'Date TBD'} • ⏰ {booking.startTime ? booking.startTime.slice(0, 5) : '--:--'} - {booking.endTime ? booking.endTime.slice(0, 5) : '--:--'} ({booking.durationMinutes || 60} mins)
                          {booking.mentorEarnings !== undefined && booking.mentorEarnings !== null && (
                            <span className="ml-2 font-semibold text-emerald-600">• Earnings: ₹{booking.mentorEarnings}</span>
                          )}
                        </p>
                      </div>
                    </div>

                    {/* Actions on this Booking */}
                    <div className="flex items-center gap-2 self-end md:self-center shrink-0 flex-wrap">
                      {booking.learner?.email && (
                        <a
                          href={`mailto:${booking.learner.email}?subject=Mentivo Mentorship: ${booking.sessionTitle || 'Session'} (Ref: ${booking.bookingRef})&body=Hi ${booking.learner.fullName},%0D%0A%0D%0AI am reaching out regarding our mentorship session scheduled for ${booking.bookingDate}.%0D%0A%0D%0ABest regards,%0D%0A${user?.fullName || 'Your Mentor'}`}
                          title="Send email to learner"
                        >
                          <Button variant="outline" size="sm" className="flex items-center gap-1.5 text-xs text-slate-700 hover:text-indigo-600 cursor-pointer">
                            <Mail className="w-3.5 h-3.5 text-indigo-600" />
                            <span>Email</span>
                          </Button>
                        </a>
                      )}

                      {booking.learner?.id && (
                        <Link to={`/messages?user=${booking.learner.id}`} title="Chat with learner">
                          <Button variant="outline" size="sm" className="text-xs cursor-pointer">
                            <MessageSquare className="w-3.5 h-3.5" />
                          </Button>
                        </Link>
                      )}

                      {booking.status === 'PENDING' && (
                        <>
                          <Button
                            size="sm"
                            variant="success"
                            onClick={() => handleBookingAction(booking.id, 'CONFIRMED')}
                            className="flex items-center gap-1 text-xs cursor-pointer"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5" /> Accept
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleBookingAction(booking.id, 'CANCELLED')}
                            className="text-red-600 hover:bg-red-50 border-red-200 text-xs cursor-pointer"
                          >
                            <XCircle className="w-3.5 h-3.5" /> Decline
                          </Button>
                        </>
                      )}

                      {(booking.status === 'CONFIRMED' || booking.status === 'IN_PROGRESS') && (
                        <Button
                          size="sm"
                          onClick={() => handleStartSessionCall(booking)}
                          className="flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-700 text-xs text-white cursor-pointer shadow-xs"
                        >
                          <Video className="w-3.5 h-3.5" /> Start Call
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
          {/* Pending Booking Requests */}
          {pendingBookings.length > 0 && (<div className="bg-amber-50/60 rounded-2xl p-6 border border-amber-200/80">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-5 h-5 text-amber-600"/>
                  <h2 className="text-lg font-bold text-gray-900">
                    Pending Booking Requests ({pendingBookings.length})
                  </h2>
                </div>
              </div>

              <div className="space-y-3">
                {pendingBookings.map((booking) => (<div key={booking.id} className="bg-white p-4 rounded-xl border border-amber-100 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <img src={booking.learner.profileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${booking.learner.fullName}`} alt={booking.learner.fullName} className="w-11 h-11 rounded-full object-cover"/>
                      <div>
                        <h4 className="font-semibold text-gray-900">{booking.learner.fullName}</h4>
                        <p className="text-xs text-gray-500">
                          {booking.bookingDate} • {booking.startTime.slice(0, 5)} - {booking.endTime.slice(0, 5)} ({booking.durationMinutes} mins)
                        </p>
                        <p className="text-xs font-semibold text-emerald-600 mt-0.5">
                          Earnings: ₹{booking.mentorEarnings}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 self-end sm:self-center">
                      {booking.learner?.email && (
                        <a
                          href={`mailto:${booking.learner.email}?subject=Mentivo Session Booking: ${booking.topic?.name || '1-on-1 Mentorship'} (Ref: ${booking.bookingRef})&body=Hi ${booking.learner.fullName},%0D%0A%0D%0AI am contacting you regarding your mentorship booking request for ${booking.bookingDate}.%0D%0A%0D%0ABest regards,%0D%0A${user?.fullName || 'Your Mentor'}`}
                          title="Contact learner via your default email application"
                        >
                          <Button variant="outline" size="sm" className="flex items-center gap-1.5 text-xs text-slate-700 hover:text-indigo-600">
                            <Mail className="w-3.5 h-3.5 text-indigo-600" />
                            <span>Email Learner</span>
                          </Button>
                        </a>
                      )}
                      <Button size="sm" variant="success" onClick={() => handleBookingAction(booking.id, 'CONFIRMED')} className="flex items-center gap-1">
                        <CheckCircle2 className="w-4 h-4"/> Accept
                      </Button>
                      <Button size="sm" variant="outline" onClick={() => handleBookingAction(booking.id, 'CANCELLED')} className="text-red-600 hover:bg-red-50 border-red-200">
                        <XCircle className="w-4 h-4"/> Decline
                      </Button>
                    </div>
                  </div>))}
              </div>
            </div>)}

          {/* Upcoming Confirmed Sessions */}
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <Calendar className="w-5 h-5 text-indigo-600"/>
                Upcoming Sessions ({upcomingBookings.length})
              </h2>
              <Link to="/sessions" className="text-xs font-medium text-indigo-600 hover:text-indigo-800 flex items-center gap-1">
                View all <ArrowUpRight className="w-3.5 h-3.5"/>
              </Link>
            </div>

            {upcomingBookings.length === 0 ? (<div className="text-center py-10">
                <Calendar className="w-12 h-12 text-gray-300 mx-auto mb-3"/>
                <h4 className="text-gray-900 font-medium">No upcoming sessions</h4>
                <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
                  When learners book your available time slots, they will appear here. Keep your status online to gain visibility!
                </p>
              </div>) : (<div className="space-y-4">
                {upcomingBookings.map((booking) => (<div key={booking.id} className="p-4 rounded-xl border border-gray-100 hover:border-indigo-100 transition-colors bg-gray-50/50 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <img src={booking.learner.profileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${booking.learner.fullName}`} alt={booking.learner.fullName} className="w-12 h-12 rounded-full object-cover"/>
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="font-semibold text-gray-900">{booking.learner.fullName}</h4>
                          <span className="text-xs bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full font-medium">
                            {booking.topic?.name || '1-on-1 Mentorship'}
                          </span>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">
                          📅 {booking.bookingDate} • ⏰ {booking.startTime.slice(0, 5)} - {booking.endTime.slice(0, 5)} ({booking.durationMinutes}m)
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      {booking.learner?.email && (
                        <a
                          href={`mailto:${booking.learner.email}?subject=Mentivo Mentorship Session: ${booking.topic?.name || '1-on-1 Mentorship'} (Ref: ${booking.bookingRef})&body=Hi ${booking.learner.fullName},%0D%0A%0D%0AI am writing regarding our mentorship session scheduled for ${booking.bookingDate}.%0D%0A%0D%0ABest regards,%0D%0A${user?.fullName || 'Your Mentor'}`}
                          title="Contact learner via your default email application"
                        >
                          <Button variant="outline" size="sm" className="flex items-center gap-1.5 text-xs text-slate-700 hover:text-indigo-600">
                            <Mail className="w-3.5 h-3.5 text-indigo-600" />
                            <span>Email Learner</span>
                          </Button>
                        </a>
                      )}
                      <Link to={`/messages?user=${booking.learner.id}`}>
                        <Button variant="outline" size="sm">
                          <MessageSquare className="w-4 h-4"/>
                        </Button>
                      </Link>
                      {(booking.status === 'CONFIRMED' || booking.status === 'IN_PROGRESS') && (
                        <Button
                          size="sm"
                          onClick={() => handleStartSessionCall(booking)}
                          className="flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-700 text-xs text-white cursor-pointer shadow-xs"
                        >
                          <Video className="w-4 h-4"/> Start Call
                        </Button>
                      )}
                    </div>
                  </div>))}
              </div>)}
          </div>

          {/* My Published Sessions & Webinars (Prompt Requirement #2) */}
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-xs">
            <div className="flex items-center justify-between mb-6 flex-wrap gap-2">
              <div>
                <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <BookOpen className="w-5 h-5 text-indigo-600"/>
                  My Published Sessions & Webinars ({mySessions.length})
                </h2>
                <p className="text-xs text-gray-500 mt-0.5">
                  Live sessions, webinars, and workshops published to the Mentivo discovery catalog
                </p>
              </div>
              <Button
                size="sm"
                variant="primary"
                onClick={handleOpenCreateSession}
                className="flex items-center gap-1.5 text-xs shadow-xs"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Publish New Session</span>
              </Button>
            </div>

            {mySessions.length === 0 ? (
              <div className="text-center py-10 border border-dashed border-gray-200 rounded-xl bg-gray-50/50">
                <Calendar className="w-12 h-12 text-gray-300 mx-auto mb-3"/>
                <h4 className="text-gray-900 font-semibold text-sm">No published sessions yet</h4>
                <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto mb-4">
                  Publish individual sessions, group workshops, or free webinars to attract learners across your categories!
                </p>
                <Button size="sm" variant="outline" onClick={handleOpenCreateSession} className="text-xs">
                  <Plus className="w-3.5 h-3.5 mr-1" /> Publish Your First Session
                </Button>
              </div>
            ) : (
              <div className="space-y-3">
                {mySessions.map((session) => (
                  <div
                    key={session.id}
                    className="p-4 rounded-xl border border-gray-100 hover:border-indigo-100 bg-gray-50/40 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-colors"
                  >
                    <div className="space-y-1.5">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-xs bg-indigo-50 text-indigo-700 px-2.5 py-0.5 rounded-full font-bold">
                          {session.sessionType === 'WEBINAR' ? 'Webinar' : session.sessionType === 'GROUP_SESSION' ? 'Group Session' : '1-on-1 Session'}
                        </span>
                        {session.categoryName && (
                          <span className="text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded-full font-medium">
                            {session.categoryName}
                          </span>
                        )}
                        <span className={`text-xs px-2 py-0.5 rounded-full font-bold ${
                          session.isFree || Number(session.price) === 0
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-amber-50 text-amber-800 border border-amber-200'
                        }`}>
                          {session.isFree || Number(session.price) === 0 ? 'Free' : `₹${session.price}`}
                        </span>
                      </div>
                      <h4 className="font-bold text-gray-900 text-sm">{session.title}</h4>
                      {session.description && (
                        <p className="text-xs text-gray-500 line-clamp-1">{session.description}</p>
                      )}
                      <p className="text-xs text-gray-400">
                        📅 {session.sessionDate} • ⏰ {session.startTime ? session.startTime.slice(0, 5) : ''} ({session.durationMinutes} mins)
                        {session.externalMeetingUrl && ' • 🌐 External Link Set'}
                      </p>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-center">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOpenEditSession(session)}
                        className="text-xs text-slate-700 hover:text-indigo-600 border-slate-200"
                      >
                        <Edit2 className="w-3.5 h-3.5 mr-1" /> Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDeleteSession(session.id)}
                        className="text-xs text-rose-600 hover:bg-rose-50 border-rose-200"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Recent Reviews Received */}
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-xs">
            <h2 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
              <Star className="w-5 h-5 text-amber-500 fill-amber-500"/>
              Recent Learner Reviews ({reviews.length})
            </h2>

            {reviews.length === 0 ? (<p className="text-sm text-gray-500 py-4 text-center">
                You haven't received any reviews yet. Complete your first session to start collecting ratings!
              </p>) : (<div className="space-y-4">
                {reviews.slice(0, 5).map((rev) => (<div key={rev.id} className="p-4 rounded-xl border border-gray-50 bg-gray-50/30">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-sm text-gray-900">{rev.learner?.fullName || 'Anonymous Learner'}</span>
                        <span className="text-xs text-gray-400">• {new Date(rev.createdAt).toLocaleDateString()}</span>
                      </div>
                      <div className="flex items-center text-amber-400">
                        {Array.from({ length: 5 }).map((_, i) => (<Star key={i} className={`w-4 h-4 ${i < rev.rating ? 'fill-amber-400' : 'text-gray-200 fill-gray-200'}`}/>))}
                      </div>
                    </div>
                    <p className="text-xs text-gray-600 italic">"{rev.reviewText}"</p>
                  </div>))}
              </div>)}
          </div>
        </div>

        {/* Right Column: Profile Snapshot & Quick Actions */}
        <div className="space-y-6">
          {/* Quick Actions Card */}
          <div className="bg-gradient-to-br from-indigo-900 to-indigo-800 text-white rounded-2xl p-6 shadow-md">
            <h3 className="text-lg font-bold mb-2">Grow Your Mentorship</h3>
            <p className="text-indigo-200 text-xs mb-6">
              Learners seek mentors with rich portfolios and fast response rates.
            </p>
            <div className="space-y-2.5">
              <Link to="/connections" className="block">
                <Button variant="secondary" className="w-full justify-start text-xs bg-white/10 hover:bg-white/20 text-white border-0">
                  <Users className="w-4 h-4 mr-2"/> View Connections
                </Button>
              </Link>
              <Link to="/messages" className="block">
                <Button variant="secondary" className="w-full justify-start text-xs bg-white/10 hover:bg-white/20 text-white border-0">
                  <MessageSquare className="w-4 h-4 mr-2"/> Open Chat Inbox
                </Button>
              </Link>
              <Link to={`/mentors/${mentorDetail?.id}`} className="block">
                <Button variant="secondary" className="w-full justify-start text-xs bg-white/10 hover:bg-white/20 text-white border-0">
                  <ArrowUpRight className="w-4 h-4 mr-2"/> Preview Public Profile
                </Button>
              </Link>
            </div>
          </div>

          {/* Social Media & Audience Showcase Card */}
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-xs space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div className="flex items-center gap-2">
                <Globe className="w-5 h-5 text-indigo-600"/>
                <h3 className="text-base font-bold text-gray-900">
                  Social Media & Showcase
                </h3>
              </div>
              <span className="text-[11px] font-semibold text-indigo-700 bg-indigo-50 border border-indigo-100 rounded-full px-2.5 py-0.5">
                {mentorDetail?.learnerFollowerCount ?? 0} Followers
              </span>
            </div>

            <p className="text-xs text-gray-500 leading-relaxed">
              Showcase your external YouTube channel and Instagram profile on your public mentor profile. Choose whether to showcase self-reported follower counts.
            </p>

            {socialSuccessMessage && (
              <div className="p-3 text-xs bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0"/>
                <span>{socialSuccessMessage}</span>
              </div>
            )}

            {socialErrorMessage && (
              <div className="p-3 text-xs bg-red-50 border border-red-200 text-red-800 rounded-xl flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-red-600 shrink-0"/>
                <span>{socialErrorMessage}</span>
              </div>
            )}

            <form onSubmit={handleSaveSocialSettings} className="space-y-4">
              {/* YouTube Section */}
              <div className="p-3.5 rounded-xl border border-gray-100 bg-gray-50/50 space-y-3">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-gray-800 flex items-center gap-1.5">
                    <svg className="w-4 h-4 text-red-600 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
                    </svg>
                    YouTube Channel URL
                  </label>
                  {youtubeUrl && (
                    <a href={youtubeUrl.startsWith('http') ? youtubeUrl : `https://${youtubeUrl}`} target="_blank" rel="noopener noreferrer" className="text-[11px] text-red-600 hover:underline flex items-center gap-0.5">
                      Preview <ExternalLink className="w-2.5 h-2.5"/>
                    </a>
                  )}
                </div>
                <input
                  type="text"
                  value={youtubeUrl}
                  onChange={(e) => setYoutubeUrl(e.target.value)}
                  placeholder="https://youtube.com/@yourchannel"
                  className="w-full text-xs px-3 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                />

                {/* YouTube Subscribers ON/OFF toggle */}
                <div className="flex items-center justify-between pt-1">
                  <span className="text-xs font-medium text-gray-700">
                    Show YouTube Subscriber Count
                  </span>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={showYoutubeSubscribers}
                      onChange={(e) => setShowYoutubeSubscribers(e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-red-600"></div>
                  </label>
                </div>

                {showYoutubeSubscribers && (
                  <div className="pt-1.5 space-y-1">
                    <label className="text-[11px] font-medium text-gray-600 block">
                      Subscribers Count (Self-reported)
                    </label>
                    <input
                      type="number"
                      min="0"
                      value={youtubeSubscribersCount}
                      onChange={(e) => setYoutubeSubscribersCount(e.target.value)}
                      placeholder="e.g. 2500"
                      className="w-full text-xs px-3 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    />
                    <p className="text-[10px] text-gray-400">
                      Labeled as "(Self-reported)" on public profile.
                    </p>
                  </div>
                )}
              </div>

              {/* Instagram Section */}
              <div className="p-3.5 rounded-xl border border-gray-100 bg-gray-50/50 space-y-3">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-gray-800 flex items-center gap-1.5">
                    <svg className="w-4 h-4 text-pink-600 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/>
                    </svg>
                    Instagram Profile URL
                  </label>
                  {instagramUrl && (
                    <a href={instagramUrl.startsWith('http') ? instagramUrl : `https://${instagramUrl}`} target="_blank" rel="noopener noreferrer" className="text-[11px] text-pink-600 hover:underline flex items-center gap-0.5">
                      Preview <ExternalLink className="w-2.5 h-2.5"/>
                    </a>
                  )}
                </div>
                <input
                  type="text"
                  value={instagramUrl}
                  onChange={(e) => setInstagramUrl(e.target.value)}
                  placeholder="https://instagram.com/yourprofile"
                  className="w-full text-xs px-3 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                />

                {/* Instagram Followers ON/OFF toggle */}
                <div className="flex items-center justify-between pt-1">
                  <span className="text-xs font-medium text-gray-700">
                    Show Instagram Follower Count
                  </span>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={showInstagramFollowers}
                      onChange={(e) => setShowInstagramFollowers(e.target.checked)}
                      className="sr-only peer"
                    />
                    <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-pink-600"></div>
                  </label>
                </div>

                {showInstagramFollowers && (
                  <div className="pt-1.5 space-y-1">
                    <label className="text-[11px] font-medium text-gray-600 block">
                      Followers Count (Self-reported)
                    </label>
                    <input
                      type="number"
                      min="0"
                      value={instagramFollowersCount}
                      onChange={(e) => setInstagramFollowersCount(e.target.value)}
                      placeholder="e.g. 1800"
                      className="w-full text-xs px-3 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    />
                    <p className="text-[10px] text-gray-400">
                      Labeled as "(Self-reported)" on public profile.
                    </p>
                  </div>
                )}
              </div>

              <Button
                type="submit"
                variant="primary"
                size="sm"
                isLoading={savingSocial}
                className="w-full justify-center gap-1.5 text-xs font-semibold"
              >
                <Save className="w-3.5 h-3.5"/>
                <span>Save Showcase Settings</span>
              </Button>
            </form>
          </div>

          {/* Active Categories, Skills & Specializations (Prompt Requirement #1) */}
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-xs space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-gray-100">
              <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider flex items-center gap-1.5">
                <Layers className="w-4 h-4 text-indigo-600"/>
                <span>Your Categories</span>
              </h3>
              <Link to="/profile" className="text-xs text-indigo-600 hover:underline font-semibold">
                Edit
              </Link>
            </div>

            <div className="flex flex-wrap gap-1.5">
              {mentorDetail?.categories && mentorDetail.categories.length > 0 ? (
                mentorDetail.categories.map((c) => (
                  <span key={c.id} className="text-xs bg-indigo-50 text-indigo-700 border border-indigo-100 px-2.5 py-1 rounded-lg font-semibold">
                    {c.name}
                  </span>
                ))
              ) : (
                <p className="text-xs text-gray-400">
                  No direct categories selected yet. <Link to="/profile" className="text-indigo-600 underline">Add categories</Link> to help learners find you.
                </p>
              )}
            </div>

            <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider pt-2 border-t border-gray-100 mb-2">
              Listed Skills
            </h3>
            <div className="flex flex-wrap gap-1.5 mb-2">
              {mentorDetail?.skills && mentorDetail.skills.length > 0 ? (
                mentorDetail.skills.map((s) => (
                  <span key={s.id} className="text-xs bg-slate-100 text-slate-700 px-2.5 py-1 rounded-md font-medium">
                    {s.name}
                  </span>
                ))
              ) : (
                <p className="text-xs text-gray-400">No skills listed yet.</p>
              )}
            </div>

            <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider pt-2 border-t border-gray-100 mb-2">
              Weekly Availability
            </h3>
            <div className="space-y-2">
              {mentorDetail?.availabilities && mentorDetail.availabilities.length > 0 ? (
                mentorDetail.availabilities.map((slot, idx) => (
                  <div key={idx} className="flex justify-between items-center text-xs py-1.5 border-b border-gray-50 last:border-0">
                    <span className="font-semibold text-gray-800">{slot.dayOfWeek}</span>
                    <span className="text-gray-500 font-mono">{slot.startTime.slice(0, 5)} - {slot.endTime.slice(0, 5)}</span>
                  </div>
                ))
              ) : (
                <div className="text-xs text-gray-400 py-2">
                  Default schedule: Mon-Fri, 10:00 - 19:00
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Session Publish / Edit Modal (Prompt Requirement #2 & #7) */}
      {showSessionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl space-y-5 relative max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setShowSessionModal(false)}
              className="absolute top-5 right-5 p-2 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <div>
              <h2 className="text-xl font-black text-slate-900">
                {editingSessionId ? 'Edit Session or Webinar' : 'Publish New Session or Webinar'}
              </h2>
              <p className="text-xs text-slate-500 mt-1">
                Add an upcoming live individual session, group workshop, or public webinar.
              </p>
            </div>

            {sessionError && (
              <div className="p-3 text-xs bg-rose-50 border border-rose-200 text-rose-800 rounded-xl flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{sessionError}</span>
              </div>
            )}

            <form onSubmit={handleSaveSession} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Session / Webinar Title *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Masterclass in Clean Architecture & Concurrency"
                  value={sessionForm.title}
                  onChange={(e) => setSessionForm({ ...sessionForm, title: e.target.value })}
                  className="w-full text-xs px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Short Description
                </label>
                <textarea
                  rows={3}
                  placeholder="What will learners discover and gain from this session?"
                  value={sessionForm.description}
                  onChange={(e) => setSessionForm({ ...sessionForm, description: e.target.value })}
                  className="w-full text-xs px-3.5 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Category *
                  </label>
                  <select
                    required
                    value={sessionForm.categoryId}
                    onChange={(e) => setSessionForm({ ...sessionForm, categoryId: e.target.value, topicId: '' })}
                    className="w-full text-xs px-3 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                  >
                    <option value="">Select Category</option>
                    {categoriesList.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Session Type *
                  </label>
                  <select
                    value={sessionForm.sessionType}
                    onChange={(e) => setSessionForm({ ...sessionForm, sessionType: e.target.value })}
                    className="w-full text-xs px-3 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                  >
                    <option value="WEBINAR">Webinar</option>
                    <option value="GROUP_SESSION">Group Session</option>
                    <option value="INDIVIDUAL_SESSION">Individual Session</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Date *
                  </label>
                  <input
                    type="date"
                    required
                    value={sessionForm.sessionDate}
                    onChange={(e) => setSessionForm({ ...sessionForm, sessionDate: e.target.value })}
                    className="w-full text-xs px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Start Time *
                  </label>
                  <input
                    type="time"
                    required
                    value={sessionForm.startTime}
                    onChange={(e) => setSessionForm({ ...sessionForm, startTime: e.target.value })}
                    className="w-full text-xs px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Duration (Minutes)
                  </label>
                  <input
                    type="number"
                    min="15"
                    max="240"
                    step="15"
                    value={sessionForm.durationMinutes}
                    onChange={(e) => setSessionForm({ ...sessionForm, durationMinutes: e.target.value })}
                    className="w-full text-xs px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                  />
                </div>
              </div>

              {/* Pricing section */}
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80 space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-800">
                    Pricing Mode
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={sessionForm.isFree}
                      onChange={(e) => setSessionForm({
                        ...sessionForm,
                        isFree: e.target.checked,
                        price: e.target.checked ? '0' : sessionForm.price
                      })}
                      className="rounded text-indigo-600 focus:ring-indigo-500"
                    />
                    <span className="text-xs font-bold text-emerald-700">Free Session</span>
                  </label>
                </div>

                {!sessionForm.isFree && (
                  <div className="pt-2">
                    <label className="block text-[11px] font-medium text-slate-600 mb-1">
                      Price per Attendee (INR ₹)
                    </label>
                    <input
                      type="number"
                      min="1"
                      step="1"
                      placeholder="e.g. 199"
                      value={sessionForm.price}
                      onChange={(e) => setSessionForm({ ...sessionForm, price: e.target.value })}
                      className="w-full text-xs px-3 py-2 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    />
                  </div>
                )}
              </div>

              {/* Meeting Link & Zoho Automation Coming Soon (Prompt Requirement #7) */}
              <div className="space-y-2">
                <label className="block text-xs font-semibold text-slate-700">
                  External Meeting or Event Link (Optional)
                </label>
                <input
                  type="url"
                  placeholder="e.g. https://meet.google.com/abc-xyz or Zoom / webinar link"
                  value={sessionForm.externalMeetingUrl}
                  onChange={(e) => setSessionForm({ ...sessionForm, externalMeetingUrl: e.target.value })}
                  className="w-full text-xs px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                />

                <div className="p-3 bg-indigo-50/60 rounded-xl border border-indigo-100 flex items-center justify-between gap-3 text-xs">
                  <div>
                    <p className="font-bold text-slate-800">Zoho Meeting Platform</p>
                    <p className="text-[11px] text-slate-500">Automated 1-click meeting creation with Zoho will arrive in a future update.</p>
                  </div>
                  <button
                    type="button"
                    disabled
                    className="px-3 py-1.5 rounded-lg bg-white border border-slate-200 text-slate-400 text-xs font-semibold cursor-not-allowed shadow-2xs shrink-0"
                    title="Automated Zoho Meeting integration is planned for a future platform update"
                  >
                    Automated Zoho Meeting — Coming Soon
                  </button>
                </div>
              </div>

              {/* Action buttons */}
              <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setShowSessionModal(false)}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  isLoading={savingSession}
                  className="px-5 text-xs font-semibold"
                >
                  {editingSessionId ? 'Update Session' : 'Publish Session'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>);
};
export default MentorDashboardPage;
