import React, { useState, useEffect } from 'react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { useAuth } from '../../context/AuthContext';
import { useNotification } from '../../context/NotificationContext';
import api from '../../api/client';
import { Calendar, Clock, CreditCard, ShieldCheck, CheckCircle2, ArrowRight } from 'lucide-react';
export const BookingModal = ({ mentor, isOpen, onClose, onSuccess, }) => {
    const { user } = useAuth();
    const { showToast } = useNotification();
    const [duration, setDuration] = useState(30);
    const [date, setDate] = useState(() => {
        const d = new Date();
        d.setDate(d.getDate() + 1);
        return d.toISOString().split('T')[0];
    });
    const [time, setTime] = useState('18:00');
    const [selectedTopicId, setSelectedTopicId] = useState(undefined);
    const [notes, setNotes] = useState('');
    const [priceCalc, setPriceCalc] = useState(null);
    const [loadingPrice, setLoadingPrice] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [checkoutStep, setCheckoutStep] = useState('DETAILS');
    const [confirmedBookingRef, setConfirmedBookingRef] = useState('');
    const [meetingUrl, setMeetingUrl] = useState('');
    // Update topic default
    useEffect(() => {
        if (mentor && mentor.topics && mentor.topics.length > 0) {
            setSelectedTopicId(mentor.topics[0].id);
        }
    }, [mentor]);
    // Fetch server-calculated price whenever mentor or duration changes
    useEffect(() => {
        if (!mentor)
            return;
        const fetchCalculation = async () => {
            setLoadingPrice(true);
            try {
                const res = await api.get(`/bookings/calculate?hourlyRate=${mentor.hourlyRate}&durationMinutes=${duration}`);
                if (res.data?.success && res.data.data) {
                    setPriceCalc(res.data.data);
                }
            }
            catch {
                // Fallback calculation for display
                const frac = duration / 60;
                const sPrice = Math.round(mentor.hourlyRate * frac);
                const pFee = Math.round(sPrice * 0.2);
                setPriceCalc({
                    hourlyRate: mentor.hourlyRate,
                    durationMinutes: duration,
                    sessionPrice: sPrice,
                    platformFee: pFee,
                    commissionPercent: 20,
                    totalAmount: sPrice,
                    mentorEarnings: sPrice - pFee,
                });
            }
            finally {
                setLoadingPrice(false);
            }
        };
        fetchCalculation();
    }, [mentor, duration]);
    if (!mentor)
        return null;
    const handleStartBooking = async () => {
        if (!user) {
            showToast('Please log in to book a mentor session', 'error');
            return;
        }
        if (user.id === mentor.userId) {
            showToast('You cannot book a session with yourself', 'error');
            return;
        }
        try {
            setIsSubmitting(true);
            // Step 1: Create booking on backend
            const bookingRes = await api.post('/bookings', {
                mentorId: mentor.id,
                topicId: selectedTopicId,
                bookingDate: date,
                startTime: time + ':00',
                durationMinutes: duration,
                meetingNotes: notes,
            });
            if (!bookingRes.data?.success || !bookingRes.data.data) {
                throw new Error(bookingRes.data?.message || 'Failed to create booking');
            }
            const booking = bookingRes.data.data;
            setConfirmedBookingRef(booking.bookingRef);
            setMeetingUrl(booking.meetingUrl || '');
            // Check if session is free (₹0) and auto-confirmed
            if (booking.status === 'CONFIRMED' || Number(booking.totalAmount) === 0) {
                setCheckoutStep('SUCCESS');
                showToast('Free session confirmed! Meeting room created.', 'success');
                if (onSuccess)
                    onSuccess();
                return;
            }
            // Step 2: Initialize payment order for paid sessions
            const orderRes = await api.post('/payments/create', { bookingId: booking.id });
            if (!orderRes.data?.success || !orderRes.data.data) {
                throw new Error('Failed to create payment order');
            }
            const order = orderRes.data.data;
            // Step 3: Simulate Razorpay checkout with verifiable signature
            setCheckoutStep('PAYMENT');
            // Auto-simulate quick verification for demo fluidity
            setTimeout(async () => {
                try {
                    const paymentId = 'pay_' + Math.random().toString(36).substring(2, 12);
                    const signature = 'sim_sig_' + Math.random().toString(36).substring(2, 12);
                    const verifyRes = await api.post('/payments/verify', {
                        bookingId: booking.id,
                        orderId: order.orderId,
                        paymentId,
                        signature,
                    });
                    if (verifyRes.data?.success) {
                        setCheckoutStep('SUCCESS');
                        showToast('Payment verified! Session confirmed.', 'success');
                        if (onSuccess)
                            onSuccess();
                    }
                }
                catch (err) {
                    showToast(err.response?.data?.message || 'Payment verification failed', 'error');
                }
            }, 1800);
        }
        catch (err) {
            const msg = err.response?.data?.message || err.message || 'Booking conflict or error occurred';
            showToast(msg, 'error');
        }
        finally {
            setIsSubmitting(false);
        }
    };
    const handleReset = () => {
        setCheckoutStep('DETAILS');
        onClose();
    };
    const mentorName = mentor.name || mentor.user?.fullName || 'Mentor';
    const mentorHeadline = mentor.headline || mentor.user?.headline;
    const isFree = Number(mentor.hourlyRate) === 0 || Number(priceCalc?.totalAmount) === 0;
    return (<Modal isOpen={isOpen} onClose={handleReset} title={checkoutStep === 'SUCCESS'
            ? 'Booking Confirmed!'
            : checkoutStep === 'PAYMENT'
                ? 'Processing Secure Payment'
                : `Book a Session with ${mentorName}`} subtitle={mentorHeadline} maxWidth="lg">
      {checkoutStep === 'DETAILS' && (<div className="space-y-4">
          {/* Topic selection */}
          {mentor.topics && mentor.topics.length > 0 && (<div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Session Topic
              </label>
              <select value={selectedTopicId} onChange={(e) => setSelectedTopicId(Number(e.target.value))} className="w-full text-xs font-medium border border-slate-200 rounded-xl p-2.5 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:outline-none">
                {mentor.topics.map((t) => (<option key={t.id} value={t.id}>
                    {t.name}
                  </option>))}
              </select>
            </div>)}

          {/* Duration Selector */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Duration
            </label>
            <div className="grid grid-cols-4 gap-2">
              {[15, 30, 45, 60].map((mins) => (<button key={mins} type="button" onClick={() => setDuration(mins)} className={`py-2 px-3 text-xs font-semibold rounded-xl border text-center transition-all cursor-pointer ${duration === mins
                    ? 'border-indigo-600 bg-indigo-50 text-indigo-700 shadow-2xs'
                    : 'border-slate-200 text-slate-600 hover:bg-slate-50'}`}>
                  {mins} mins
                </button>))}
            </div>
          </div>

          {/* Date & Time Pickers */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-slate-400"/>
                Date
              </label>
              <input type="date" value={date} min={new Date().toISOString().split('T')[0]} onChange={(e) => setDate(e.target.value)} className="w-full text-xs font-medium border border-slate-200 rounded-xl p-2.5 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"/>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1">
                <Clock className="w-3.5 h-3.5 text-slate-400"/>
                Start Time
              </label>
              <select value={time} onChange={(e) => setTime(e.target.value)} className="w-full text-xs font-medium border border-slate-200 rounded-xl p-2.5 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:outline-none">
                <option value="10:00">10:00 AM</option>
                <option value="11:00">11:00 AM</option>
                <option value="12:00">12:00 PM</option>
                <option value="14:00">02:00 PM</option>
                <option value="16:00">04:00 PM</option>
                <option value="18:00">06:00 PM</option>
                <option value="19:00">07:00 PM</option>
                <option value="20:00">08:00 PM</option>
                <option value="21:00">09:00 PM</option>
              </select>
            </div>
          </div>

          {/* Notes for mentor */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              What do you need help with? (Optional)
            </label>
            <textarea rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="e.g. I need help understanding Spring Boot pagination with JPA specifications." className="w-full text-xs border border-slate-200 rounded-xl p-2.5 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"/>
          </div>

          {/* Pricing Breakdown Card */}
          <div className="bg-slate-50 rounded-xl p-3.5 border border-slate-200 text-xs space-y-1.5">
            {isFree ? (<>
                <div className="flex justify-between text-slate-500">
                  <span>Mentor Rate</span>
                  <span className="font-bold text-emerald-600">Free (₹0/hr)</span>
                </div>
                <div className="flex justify-between text-slate-500">
                  <span>Platform Fee</span>
                  <span className="text-slate-400">₹0 (Free Session)</span>
                </div>
                <div className="pt-2 border-t border-slate-200 flex justify-between font-bold text-slate-900 text-sm">
                  <span>Total Payable Amount</span>
                  <span className="text-emerald-600 font-black">₹0 (Free)</span>
                </div>
              </>) : (<>
                <div className="flex justify-between text-slate-500">
                  <span>Mentor Rate (₹{mentor.hourlyRate}/hr × {duration}m)</span>
                  <span>₹{priceCalc?.sessionPrice || '--'}</span>
                </div>
                <div className="flex justify-between text-slate-500">
                  <span>Platform Fee (Included - 20%)</span>
                  <span>₹{priceCalc?.platformFee || '--'}</span>
                </div>
                <div className="pt-2 border-t border-slate-200 flex justify-between font-bold text-slate-900 text-sm">
                  <span>Total Payable Amount</span>
                  <span className="text-indigo-600">₹{priceCalc?.totalAmount || '--'}</span>
                </div>
              </>)}
          </div>

          {/* CTA Button */}
          <div className="flex items-center justify-end gap-2 pt-2">
            <Button variant="ghost" size="sm" onClick={onClose}>
              Cancel
            </Button>
            <Button variant="primary" size="md" onClick={handleStartBooking} isLoading={isSubmitting || loadingPrice} className={`gap-2 ${isFree ? 'bg-emerald-600 hover:bg-emerald-700 text-white border-transparent' : ''}`}>
              {isFree ? (<>
                  <CheckCircle2 className="w-4 h-4"/>
                  <span>Confirm Free Session</span>
                </>) : (<>
                  <CreditCard className="w-4 h-4"/>
                  <span>Pay ₹{priceCalc?.totalAmount || '--'} & Confirm</span>
                </>)}
            </Button>
          </div>
        </div>)}

      {checkoutStep === 'PAYMENT' && (<div className="py-8 text-center space-y-4">
          <div className="w-16 h-16 mx-auto rounded-2xl bg-indigo-50 border border-indigo-200 flex items-center justify-center text-indigo-600 animate-pulse">
            <CreditCard className="w-8 h-8"/>
          </div>
          <div>
            <h4 className="text-base font-bold text-slate-900">
              Verifying Payment with Razorpay...
            </h4>
            <p className="text-xs text-slate-500 mt-1 max-w-xs mx-auto">
              Simulating Indian banking gateway handshake & HMAC SHA-256 signature verification.
            </p>
          </div>
          <div className="flex items-center justify-center gap-2 text-xs font-semibold text-indigo-600">
            <ShieldCheck className="w-4 h-4"/>
            <span>256-bit Encrypted Transaction</span>
          </div>
        </div>)}

      {checkoutStep === 'SUCCESS' && (<div className="py-6 text-center space-y-4">
          <div className="w-16 h-16 mx-auto rounded-full bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-600">
            <CheckCircle2 className="w-8 h-8"/>
          </div>
          <div>
            <h4 className="text-lg font-bold text-slate-900">Session Confirmed!</h4>
            <p className="text-xs text-slate-500 mt-1">
              Booking Reference:{' '}
              <span className="font-mono font-bold text-slate-800">{confirmedBookingRef}</span>
            </p>
            <p className="text-xs text-slate-600 mt-2">
              A calendar invite and in-app reminder have been sent to both you and {mentorName}.
            </p>
          </div>

          <div className="p-3 bg-indigo-50/60 rounded-xl border border-indigo-100 text-xs">
              <span className="text-indigo-800 font-semibold block mb-1">In-App Mentivo Video Call:</span>
              <p className="text-indigo-700 text-[11px]">
                Your booking is registered for in-app WebRTC video calling. You can join directly from your sessions dashboard.
              </p>
          </div>

          <div className="pt-2 flex justify-center gap-3">
            <Button variant="primary" size="md" onClick={() => {
                handleReset();
                window.location.href = '/sessions?tab=bookings';
            }} className="gap-2">
              <span>View in My Sessions</span>
              <ArrowRight className="w-4 h-4"/>
            </Button>
          </div>
        </div>)}
    </Modal>);
};
export default BookingModal;
