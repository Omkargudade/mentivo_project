import React, { useState, useEffect } from 'react';
import { Link, useSearchParams, useLocation } from 'react-router-dom';
import {
  Calendar,
  Clock,
  Video,
  Star,
  Search,
  MessageSquare,
  Users,
  Sparkles,
  Tag,
  ExternalLink,
  Info,
  CheckCircle2,
  X,
  SlidersHorizontal,
  RotateCcw,
  BookOpen,
  Layers,
  ArrowRight
} from 'lucide-react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useCall } from '../context/CallContext';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import ReviewModal from '../components/review/ReviewModal';
import UserAvatar from '../components/common/UserAvatar';
import SessionBookingModal from '../components/booking/SessionBookingModal';

export const SessionsPage = ({ defaultTab }) => {
  const { user, currentMode } = useAuth();
  const { startCall } = useCall();
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();

  const handleStartSessionCall = (booking) => {
    const isMentor = currentMode === 'MENTOR';
    const targetUserId = isMentor
      ? (booking.learnerUserId || booking.learner?.id)
      : (booking.mentorUserId || booking.mentor?.userId || booking.mentor?.user?.id);
    const targetName = isMentor
      ? (booking.learner?.fullName || 'Learner')
      : (booking.mentor?.name || booking.mentor?.user?.fullName || 'Mentor');
    const targetImage = isMentor
      ? booking.learner?.profileImage
      : (booking.mentor?.profileImage || booking.mentor?.user?.profileImage);

    if (!targetUserId) {
      alert('Participant details are unavailable for this session call.');
      return;
    }

    const sessionTitle = booking.sessionTitle || booking.topic?.name || 'Mentorship Session';
    startCall(targetUserId, targetName, targetImage, {
      bookingId: booking.id,
      sessionId: booking.sessionId,
      sessionTitle
    });
  };

  const isBookingsPath = defaultTab === 'BOOKINGS' ||
    location.pathname === '/bookings' ||
    location.pathname === '/my-bookings' ||
    searchParams.get('tab') === 'bookings';

  // Active Tab: 'EXPLORE' or 'BOOKINGS'
  const [activeTab, setActiveTab] = useState(isBookingsPath ? 'BOOKINGS' : 'EXPLORE');

  // Explore Sessions State
  const [sessions, setSessions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [selectedSessionModal, setSelectedSessionModal] = useState(null);
  const [selectedBookingSession, setSelectedBookingSession] = useState(null);

  const handleBookingSuccess = (booking) => {
    fetchSessions();
    setActiveTab('BOOKINGS');
    setSearchParams({ tab: 'bookings' });
    fetchBookings();
  };

  // Explore Filters State
  const [searchQuery, setSearchQuery] = useState(searchParams.get('query') || '');
  const [debouncedQuery, setDebouncedQuery] = useState(searchQuery);
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('categoryId') || '');
  const [selectedType, setSelectedType] = useState(searchParams.get('type') || '');
  const [priceFilter, setPriceFilter] = useState(searchParams.get('pricing') || 'ALL'); // 'ALL', 'FREE', 'PAID'
  const [upcomingOnly, setUpcomingOnly] = useState(true);

  // 1-on-1 Bookings State (Preserved existing feature)
  const [bookings, setBookings] = useState([]);
  const [bookingFilterStatus, setBookingFilterStatus] = useState('ALL');
  const [bookingSearchQuery, setBookingSearchQuery] = useState('');
  const [loadingBookings, setLoadingBookings] = useState(false);
  const [selectedReviewBooking, setSelectedReviewBooking] = useState(null);
  const [cancellingBookingId, setCancellingBookingId] = useState(null);

  // Sync tab with searchParams and location if needed
  useEffect(() => {
    const tabParam = searchParams.get('tab');
    const isBookingsTarget = tabParam === 'bookings' ||
      location.pathname === '/bookings' ||
      location.pathname === '/my-bookings' ||
      defaultTab === 'BOOKINGS';

    if (isBookingsTarget && activeTab !== 'BOOKINGS') {
      setActiveTab('BOOKINGS');
    }
    const catParam = searchParams.get('categoryId');
    if (catParam && catParam !== selectedCategory) {
      setSelectedCategory(catParam);
    }
  }, [searchParams, location.pathname, defaultTab]);

  // Debounce search query for sessions
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(searchQuery);
    }, 350);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Load Categories on mount
  useEffect(() => {
    api.get('/categories')
      .then((res) => {
        if (res.data?.success && res.data.data) {
          setCategories(res.data.data);
        }
      })
      .catch((err) => console.error('Failed to load categories', err));
  }, []);

  // Fetch Sessions when explore filters change
  useEffect(() => {
    if (activeTab === 'EXPLORE') {
      fetchSessions();
    }
  }, [activeTab, debouncedQuery, selectedCategory, selectedType, priceFilter, upcomingOnly]);

  // Fetch Bookings when tab is BOOKINGS or mode changes
  useEffect(() => {
    if (activeTab === 'BOOKINGS' && user) {
      fetchBookings();
    }
  }, [activeTab, currentMode, user]);

  const fetchSessions = async () => {
    setLoadingSessions(true);
    try {
      const params = new URLSearchParams();
      if (debouncedQuery.trim()) params.set('query', debouncedQuery.trim());
      if (selectedCategory) params.set('categoryId', selectedCategory);
      if (selectedType) params.set('sessionType', selectedType);
      if (priceFilter === 'FREE') params.set('isFree', 'true');
      if (priceFilter === 'PAID') params.set('isFree', 'false');
      if (upcomingOnly) params.set('upcomingOnly', 'true');
      params.set('size', '50');

      const res = await api.get(`/sessions?${params.toString()}`);
      if (res.data?.success && res.data.data) {
        setSessions(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load sessions', err);
    } finally {
      setLoadingSessions(false);
    }
  };

  const fetchBookings = async () => {
    try {
      setLoadingBookings(true);
      const endpoint = currentMode === 'MENTOR' ? '/bookings/mentor' : '/bookings/learner';
      const res = await api.get(endpoint);
      if (res.data?.success && res.data.data) {
        setBookings(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load bookings', err);
    } finally {
      setLoadingBookings(false);
    }
  };

  const handleClearFilters = () => {
    setSearchQuery('');
    setDebouncedQuery('');
    setSelectedCategory('');
    setSelectedType('');
    setPriceFilter('ALL');
    setUpcomingOnly(true);
  };

  const handleCancelBooking = async (bookingId) => {
    const reason = window.prompt('Please enter a reason for cancellation:');
    if (!reason) return;
    try {
      setCancellingBookingId(bookingId);
      await api.patch(`/bookings/${bookingId}/cancel`, { reason });
      fetchBookings();
    } catch (err) {
      console.error('Failed to cancel booking', err);
      alert('Failed to cancel booking. Please try again.');
    } finally {
      setCancellingBookingId(null);
    }
  };

  const filteredBookings = bookings.filter((b) => {
    const matchesStatus = bookingFilterStatus === 'ALL' || b.status === bookingFilterStatus;
    const peerName = currentMode === 'MENTOR' ? b.learner?.fullName : b.mentor?.name;
    const topicName = b.topic?.name || '';
    const sessionTitle = b.sessionTitle || '';
    const matchesSearch =
      (peerName || '').toLowerCase().includes(bookingSearchQuery.toLowerCase()) ||
      topicName.toLowerCase().includes(bookingSearchQuery.toLowerCase()) ||
      sessionTitle.toLowerCase().includes(bookingSearchQuery.toLowerCase()) ||
      (b.bookingRef || '').toLowerCase().includes(bookingSearchQuery.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  const getStatusBadgeVariant = (status) => {
    switch (status) {
      case 'CONFIRMED': return 'success';
      case 'IN_PROGRESS': return 'warning';
      case 'COMPLETED': return 'primary';
      case 'CANCELLED': return 'danger';
      case 'PENDING': return 'default';
      default: return 'default';
    }
  };

  const formatSessionType = (type) => {
    switch (type) {
      case 'WEBINAR': return 'Webinar';
      case 'GROUP_SESSION': return 'Group Session';
      case 'INDIVIDUAL_SESSION': return '1-on-1 Session';
      default: return type ? type.replace('_', ' ') : 'Session';
    }
  };

  const hasActiveFilters = debouncedQuery || selectedCategory || selectedType || priceFilter !== 'ALL' || !upcomingOnly;

  // Split sessions into sections when not searching with custom terms
  const upcomingSessionsList = sessions.filter(s => s.sessionDate);
  const freeSessionsList = sessions.filter(s => s.isFree || Number(s.price) === 0);
  const interestSessionsList = selectedCategory
    ? sessions.filter(s => String(s.categoryId) === String(selectedCategory))
    : sessions.slice(0, 4);

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Top Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-semibold mb-2">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Mentivo Sessions & Events</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              Live Mentorship Sessions & Webinars
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">
              Join interactive webinars, small group workshops, and personalized 1-on-1 sessions led by industry practitioners.
            </p>
          </div>

          {/* Mode-aware quick actions */}
          <div className="flex items-center gap-2">
            {currentMode === 'MENTOR' ? (
              <Link to="/mentor/dashboard">
                <Button variant="primary" size="sm" className="flex items-center gap-1.5 text-xs">
                  <Calendar className="w-3.5 h-3.5" />
                  <span>Mentor Dashboard</span>
                </Button>
              </Link>
            ) : (
              <Link to="/mentors">
                <Button variant="outline" size="sm" className="flex items-center gap-1.5 text-xs">
                  <Users className="w-3.5 h-3.5 text-indigo-600" />
                  <span>Browse All Mentors</span>
                </Button>
              </Link>
            )}
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-2 border-b border-slate-200">
          <button
            onClick={() => setActiveTab('EXPLORE')}
            className={`pb-3 px-3 text-sm font-bold border-b-2 transition-all cursor-pointer flex items-center gap-2 ${
              activeTab === 'EXPLORE'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Calendar className="w-4 h-4" />
            <span>Explore Sessions & Webinars</span>
            <span className="px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 text-[10px] font-bold">
              {sessions.length}
            </span>
          </button>

          {user && (
            <button
              onClick={() => setActiveTab('BOOKINGS')}
              className={`pb-3 px-3 text-sm font-bold border-b-2 transition-all cursor-pointer flex items-center gap-2 ${
                activeTab === 'BOOKINGS'
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <Clock className="w-4 h-4" />
              <span>{currentMode === 'MENTOR' ? 'My Mentoring Bookings' : 'My 1-on-1 Bookings'}</span>
              {bookings.length > 0 && (
                <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 text-[10px] font-bold">
                  {bookings.length}
                </span>
              )}
            </button>
          )}
        </div>

        {/* TAB 1: EXPLORE SESSIONS & WEBINARS */}
        {activeTab === 'EXPLORE' && (
          <div className="space-y-6">
            {/* Search and Filters Bar */}
            <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-xs space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-12 gap-3">
                {/* Search Input */}
                <div className="md:col-span-5 relative">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search by session title, topic, mentor, or description..."
                    className="w-full text-xs pl-10 pr-4 py-2.5 bg-slate-50/50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white text-slate-800 transition-all"
                  />
                  {searchQuery && (
                    <button
                      onClick={() => setSearchQuery('')}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 text-xs"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>

                {/* Category Dropdown */}
                <div className="md:col-span-3">
                  <select
                    value={selectedCategory}
                    onChange={(e) => setSelectedCategory(e.target.value)}
                    className="w-full text-xs px-3 py-2.5 bg-slate-50/50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white text-slate-800"
                  >
                    <option value="">All Categories</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Session Type Dropdown */}
                <div className="md:col-span-2">
                  <select
                    value={selectedType}
                    onChange={(e) => setSelectedType(e.target.value)}
                    className="w-full text-xs px-3 py-2.5 bg-slate-50/50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white text-slate-800"
                  >
                    <option value="">All Types</option>
                    <option value="WEBINAR">Webinar</option>
                    <option value="GROUP_SESSION">Group Session</option>
                    <option value="INDIVIDUAL_SESSION">1-on-1 Session</option>
                  </select>
                </div>

                {/* Free / Paid Toggle */}
                <div className="md:col-span-2">
                  <select
                    value={priceFilter}
                    onChange={(e) => setPriceFilter(e.target.value)}
                    className="w-full text-xs px-3 py-2.5 bg-slate-50/50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white text-slate-800"
                  >
                    <option value="ALL">All Pricing</option>
                    <option value="FREE">Free Sessions</option>
                    <option value="PAID">Paid Sessions</option>
                  </select>
                </div>
              </div>

              {/* Filter Pills and Reset Button */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-100 text-xs">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-slate-400 font-medium">Quick Filters:</span>
                  <button
                    onClick={() => setPriceFilter(priceFilter === 'FREE' ? 'ALL' : 'FREE')}
                    className={`px-2.5 py-1 rounded-lg border text-xs font-medium transition-all ${
                      priceFilter === 'FREE'
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-300'
                        : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    Free Only
                  </button>

                  <button
                    onClick={() => setSelectedType(selectedType === 'WEBINAR' ? '' : 'WEBINAR')}
                    className={`px-2.5 py-1 rounded-lg border text-xs font-medium transition-all ${
                      selectedType === 'WEBINAR'
                        ? 'bg-indigo-50 text-indigo-700 border-indigo-300'
                        : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    Webinars Only
                  </button>

                  <button
                    onClick={() => setUpcomingOnly(!upcomingOnly)}
                    className={`px-2.5 py-1 rounded-lg border text-xs font-medium transition-all ${
                      upcomingOnly
                        ? 'bg-blue-50 text-blue-700 border-blue-300'
                        : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    Upcoming Only
                  </button>
                </div>

                {hasActiveFilters && (
                  <button
                    onClick={handleClearFilters}
                    className="flex items-center gap-1 text-slate-500 hover:text-rose-600 font-medium transition-colors cursor-pointer"
                  >
                    <RotateCcw className="w-3 h-3" />
                    <span>Clear All Filters</span>
                  </button>
                )}
              </div>
            </div>

            {/* Sessions Discovery Results */}
            {loadingSessions ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {[1, 2, 3, 4, 5, 6].map((idx) => (
                  <div key={idx} className="bg-white rounded-2xl border border-slate-200/80 p-6 animate-pulse space-y-4">
                    <div className="flex justify-between">
                      <div className="h-5 w-24 bg-slate-100 rounded" />
                      <div className="h-5 w-16 bg-slate-100 rounded" />
                    </div>
                    <div className="h-6 w-3/4 bg-slate-100 rounded" />
                    <div className="h-12 w-full bg-slate-100 rounded" />
                    <div className="h-10 w-full bg-slate-100 rounded pt-2" />
                  </div>
                ))}
              </div>
            ) : sessions.length === 0 ? (
              <div className="bg-white rounded-3xl border border-dashed border-slate-300 p-12 text-center space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto">
                  <Calendar className="w-6 h-6" />
                </div>
                <h3 className="text-base font-bold text-slate-900">No sessions match your search</h3>
                <p className="text-xs text-slate-500 max-w-md mx-auto">
                  We couldn&apos;t find any published sessions or webinars matching your current filters. Try selecting another category or clearing your search term.
                </p>
                {hasActiveFilters && (
                  <Button variant="outline" size="sm" onClick={handleClearFilters}>
                    Clear Filters
                  </Button>
                )}
              </div>
            ) : hasActiveFilters ? (
              /* Filtered Results View */
              <div className="space-y-4">
                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span>Found <strong>{sessions.length}</strong> matching sessions</span>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {sessions.map((session) => (
                    <SessionCard
                      key={session.id}
                      session={session}
                      formatSessionType={formatSessionType}
                      onViewDetails={() => setSelectedSessionModal(session)}
                      onBookSession={() => setSelectedBookingSession(session)}
                    />
                  ))}
                </div>
              </div>
            ) : (
              /* Discovery Sections View (When no custom search is typed) */
              <div className="space-y-10">
                {/* Section 1: Upcoming Sessions */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                        <Calendar className="w-4 h-4 text-indigo-600" />
                        <span>Upcoming Sessions & Webinars</span>
                      </h2>
                      <p className="text-xs text-slate-500">Scheduled live mentoring and workshops in the coming days</p>
                    </div>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {upcomingSessionsList.slice(0, 3).map((session) => (
                      <SessionCard
                        key={session.id}
                        session={session}
                        formatSessionType={formatSessionType}
                        onViewDetails={() => setSelectedSessionModal(session)}
                        onBookSession={() => setSelectedBookingSession(session)}
                      />
                    ))}
                  </div>
                </div>

                {/* Section 2: Free Sessions */}
                {freeSessionsList.length > 0 && (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                          <Sparkles className="w-4 h-4 text-emerald-600" />
                          <span>Free Community Sessions & Webinars</span>
                        </h2>
                        <p className="text-xs text-slate-500">Open masterclasses and knowledge sharing sessions at zero cost</p>
                      </div>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                      {freeSessionsList.slice(0, 3).map((session) => (
                        <SessionCard
                          key={session.id}
                          session={session}
                          formatSessionType={formatSessionType}
                          onViewDetails={() => setSelectedSessionModal(session)}
                          onBookSession={() => setSelectedBookingSession(session)}
                        />
                      ))}
                    </div>
                  </div>
                )}

                {/* Section 3: Trending Sessions (Honest real data state) */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                        <Users className="w-4 h-4 text-amber-500" />
                        <span>Trending Sessions</span>
                      </h2>
                      <p className="text-xs text-slate-500">High-demand sessions based on learner engagement</p>
                    </div>
                  </div>
                  <div className="rounded-2xl border border-slate-200/90 bg-white p-6 sm:p-8 text-center space-y-2">
                    <p className="text-xs font-semibold text-slate-700">
                      Trending statistics will activate as community registrations grow.
                    </p>
                    <p className="text-[11px] text-slate-400 max-w-md mx-auto">
                      All published sessions currently have open slots. Browse the upcoming sessions section above to discover opportunities with verified mentors.
                    </p>
                  </div>
                </div>

                {/* Section 4: Sessions Based on Your Interests */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                        <Layers className="w-4 h-4 text-purple-600" />
                        <span>Sessions Based on Your Interests</span>
                      </h2>
                      <p className="text-xs text-slate-500">Recommended based on platform specializations and tech tracks</p>
                    </div>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {interestSessionsList.map((session) => (
                      <SessionCard
                        key={session.id}
                        session={session}
                        formatSessionType={formatSessionType}
                        onViewDetails={() => setSelectedSessionModal(session)}
                        onBookSession={() => setSelectedBookingSession(session)}
                      />
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: MY 1-ON-1 BOOKINGS (Preserved) */}
        {activeTab === 'BOOKINGS' && (
          <div className="space-y-6">
            {/* Filter and Search Bar for Bookings */}
            <div className="flex flex-col md:flex-row items-center justify-between gap-4 border-b border-gray-200 pb-4">
              <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto pb-2 md:pb-0">
                {['ALL', 'CONFIRMED', 'PENDING', 'COMPLETED', 'CANCELLED'].map((status) => (
                  <button
                    key={status}
                    onClick={() => setBookingFilterStatus(status)}
                    className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-colors whitespace-nowrap cursor-pointer ${
                      bookingFilterStatus === status
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                  >
                    {status === 'ALL' ? 'All Sessions' : status.charAt(0) + status.slice(1).toLowerCase()}
                  </button>
                ))}
              </div>

              <div className="relative w-full md:w-72">
                <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Search by name, topic, or ref..."
                  value={bookingSearchQuery}
                  onChange={(e) => setBookingSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 text-xs border border-gray-200 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
                />
              </div>
            </div>

            {/* Bookings List */}
            {loadingBookings ? (
              <div className="flex justify-center items-center py-20">
                <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600"></div>
              </div>
            ) : filteredBookings.length === 0 ? (
              <div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
                <Calendar className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <h3 className="text-base font-semibold text-gray-900">No bookings found</h3>
                <p className="text-xs text-gray-500 max-w-sm mx-auto mt-1 mb-6">
                  {bookingFilterStatus === 'ALL'
                    ? "You haven't scheduled any 1-on-1 mentorship bookings yet."
                    : `No bookings with status ${bookingFilterStatus.toLowerCase()} found.`}
                </p>
                {currentMode === 'LEARNER' && (
                  <Link to="/mentors">
                    <Button variant="outline" size="sm">Explore Available Mentors</Button>
                  </Link>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                {filteredBookings.map((booking) => {
                  const peer = currentMode === 'MENTOR' ? booking.learner : booking.mentor;
                  const peerImage = currentMode === 'MENTOR'
                    ? booking.learner?.profileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${booking.learner?.fullName}`
                    : booking.mentor?.profileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${booking.mentor?.name}`;
                  const peerName = currentMode === 'MENTOR' ? booking.learner?.fullName : booking.mentor?.name;

                  return (
                    <div
                      key={booking.id}
                      className="bg-white rounded-2xl border border-gray-100 p-6 shadow-xs hover:border-indigo-100 transition-colors flex flex-col md:flex-row md:items-center justify-between gap-6"
                    >
                      {/* Left: Peer info & Session details */}
                      <div className="flex items-start gap-4">
                        <UserAvatar src={peerImage} name={peerName} size="lg" className="rounded-2xl shrink-0" />
                        <div>
                          <div className="flex items-center gap-2 mb-1 flex-wrap">
                            <span className="text-xs font-mono font-semibold text-gray-400">
                              #{booking.bookingRef}
                            </span>
                            <Badge variant={getStatusBadgeVariant(booking.status)}>
                              {booking.status}
                            </Badge>
                            {Number(booking.totalAmount) === 0 && (
                              <span className="text-xs bg-emerald-50 text-emerald-700 border border-emerald-200 px-2 py-0.5 rounded-full font-bold">
                                Free Session
                              </span>
                            )}
                            {(booking.sessionTitle || booking.topic?.name) && (
                              <span className="text-xs bg-indigo-50 text-indigo-700 px-2.5 py-0.5 rounded-full font-medium">
                                {booking.sessionTitle || booking.topic.name}
                              </span>
                            )}
                          </div>

                          <h3 className="text-base font-bold text-gray-900">{peerName}</h3>

                          <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-gray-500 mt-2">
                            <span className="flex items-center gap-1">
                              <Calendar className="w-3.5 h-3.5 text-gray-400" />
                              {booking.bookingDate}
                            </span>
                            <span className="flex items-center gap-1">
                              <Clock className="w-3.5 h-3.5 text-gray-400" />
                              {booking.startTime?.slice(0, 5)} - {booking.endTime?.slice(0, 5)} ({booking.durationMinutes} mins)
                            </span>
                            <span className="font-semibold text-gray-900">
                              {Number(booking.totalAmount) === 0 ? (
                                <span className="text-emerald-600 font-bold">₹0 (Free)</span>
                              ) : (
                                `₹${booking.totalAmount}`
                              )}
                            </span>
                          </div>

                          {booking.cancellationReason && (
                            <p className="text-xs text-red-600 bg-red-50 px-3 py-1.5 rounded-lg mt-3">
                              <span className="font-semibold">Cancelled:</span> {booking.cancellationReason}
                            </p>
                          )}
                        </div>
                      </div>

                      {/* Right: Actions */}
                      <div className="flex flex-wrap items-center gap-2.5 self-end md:self-center">
                        <Link to={`/messages?user=${currentMode === 'MENTOR' ? booking.learner?.id : booking.mentor?.userId}`}>
                          <Button variant="outline" size="sm" className="flex items-center gap-1 text-xs">
                            <MessageSquare className="w-3.5 h-3.5" /> Chat
                          </Button>
                        </Link>

                        {currentMode === 'MENTOR' && booking.status === 'PENDING' && (
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={async () => {
                              try {
                                await api.patch(`/bookings/${booking.id}/confirm`);
                                fetchBookings();
                              } catch (err) {
                                console.error('Failed to confirm booking', err);
                              }
                            }}
                            className="bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1 text-xs cursor-pointer shadow-xs"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5" /> Accept
                          </Button>
                        )}

                        {(booking.status === 'CONFIRMED' || booking.status === 'IN_PROGRESS') && (
                          <Button
                            size="sm"
                            onClick={() => handleStartSessionCall(booking)}
                            className="bg-indigo-600 hover:bg-indigo-700 flex items-center gap-1.5 text-xs shadow-xs text-white cursor-pointer"
                          >
                            <Video className="w-4 h-4" /> Start Video Call
                          </Button>
                        )}

                        {currentMode === 'LEARNER' && booking.status === 'COMPLETED' && !booking.hasReview && (
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={() => setSelectedReviewBooking(booking)}
                            className="bg-amber-500 hover:bg-amber-600 text-white flex items-center gap-1 text-xs"
                          >
                            <Star className="w-3.5 h-3.5 fill-white" /> Leave Review
                          </Button>
                        )}

                        {(booking.status === 'PENDING' || booking.status === 'CONFIRMED') && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleCancelBooking(booking.id)}
                            isLoading={cancellingBookingId === booking.id}
                            className="text-xs text-red-600 hover:bg-red-50 border-red-200"
                          >
                            Cancel
                          </Button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Review Modal for bookings */}
        {selectedReviewBooking && (
          <ReviewModal
            isOpen={!!selectedReviewBooking}
            onClose={() => setSelectedReviewBooking(null)}
            bookingId={selectedReviewBooking.id}
            mentorName={selectedReviewBooking.mentor?.name}
            onSuccess={() => {
              setSelectedReviewBooking(null);
              fetchBookings();
            }}
          />
        )}

        {/* Session Details Modal */}
        {selectedSessionModal && (
          <SessionDetailModal
            session={selectedSessionModal}
            formatSessionType={formatSessionType}
            onClose={() => setSelectedSessionModal(null)}
            onBookSession={() => {
              const s = selectedSessionModal;
              setSelectedSessionModal(null);
              setSelectedBookingSession(s);
            }}
          />
        )}

        {/* Session Booking Modal */}
        {selectedBookingSession && (
          <SessionBookingModal
            session={selectedBookingSession}
            isOpen={Boolean(selectedBookingSession)}
            onClose={() => setSelectedBookingSession(null)}
            onSuccess={handleBookingSuccess}
          />
        )}
      </div>
    </div>
  );
};

// Sub-component: Session Card
const SessionCard = ({ session, formatSessionType, onViewDetails, onBookSession }) => {
  const isFree = session.isFree || Number(session.price) === 0;

  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 p-5 sm:p-6 flex flex-col justify-between hover:border-indigo-300 hover:shadow-lg transition-all duration-200 group">
      <div className="space-y-3.5">
        {/* Top Badges */}
        <div className="flex items-center justify-between gap-2 flex-wrap">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="px-2.5 py-0.5 rounded-full bg-indigo-50 text-indigo-700 text-[11px] font-bold">
              {formatSessionType(session.sessionType)}
            </span>
            {session.categoryName && (
              <span className="px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700 text-[11px] font-medium">
                {session.categoryName}
              </span>
            )}
          </div>

          <span
            className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${
              isFree
                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                : 'bg-amber-50 text-amber-800 border border-amber-200'
            }`}
          >
            {isFree ? 'Free' : `₹${session.price}`}
          </span>
        </div>

        {/* Title */}
        <h3
          onClick={onViewDetails}
          className="text-base font-bold text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-2 cursor-pointer leading-snug"
        >
          {session.title}
        </h3>

        {/* Description */}
        {session.description && (
          <p className="text-xs text-slate-500 line-clamp-2 leading-relaxed">
            {session.description}
          </p>
        )}

        {/* Mentor Info */}
        <div className="pt-2 flex items-center gap-3">
          <Link to={`/mentors/${session.mentorId}`} className="shrink-0">
            <img
              src={session.mentorProfileImage || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100'}
              alt={session.mentorName}
              className="w-9 h-9 rounded-full object-cover border border-slate-200 hover:ring-2 hover:ring-indigo-300 transition-all"
            />
          </Link>
          <div className="min-w-0 flex-1">
            <Link
              to={`/mentors/${session.mentorId}`}
              className="text-xs font-bold text-slate-800 hover:text-indigo-600 truncate block transition-colors"
            >
              {session.mentorName}
            </Link>
            <p className="text-[11px] text-slate-400 truncate">
              {session.mentorHeadline || 'Expert Mentor'}
            </p>
          </div>
        </div>

        {/* Schedule & Duration */}
        <div className="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between text-[11px] text-slate-500 gap-y-1">
          <span className="flex items-center gap-1 font-medium">
            <Calendar className="w-3.5 h-3.5 text-indigo-500" />
            {session.sessionDate || 'Upcoming'}
          </span>
          <span className="flex items-center gap-1">
            <Clock className="w-3.5 h-3.5 text-slate-400" />
            {session.startTime ? session.startTime.slice(0, 5) : ''}
            {session.durationMinutes ? ` (${session.durationMinutes} min)` : ''}
          </span>
        </div>
      </div>

      {/* Card Actions */}
      <div className="pt-4 border-t border-slate-100 mt-4 flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={onViewDetails}
          className="flex-1 text-xs border-slate-200 hover:border-indigo-300 hover:bg-indigo-50/50"
        >
          View Details
        </Button>

        {/* Book Session Action */}
        <Button
          variant="primary"
          size="sm"
          onClick={onBookSession}
          className="flex-1 text-xs"
        >
          Book Session
        </Button>
      </div>
    </div>
  );
};

// Sub-component: Session Detail Modal
const SessionDetailModal = ({ session, formatSessionType, onClose, onBookSession }) => {
  const isFree = session.isFree || Number(session.price) === 0;
  const [reviews, setReviews] = useState([]);
  const [loadingReviews, setLoadingReviews] = useState(true);

  useEffect(() => {
    let isMounted = true;
    const fetchPreviousReviews = async () => {
      if (!session?.mentorId) {
        setLoadingReviews(false);
        return;
      }
      try {
        setLoadingReviews(true);
        const res = await api.get(`/reviews/mentor/${session.mentorId}?size=5`);
        if (isMounted && res.data?.success && res.data.data) {
          const list = res.data.data.content || res.data.data || [];
          setReviews(list);
        }
      } catch (err) {
        console.error('Failed to load previous session reviews:', err);
      } finally {
        if (isMounted) setLoadingReviews(false);
      }
    };

    fetchPreviousReviews();
    return () => { isMounted = false; };
  }, [session?.mentorId]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-xl w-full p-6 sm:p-8 shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="space-y-2">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-bold">
              {formatSessionType(session.sessionType)}
            </span>
            {session.categoryName && (
              <span className="px-3 py-1 rounded-full bg-slate-100 text-slate-700 text-xs font-medium">
                {session.categoryName}
              </span>
            )}
            <span
              className={`px-3 py-1 rounded-full text-xs font-bold ${
                isFree
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : 'bg-amber-50 text-amber-800 border border-amber-200'
              }`}
            >
              {isFree ? 'Free Admission' : `Price: ₹${session.price}`}
            </span>
          </div>

          <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">
            {session.title}
          </h2>
        </div>

        {/* Timing Information */}
        <div className="bg-slate-50 rounded-2xl p-4 grid grid-cols-2 gap-4 text-xs">
          <div>
            <span className="text-slate-400 font-semibold block mb-1">Date & Time</span>
            <p className="font-bold text-slate-800 flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-indigo-600" />
              {session.sessionDate || 'Scheduled Date'}
            </p>
            <p className="text-slate-500 mt-0.5">
              {session.startTime ? session.startTime.slice(0, 5) : ''}
              {session.endTime ? ` - ${session.endTime.slice(0, 5)}` : ''}
            </p>
          </div>
          <div>
            <span className="text-slate-400 font-semibold block mb-1">Duration</span>
            <p className="font-bold text-slate-800 flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-indigo-600" />
              {session.durationMinutes ? `${session.durationMinutes} Minutes` : '60 Minutes'}
            </p>
            <p className="text-slate-500 mt-0.5">Live Interactive Format</p>
          </div>
        </div>

        {/* Detailed Description */}
        <div className="space-y-2">
          <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
            About this Session
          </h4>
          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed whitespace-pre-line">
            {session.description || 'No description provided by the mentor.'}
          </p>
        </div>

        {/* Mentor Profile Strip */}
        <div className="bg-indigo-50/50 rounded-2xl p-4 border border-indigo-100/70 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <img
              src={session.mentorProfileImage || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100'}
              alt={session.mentorName}
              className="w-11 h-11 rounded-full object-cover border border-white shadow-xs"
            />
            <div>
              <p className="text-xs font-bold text-slate-900">{session.mentorName}</p>
              <p className="text-[11px] text-slate-500">{session.mentorHeadline || 'Expert Mentor'}</p>
            </div>
          </div>
          <Link to={`/mentors/${session.mentorId}`} onClick={onClose}>
            <Button variant="outline" size="sm" className="text-xs border-indigo-200 bg-white hover:bg-indigo-50">
              Profile
            </Button>
          </Link>
        </div>

        {/* Previous Session Reviews Section (Prompt Requirement #1) */}
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
              <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-400" />
              <span>Previous Session Reviews</span>
            </h4>
            {reviews.length > 0 && (
              <span className="text-[11px] font-semibold text-slate-500 bg-slate-100 px-2.5 py-0.5 rounded-full">
                {reviews.length} {reviews.length === 1 ? 'Review' : 'Reviews'}
              </span>
            )}
          </div>

          {loadingReviews ? (
            <div className="py-6 flex justify-center items-center text-xs text-slate-400 gap-2">
              <div className="w-4 h-4 rounded-full border-2 border-indigo-600 border-t-transparent animate-spin"></div>
              <span>Loading reviews from previous sessions...</span>
            </div>
          ) : reviews.length > 0 ? (
            <div className="space-y-2.5">
              {reviews.map((rev) => (
                <div
                  key={rev.id}
                  className="p-3.5 rounded-2xl bg-slate-50/80 border border-slate-200/70 space-y-2 hover:bg-slate-50 transition-colors"
                >
                  <div className="flex items-center justify-between gap-2 flex-wrap">
                    <div className="flex items-center gap-2">
                      <UserAvatar
                        src={rev.learner?.profileImage}
                        name={rev.learner?.fullName || 'Mentivo Learner'}
                        size="xs"
                        className="rounded-full shrink-0"
                      />
                      <div>
                        <span className="text-xs font-bold text-slate-900 block leading-tight">
                          {rev.learner?.fullName || 'Mentivo Learner'}
                        </span>
                        {rev.sessionDate && (
                          <span className="text-[10px] text-slate-400 flex items-center gap-1 mt-0.5">
                            <Calendar className="w-3 h-3 text-slate-400" />
                            Session Date: {rev.sessionDate}
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5">
                      <div className="flex items-center gap-0.5">
                        {[1, 2, 3, 4, 5].map((st) => (
                          <Star
                            key={st}
                            className={`w-3 h-3 ${
                              st <= rev.rating ? 'text-amber-400 fill-amber-400' : 'text-slate-200'
                            }`}
                          />
                        ))}
                      </div>
                      <span className="text-[11px] font-bold text-slate-700 ml-0.5">
                        {rev.rating}/5
                      </span>
                      {rev.verified && (
                        <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-0.5">
                          <CheckCircle2 className="w-2.5 h-2.5 text-emerald-600" />
                          Verified Review
                        </span>
                      )}
                    </div>
                  </div>

                  {rev.sessionTitle && (
                    <p className="text-[11px] font-medium text-indigo-700">
                      Topic: {rev.sessionTitle}
                    </p>
                  )}

                  <p className="text-xs text-slate-600 leading-relaxed italic">
                    "{rev.reviewText}"
                  </p>

                  {rev.createdAt && (
                    <span className="text-[10px] text-slate-400 block text-right">
                      Reviewed on {new Date(rev.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                    </span>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="p-4 rounded-2xl bg-slate-50/60 border border-slate-200/60 text-center space-y-1">
              <p className="text-xs font-semibold text-slate-700">
                No reviews yet for previous sessions.
              </p>
              <p className="text-[11px] text-slate-400">
                This mentor has not conducted previous sessions with reviews yet.
              </p>
            </div>
          )}
        </div>

        {/* External Meeting Link if mentor provided one */}
        {session.externalMeetingUrl && (
          <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-200 text-xs text-emerald-800 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>External Meeting Link Provided:</span>
            </div>
            <a
              href={session.externalMeetingUrl}
              target="_blank"
              rel="noreferrer"
              className="font-bold underline flex items-center gap-1 hover:text-emerald-900"
            >
              <span>Open Link</span>
              <ExternalLink className="w-3 h-3" />
            </a>
          </div>
        )}

        {/* Coming Soon Features Notice (Requirements 6 & 7) */}
        <div className="space-y-3 pt-2">
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-2">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-800">
              <Info className="w-4 h-4 text-indigo-600" />
              <span>Registration & Platform Automation Status</span>
            </div>
            <p className="text-[11px] text-slate-500 leading-relaxed">
              Automated attendee registration, capacity limits, slot reservations, and payments are being prepared for an upcoming release.
            </p>

            <div className="pt-2 flex flex-wrap gap-2">
              <Button
                variant="primary"
                size="sm"
                onClick={onBookSession}
                className="px-5 py-2 text-xs rounded-xl"
              >
                Book Session Now
              </Button>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" size="sm" onClick={onClose}>
              Close
            </Button>
            <Link to={`/mentors/${session.mentorId}`} onClick={onClose}>
              <Button variant="primary" size="sm" className="flex items-center gap-1.5 text-xs">
                <span>Contact Mentor</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SessionsPage;
