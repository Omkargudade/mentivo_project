import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { OnlineBadge, VerificationBadge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { UserAvatar } from '../components/common/UserAvatar';
import { BookingModal } from '../components/booking/BookingModal';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { useFeatureFlags } from '../context/FeatureFlagContext';
import { useCall } from '../context/CallContext';
import { useSocket } from '../context/SocketContext';
import api from '../api/client';
import { Star, Clock, Briefcase, MapPin, Globe, Calendar, MessageSquare, UserPlus, Award, ArrowLeft, Video, Users, UserCheck, ExternalLink, CheckCircle2 } from 'lucide-react';
export const MentorProfilePage = () => {
    const { id } = useParams();
    const { user } = useAuth();
    const { showToast } = useNotification();
    const { features } = useFeatureFlags();
    const { startCall } = useCall();
    const { subscribeToMentorStatus, subscribeToUserStatus } = useSocket();
    const navigate = useNavigate();
    const [mentor, setMentor] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [bookingModalOpen, setBookingModalOpen] = useState(false);
    const [isConnecting, setIsConnecting] = useState(false);
    const [connected, setConnected] = useState(false);
    const [isFollowing, setIsFollowing] = useState(false);
    const [followerCount, setFollowerCount] = useState(0);
    const [isFollowLoading, setIsFollowLoading] = useState(false);

    useEffect(() => {
        if (id) {
            fetchMentor();
        }
    }, [id]);

    // Real-time status sync for this mentor
    useEffect(() => {
        if (!id) return;
        const unsubMentor = subscribeToMentorStatus?.((card) => {
            if (card && (String(card.id) === String(id) || (mentor && card.userId === mentor.userId))) {
                setMentor((prev) => prev ? {
                    ...prev,
                    onlineStatus: card.onlineStatus,
                    lastSeen: card.lastSeen || prev.lastSeen
                } : prev);
            }
        });
        const unsubUser = subscribeToUserStatus?.((statusUpdate) => {
            if (statusUpdate && mentor && (statusUpdate.userId === mentor.userId || String(statusUpdate.mentorProfileId) === String(id))) {
                setMentor((prev) => prev ? {
                    ...prev,
                    onlineStatus: statusUpdate.onlineStatus || statusUpdate.status,
                    lastSeen: statusUpdate.timestamp || prev.lastSeen
                } : prev);
            }
        });
        return () => {
            if (unsubMentor) unsubMentor();
            if (unsubUser) unsubUser();
        };
    }, [id, mentor?.userId, subscribeToMentorStatus, subscribeToUserStatus]);

    const fetchMentor = async () => {
        setIsLoading(true);
        try {
            const res = await api.get(`/mentors/${id}`);
            if (res.data?.success && res.data.data) {
                const data = res.data.data;
                setMentor(data);
                setFollowerCount(data.learnerFollowerCount || 0);
                setIsFollowing(Boolean(data.isFollowing));
            }
        }
        catch {
            // Handled
        }
        finally {
            setIsLoading(false);
        }
    };

    const handleFollowToggle = async () => {
        if (!user) {
            navigate('/login');
            return;
        }
        if (mentor && user.id === mentor.userId) {
            showToast('You cannot follow your own mentor profile', 'info');
            return;
        }
        if (!mentor) return;

        try {
            setIsFollowLoading(true);
            if (isFollowing) {
                const res = await api.delete(`/mentors/${mentor.id}/follow`);
                if (res.data?.success) {
                    setIsFollowing(false);
                    const newCount = res.data.data?.followerCount ?? Math.max(0, followerCount - 1);
                    setFollowerCount(newCount);
                    showToast('Unfollowed mentor', 'info');
                }
            } else {
                const res = await api.post(`/mentors/${mentor.id}/follow`);
                if (res.data?.success) {
                    setIsFollowing(true);
                    const newCount = res.data.data?.followerCount ?? (followerCount + 1);
                    setFollowerCount(newCount);
                    showToast('You are now following this mentor!', 'success');
                }
            }
        } catch (err) {
            showToast(err.response?.data?.message || 'Failed to update follow status', 'error');
        } finally {
            setIsFollowLoading(false);
        }
    };
    const handleConnect = async () => {
        if (!user) {
            navigate('/login');
            return;
        }
        if (mentor && user.id === mentor.userId) {
            showToast('You cannot connect with yourself', 'error');
            return;
        }
        if (!mentor)
            return;
        try {
            setIsConnecting(true);
            const targetName = mentor.name || mentor.user?.fullName || 'Mentor';
            const res = await api.post('/connections', {
                receiverId: mentor.userId,
                message: `Hi ${targetName}, I would love to connect with you on Mentivo!`,
            });
            if (res.data?.success) {
                setConnected(true);
                showToast(`Connection request sent to ${targetName}!`, 'success');
            }
        }
        catch (err) {
            showToast(err.response?.data?.message || 'Connection request already exists', 'info');
        }
        finally {
            setIsConnecting(false);
        }
    };
    const handleMessage = async () => {
        if (!user) {
            navigate('/login');
            return;
        }
        if (!mentor)
            return;
        if (!features.messaging) {
            showToast('Direct messaging is currently disabled by administrator', 'info');
            return;
        }
        try {
            const res = await api.post('/conversations', { peerId: mentor.userId });
            if (res.data?.success && res.data.data) {
                navigate(`/messages?convId=${res.data.data.id}`);
            }
            else {
                navigate('/messages');
            }
        }
        catch {
            navigate('/messages');
        }
    };
    const handleVideoCall = () => {
        if (!user) {
            navigate('/login');
            return;
        }
        if (!mentor)
            return;
        if (user.id === mentor.userId) {
            showToast('You cannot call yourself', 'error');
            return;
        }
        if (!features.videoCalling) {
            showToast('Video calling is currently disabled by administrator', 'info');
            return;
        }
        const targetName = mentor.name || mentor.user?.fullName || 'Mentor';
        const targetImage = mentor.profileImage || mentor.user?.profileImage;
        startCall(mentor.userId, targetName, targetImage);
    };
    if (isLoading) {
        return (<div className="max-w-4xl mx-auto px-4 py-12 text-center text-slate-500">
        Loading mentor profile...
      </div>);
    }
    if (!mentor) {
        return (<div className="max-w-md mx-auto px-4 py-16 text-center">
        <h2 className="text-xl font-bold text-slate-900 mb-2">Mentor Not Found</h2>
        <p className="text-xs text-slate-500 mb-6">This mentor profile could not be located or has been deactivated.</p>
        <Link to="/mentors">
          <Button variant="primary" size="sm">Browse All Mentors</Button>
        </Link>
      </div>);
    }
    const mentorName = mentor.name || mentor.user?.fullName || 'Mentor';
    const mentorHeadline = mentor.headline || mentor.user?.headline || 'Verified Knowledge Mentor';
    const mentorBio = mentor.bio || mentor.user?.bio || mentor.bioSummary || 'Dedicated mentor passionate about sharing practical knowledge and mentoring learners.';
    const mentorLocation = mentor.location || mentor.user?.location;
    const mentorLanguages = mentor.languages || mentor.user?.languages;
    const mentorImage = mentor.profileImage || mentor.user?.profileImage;
    const isFree = Number(mentor.hourlyRate) === 0;
    return (<div className="min-h-screen bg-slate-50 py-8">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Back navigation */}
        <Link to="/mentors" className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-indigo-600 mb-6 transition-colors">
          <ArrowLeft className="w-4 h-4"/>
          <span>Back to Mentors</span>
        </Link>

        {/* Profile Card Banner */}
        <div className="bg-white rounded-3xl border border-slate-200/80 p-6 sm:p-8 shadow-xs mb-6">
          <div className="flex flex-col md:flex-row items-start justify-between gap-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5">
              <UserAvatar src={mentorImage} name={mentorName} size="2xl" className="border-2 border-slate-100 shadow-md shadow-slate-100 shrink-0"/>
              <div className="space-y-1.5">
                <div className="flex items-center gap-2.5 flex-wrap">
                  <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
                    {mentorName}
                  </h1>
                  <OnlineBadge status={mentor.onlineStatus}/>
                  {isFree && (<span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
                      Free Mentoring (₹0)
                    </span>)}
                </div>
                <p className="text-sm font-medium text-slate-600 max-w-xl">
                  {mentorHeadline}
                </p>
                <div className="flex items-center gap-3 text-xs text-slate-500 flex-wrap pt-1">
                  <VerificationBadge level={mentor.verificationLevel}/>
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
                    <Users className="w-3.5 h-3.5 text-indigo-600"/>
                    Website Followers: {followerCount}
                  </span>
                  {mentorLocation && (<span className="flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-slate-400"/>
                      {mentorLocation}
                    </span>)}
                  {mentorLanguages && (<span className="flex items-center gap-1">
                      <Globe className="w-3.5 h-3.5 text-slate-400"/>
                      {mentorLanguages}
                    </span>)}
                </div>
              </div>
            </div>

            {/* Price & Action Widget */}
            <div className="w-full md:w-auto bg-slate-50 rounded-2xl p-4 border border-slate-200 text-center space-y-3 min-w-[210px]">
              <div>
                {isFree ? (<div className="py-0.5">
                    <span className="text-3xl font-black text-emerald-600">Free</span>
                    <span className="block text-[11px] font-bold text-emerald-700 bg-emerald-100/70 border border-emerald-300 rounded-lg px-2 py-0.5 mt-1">
                      Volunteer / ₹0 per hour
                    </span>
                  </div>) : (<div>
                    <span className="text-2xl font-black text-slate-900">₹{mentor.hourlyRate}</span>
                    <span className="text-xs text-slate-500"> / hour</span>
                  </div>)}
              </div>

              <div className="space-y-2">
                <Button variant="primary" size="md" onClick={() => setBookingModalOpen(true)} className={`w-full gap-1.5 ${isFree ? 'bg-emerald-600 hover:bg-emerald-700 text-white border-transparent' : ''}`}>
                  <Calendar className="w-4 h-4"/>
                  <span>{isFree ? 'Book Free Session' : 'Book Session'}</span>
                </Button>

                <Button variant="outline" size="sm" onClick={handleVideoCall} className={`w-full gap-1.5 text-xs font-semibold ${features.videoCalling
            ? 'border-indigo-200 text-indigo-700 bg-indigo-50/60 hover:bg-indigo-100 hover:border-indigo-300'
            : 'opacity-50 cursor-not-allowed bg-slate-50 text-slate-400'}`} title={features.videoCalling ? 'Start 1-to-1 Free Video Call' : 'Video calling is currently disabled'}>
                  <Video className="w-4 h-4 text-indigo-600"/>
                  <span>Free 1-on-1 Video Call</span>
                </Button>

                <div className="grid grid-cols-2 gap-2">
                  <Button variant="outline" size="sm" onClick={handleMessage} className={`gap-1 text-xs ${!features.messaging ? 'opacity-50 cursor-not-allowed text-slate-400' : ''}`} title={features.messaging ? 'Direct Chat' : 'Messaging is currently disabled'}>
                    <MessageSquare className="w-3.5 h-3.5"/>
                    <span>Chat</span>
                  </Button>
                  <Button variant="outline" size="sm" disabled={connected || isConnecting} onClick={handleConnect} className="gap-1 text-xs">
                    <UserPlus className="w-3.5 h-3.5"/>
                    <span>{connected ? 'Sent' : 'Connect'}</span>
                  </Button>
                </div>

                {(!user || user.id !== mentor.userId) && (
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={isFollowLoading}
                    onClick={handleFollowToggle}
                    className={`w-full gap-1.5 text-xs font-semibold transition-all ${
                      isFollowing
                        ? 'border-emerald-300 text-emerald-700 bg-emerald-50 hover:bg-emerald-100 hover:border-emerald-400'
                        : 'border-slate-300 text-slate-700 hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-300'
                    }`}
                  >
                    {isFollowing ? <UserCheck className="w-3.5 h-3.5 text-emerald-600"/> : <Users className="w-3.5 h-3.5"/>}
                    <span>{isFollowing ? '✓ Following' : '+ Follow Mentor'}</span>
                  </Button>
                )}
              </div>
            </div>
          </div>

          {/* Quick Metrics Bar */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-8 pt-6 border-t border-slate-100 text-center">
            <div>
              <div className="flex items-center justify-center gap-1 text-base font-bold text-slate-900">
                <Star className="w-4 h-4 text-amber-500 fill-amber-400"/>
                <span>{mentor.averageRating > 0 ? mentor.averageRating.toFixed(1) : 'New'}</span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                {mentor.totalReviews > 0 ? `${mentor.totalReviews} verified ${mentor.totalReviews === 1 ? 'review' : 'reviews'}` : 'No verified reviews yet'}
              </p>
            </div>

            <div>
              <div className="flex items-center justify-center gap-1 text-base font-bold text-slate-900">
                <Briefcase className="w-4 h-4 text-slate-400"/>
                <span>{mentor.experienceYears} Years</span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">Industry Experience</p>
            </div>

            <div>
              <div className="flex items-center justify-center gap-1 text-base font-bold text-slate-900">
                <Award className="w-4 h-4 text-slate-400"/>
                <span>{mentor.sessionsCompleted}</span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">Sessions Completed</p>
            </div>

            <div>
              <div className="flex items-center justify-center gap-1 text-base font-bold text-slate-900">
                <Clock className="w-4 h-4 text-slate-400"/>
                <span>~{mentor.responseTimeMinutes} mins</span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">Avg. Response Time</p>
            </div>
          </div>
        </div>

        {/* 2-Column Details: About / Skills vs Availability & Reviews */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Left Column: Bio & Skills */}
          <div className="md:col-span-2 space-y-6">
            {/* About / Bio */}
            <div className="bg-white rounded-3xl border border-slate-200/80 p-6 shadow-xs">
              <h3 className="text-base font-bold text-slate-900 mb-3">About {mentorName}</h3>
              <p className="text-xs text-slate-600 leading-relaxed whitespace-pre-line">
                {mentorBio}
              </p>
            </div>

            {/* Skills & Topics */}
            <div className="bg-white rounded-3xl border border-slate-200/80 p-6 shadow-xs space-y-4">
              <h3 className="text-base font-bold text-slate-900">Areas of Expertise</h3>

              <div>
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Skills</span>
                <div className="flex flex-wrap gap-2">
                  {mentor.skills?.map((s) => (<span key={s.id} className="px-3 py-1 rounded-xl text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
                      {s.name}
                    </span>))}
                </div>
              </div>

              <div>
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Topics</span>
                <div className="flex flex-wrap gap-2">
                  {mentor.topics?.map((t) => (<span key={t.id} className="px-3 py-1 rounded-xl text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
                      #{t.name}
                    </span>))}
                </div>
              </div>
            </div>

            {/* Verified Reviews Section (Prompt Requirement #2) */}
            <div className="bg-white rounded-3xl border border-slate-200/80 p-6 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                    <span>Verified Reviews</span>
                    {mentor.totalReviews > 0 && (
                      <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                        {mentor.totalReviews} eligible
                      </span>
                    )}
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Authentic feedback from learners who completed verified 1-on-1 sessions.
                  </p>
                </div>

                {mentor.totalReviews > 0 && (
                  <div className="text-right">
                    <div className="flex items-center justify-end gap-1 text-sm font-black text-slate-900">
                      <Star className="w-4 h-4 text-amber-500 fill-amber-400" />
                      <span>{mentor.averageRating > 0 ? mentor.averageRating.toFixed(1) : '5.0'}</span>
                      <span className="text-slate-400 text-xs font-normal">/ 5.0</span>
                    </div>
                    <span className="text-[11px] text-slate-400">
                      Based on {mentor.totalReviews} {mentor.totalReviews === 1 ? 'review' : 'reviews'}
                    </span>
                  </div>
                )}
              </div>

              {mentor.recentReviews && mentor.recentReviews.length > 0 ? (
                <div className="divide-y divide-slate-100">
                  {mentor.recentReviews.map((rev) => (
                    <div key={rev.id} className="py-4 first:pt-0 last:pb-0 space-y-2">
                      <div className="flex items-center justify-between gap-2 flex-wrap">
                        <div className="flex items-center gap-2.5">
                          <UserAvatar
                            src={rev.learner?.profileImage}
                            name={rev.learner?.fullName || 'Mentivo Learner'}
                            size="sm"
                            className="rounded-full shrink-0"
                          />
                          <div>
                            <span className="text-xs font-bold text-slate-900 block leading-tight">
                              {rev.learner?.fullName || 'Mentivo Learner'}
                            </span>
                            {rev.sessionDate && (
                              <span className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                                <Calendar className="w-3 h-3 text-slate-400" />
                                Session: {rev.sessionDate}
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          <div className="flex items-center gap-0.5">
                            {[1, 2, 3, 4, 5].map((st) => (
                              <Star
                                key={st}
                                className={`w-3.5 h-3.5 ${
                                  st <= rev.rating ? 'text-amber-400 fill-amber-400' : 'text-slate-200'
                                }`}
                              />
                            ))}
                          </div>
                          <span className="text-xs font-bold text-slate-700 ml-0.5">
                            {rev.rating}/5
                          </span>
                          {/* Only show Verified Review badge if genuinely verified */}
                          {rev.verified ? (
                            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1">
                              <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                              Verified Review
                            </span>
                          ) : (
                            <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-600">
                              Community Feedback
                            </span>
                          )}
                        </div>
                      </div>

                      {rev.sessionTitle && (
                        <p className="text-xs font-medium text-indigo-700">
                          Topic: {rev.sessionTitle}
                        </p>
                      )}

                      <p className="text-xs text-slate-600 leading-relaxed italic">
                        "{rev.reviewText}"
                      </p>

                      {rev.createdAt && (
                        <span className="text-[10px] text-slate-400 block text-right">
                          Submitted on {new Date(rev.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="py-8 text-center bg-slate-50/60 rounded-2xl border border-slate-200/60">
                  <p className="text-xs font-semibold text-slate-700">
                    No verified reviews yet.
                  </p>
                  <p className="text-[11px] text-slate-400 mt-1 max-w-sm mx-auto">
                    Reviews only appear here after learners attend and complete genuine 1-on-1 mentoring sessions.
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Community / Social & Weekly Availability */}
          <div className="space-y-6">
            {/* Community & Social Profiles Showcase */}
            <div className="bg-white rounded-3xl border border-slate-200/80 p-6 shadow-xs space-y-4">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Globe className="w-4 h-4 text-indigo-600"/>
                <span>Community & Social Profiles</span>
              </h3>

              <div className="space-y-3 divide-y divide-slate-100">
                {/* Website Learner Followers */}
                <div className="pt-1 first:pt-0 flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-700 flex items-center gap-2">
                    <Users className="w-4 h-4 text-indigo-600"/>
                    Website Learner Followers:
                  </span>
                  <span className="font-bold text-slate-900 bg-indigo-50 border border-indigo-100 px-2.5 py-0.5 rounded-full">
                    {followerCount}
                  </span>
                </div>

                {/* YouTube */}
                {mentor.youtubeUrl && (
                  <div className="pt-3.5 space-y-1.5 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-slate-700 flex items-center gap-2">
                        <svg className="w-4 h-4 text-red-600 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
                        </svg>
                        YouTube:
                      </span>
                      <a
                        href={mentor.youtubeUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 font-semibold text-red-600 hover:text-red-700 hover:underline"
                      >
                        <span>Visit Channel</span>
                        <ExternalLink className="w-3 h-3"/>
                      </a>
                    </div>
                    {mentor.showYoutubeSubscribers && mentor.youtubeSubscribersCount != null && (
                      <div className="flex items-center justify-between pl-6 text-slate-500">
                        <span>YouTube Subscribers:</span>
                        <span className="font-semibold text-slate-800">
                          {Number(mentor.youtubeSubscribersCount).toLocaleString()} <span className="text-[11px] font-normal text-slate-500">({mentor.youtubeSubscribersSource || 'Self-reported'})</span>
                        </span>
                      </div>
                    )}
                  </div>
                )}

                {/* Instagram */}
                {mentor.instagramUrl && (
                  <div className="pt-3.5 space-y-1.5 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-slate-700 flex items-center gap-2">
                        <svg className="w-4 h-4 text-pink-600 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/>
                        </svg>
                        Instagram:
                      </span>
                      <a
                        href={mentor.instagramUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 font-semibold text-pink-600 hover:text-pink-700 hover:underline"
                      >
                        <span>Visit Profile</span>
                        <ExternalLink className="w-3 h-3"/>
                      </a>
                    </div>
                    {mentor.showInstagramFollowers && mentor.instagramFollowersCount != null && (
                      <div className="flex items-center justify-between pl-6 text-slate-500">
                        <span>Instagram Followers:</span>
                        <span className="font-semibold text-slate-800">
                          {Number(mentor.instagramFollowersCount).toLocaleString()} <span className="text-[11px] font-normal text-slate-500">({mentor.instagramFollowersSource || 'Self-reported'})</span>
                        </span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Weekly Availability */}
            <div className="bg-white rounded-3xl border border-slate-200/80 p-6 shadow-xs">
              <h3 className="text-base font-bold text-slate-900 mb-3 flex items-center gap-2">
                <Clock className="w-4 h-4 text-indigo-600"/>
                <span>Weekly Availability</span>
              </h3>

              {mentor.availabilities && mentor.availabilities.length > 0 ? (<div className="space-y-2 text-xs">
                  {mentor.availabilities.map((av, idx) => (<div key={idx} className="flex items-center justify-between py-2 border-b border-slate-50 last:border-0">
                      <span className="font-semibold text-slate-700 capitalize">
                        {av.dayOfWeek.toLowerCase()}
                      </span>
                      <span className="text-slate-500 font-mono text-[11px]">
                        {av.startTime.substring(0, 5)} – {av.endTime.substring(0, 5)}
                      </span>
                    </div>))}
                </div>) : (<p className="text-xs text-slate-400">Flexible schedule upon request.</p>)}
            </div>
          </div>
        </div>
      </div>

      {/* Booking Modal */}
      <BookingModal mentor={mentor} isOpen={bookingModalOpen} onClose={() => setBookingModalOpen(false)} onSuccess={fetchMentor}/>
    </div>);
};
export default MentorProfilePage;
