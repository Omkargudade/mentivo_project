import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Modal } from '../common/Modal';
import Button from '../common/Button';
import { useAuth } from '../../context/AuthContext';
import { useNotification } from '../../context/NotificationContext';
import api from '../../api/client';
import {
    Calendar,
    Clock,
    User,
    CheckCircle2,
    AlertCircle,
    Video,
    Sparkles,
    ArrowRight,
    Tag
} from 'lucide-react';

export const SessionBookingModal = ({ session, isOpen, onClose, onSuccess }) => {
    const { user } = useAuth();
    const { showToast } = useNotification();
    const navigate = useNavigate();

    const [notes, setNotes] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [confirmedBooking, setConfirmedBooking] = useState(null);

    if (!session) return null;

    const isFree = session.isFree || Number(session.price) === 0;

    const handleConfirmBooking = async () => {
        if (!user) {
            setError('Please log in as a learner to book this session.');
            return;
        }

        if (session.mentorUserId && user.id === session.mentorUserId) {
            setError('You cannot book your own session as a learner.');
            return;
        }

        try {
            setError('');
            setIsSubmitting(true);

            // Normalize start time to HH:mm:ss format expected by LocalTime
            let startTimeFormatted = session.startTime || '10:00:00';
            if (startTimeFormatted.length === 5) {
                startTimeFormatted += ':00';
            }

            const payload = {
                mentorId: session.mentorId,
                sessionId: session.id,
                topicId: session.topicId || undefined,
                bookingDate: session.sessionDate,
                startTime: startTimeFormatted,
                durationMinutes: session.durationMinutes || 60,
                meetingNotes: notes.trim() || `Booked session: ${session.title}`
            };

            const res = await api.post('/bookings', payload);

            if (res.data?.success && res.data.data) {
                const booking = res.data.data;
                setConfirmedBooking(booking);
                showToast('Session booked successfully!', 'success');
                if (onSuccess) {
                    onSuccess(booking);
                }
            } else {
                throw new Error(res.data?.message || 'Failed to book session');
            }
        } catch (err) {
            const apiMsg = err.response?.data?.message || err.message || 'An error occurred while booking';
            setError(apiMsg);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleClose = () => {
        setError('');
        setNotes('');
        setConfirmedBooking(null);
        onClose();
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={handleClose}
            title={confirmedBooking ? "Booking Confirmed!" : "Book Mentorship Session"}
            subtitle={confirmedBooking ? "Your session has been saved to your schedule" : `Confirm your spot for "${session.title}"`}
            maxWidth="md"
        >
            {confirmedBooking ? (
                <div className="space-y-5 py-2">
                    <div className="text-center">
                        <div className="w-14 h-14 bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center mx-auto mb-3 border border-emerald-100 shadow-xs">
                            <CheckCircle2 className="w-8 h-8" />
                        </div>
                        <h4 className="text-lg font-bold text-slate-900">Session Confirmed</h4>
                        <p className="text-xs text-slate-500 mt-1">
                            Booking Reference: <span className="font-semibold text-slate-800">{confirmedBooking.bookingRef}</span>
                        </p>
                    </div>

                    <div className="bg-slate-50 border border-slate-100 rounded-xl p-4 space-y-2 text-xs">
                        <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                            <span className="text-slate-500">Session</span>
                            <span className="font-semibold text-slate-800">{session.title}</span>
                        </div>
                        <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                            <span className="text-slate-500">Mentor</span>
                            <span className="font-semibold text-slate-800">{session.mentorName || 'Mentor'}</span>
                        </div>
                        <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                            <span className="text-slate-500">Date & Time</span>
                            <span className="font-semibold text-slate-800">
                                {session.sessionDate} at {session.startTime ? session.startTime.slice(0, 5) : 'Scheduled'}
                            </span>
                        </div>
                        <div className="flex justify-between items-center py-1">
                            <span className="text-slate-500">Status</span>
                            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-emerald-100 text-emerald-800">
                                {confirmedBooking.status || 'CONFIRMED'}
                            </span>
                        </div>
                    </div>

                    <div className="bg-indigo-50/70 border border-indigo-100 rounded-xl p-3 flex items-start gap-2.5">
                        <Video className="w-4 h-4 text-indigo-600 shrink-0 mt-0.5" />
                        <div className="text-xs">
                            <span className="font-semibold text-indigo-900">In-App Video Calling Ready</span>
                            <p className="text-indigo-700/80 text-[11px] mt-0.5">
                                You can start or join the video call directly from your Mentivo bookings dashboard when it's time.
                            </p>
                        </div>
                    </div>

                    <div className="pt-2 flex gap-3">
                        <Button
                            variant="primary"
                            onClick={() => {
                                handleClose();
                                navigate('/sessions?tab=bookings');
                            }}
                            className="w-full py-2.5 text-xs rounded-xl"
                        >
                            View in My Bookings
                        </Button>
                    </div>
                </div>
            ) : (
                <div className="space-y-4">
                    {/* Session Overview Card */}
                    <div className="bg-slate-50 border border-slate-100 rounded-xl p-4">
                        <div className="flex items-start justify-between gap-3 mb-3">
                            <div>
                                <h4 className="font-bold text-slate-900 text-sm leading-snug">
                                    {session.title}
                                </h4>
                                <p className="text-xs text-slate-500 mt-0.5 flex items-center gap-1.5">
                                    <User className="w-3.5 h-3.5 text-slate-400" />
                                    <span>{session.mentorName || 'Mentor'}</span>
                                    {session.mentorHeadline && (
                                        <span className="text-slate-400">• {session.mentorHeadline}</span>
                                    )}
                                </p>
                            </div>
                            <span className={`px-2.5 py-1 rounded-full text-xs font-bold shrink-0 ${
                                isFree ? 'bg-emerald-100 text-emerald-800' : 'bg-indigo-100 text-indigo-800'
                            }`}>
                                {isFree ? 'Free Session' : `₹${session.price}`}
                            </span>
                        </div>

                        <div className="grid grid-cols-2 gap-2 pt-3 border-t border-slate-200/60 text-xs text-slate-600">
                            <div className="flex items-center gap-2">
                                <Calendar className="w-3.5 h-3.5 text-indigo-500" />
                                <span>{session.sessionDate || 'Upcoming'}</span>
                            </div>
                            <div className="flex items-center gap-2">
                                <Clock className="w-3.5 h-3.5 text-indigo-500" />
                                <span>
                                    {session.startTime ? session.startTime.slice(0, 5) : ''}
                                    {session.durationMinutes ? ` (${session.durationMinutes} mins)` : ''}
                                </span>
                            </div>
                        </div>
                    </div>

                    {!user ? (
                        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-center">
                            <AlertCircle className="w-5 h-5 text-amber-600 mx-auto mb-1.5" />
                            <p className="text-xs font-semibold text-amber-900 mb-2">
                                Please sign in to book this session
                            </p>
                            <Link to="/login" state={{ from: { pathname: '/sessions' } }}>
                                <Button variant="primary" size="sm" className="rounded-xl text-xs">
                                    Sign In to Proceed
                                </Button>
                            </Link>
                        </div>
                    ) : (
                        <>
                            {error && (
                                <div className="bg-rose-50 border border-rose-200 text-rose-700 text-xs p-3.5 rounded-xl flex items-center gap-2">
                                    <AlertCircle className="w-4 h-4 shrink-0" />
                                    <span>{error}</span>
                                </div>
                            )}

                            <div>
                                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                                    Topics / Questions for the Mentor (Optional)
                                </label>
                                <textarea
                                    value={notes}
                                    onChange={(e) => setNotes(e.target.value)}
                                    rows={3}
                                    placeholder="Briefly describe what you'd like to learn or discuss..."
                                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:bg-white resize-none"
                                />
                            </div>

                            <div className="bg-indigo-50/50 border border-indigo-100 rounded-xl p-3 flex items-start gap-2.5">
                                <Sparkles className="w-4 h-4 text-indigo-600 shrink-0 mt-0.5" />
                                <p className="text-[11px] text-indigo-900 leading-relaxed">
                                    Your booking will be reserved instantly in the mentor's dashboard. A notification and session link will be generated automatically.
                                </p>
                            </div>

                            <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
                                <Button variant="outline" size="sm" onClick={handleClose} disabled={isSubmitting}>
                                    Cancel
                                </Button>
                                <Button
                                    variant="primary"
                                    size="sm"
                                    onClick={handleConfirmBooking}
                                    isLoading={isSubmitting}
                                    className="px-5 rounded-xl text-xs"
                                >
                                    Confirm & Book Session
                                </Button>
                            </div>
                        </>
                    )}
                </div>
            )}
        </Modal>
    );
};

export default SessionBookingModal;
