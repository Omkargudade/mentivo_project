import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { useNotification } from '../../context/NotificationContext';
import api from '../../api/client';
import { Star } from 'lucide-react';
export const ReviewModal = ({ bookingId, mentorName, isOpen, onClose, onSuccess, cancelText = 'Skip for Now' }) => {
    const { showToast } = useNotification();
    const [rating, setRating] = useState(5);
    const [hoverRating, setHoverRating] = useState(0);
    const [reviewText, setReviewText] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (rating < 1 || rating > 5) {
            showToast('Please select a star rating between 1 and 5', 'error');
            return;
        }
        try {
            setIsSubmitting(true);
            const res = await api.post('/reviews', {
                bookingId,
                rating,
                reviewText: reviewText.trim(),
            });
            if (res.data?.success) {
                showToast('Thank you! Your review has been published.', 'success');
                if (onSuccess)
                    onSuccess();
                onClose();
            }
        }
        catch (err) {
            const msg = err.response?.data?.message || 'Failed to submit review';
            showToast(msg, 'error');
        }
        finally {
            setIsSubmitting(false);
        }
    };
    return (<Modal isOpen={isOpen} onClose={onClose} title={`Review Session with ${mentorName}`} subtitle="Your authentic feedback helps the community find great mentors" maxWidth="md">
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Star Rating Picker */}
        <div className="text-center py-2">
          <label className="block text-xs font-semibold text-slate-700 mb-2">
            Rate your experience
          </label>
          <div className="flex items-center justify-center gap-2">
            {[1, 2, 3, 4, 5].map((star) => (<button key={star} type="button" onMouseEnter={() => setHoverRating(star)} onMouseLeave={() => setHoverRating(0)} onClick={() => setRating(star)} className="p-1 text-slate-300 hover:scale-110 transition-transform cursor-pointer">
                <Star className={`w-7 h-7 ${(hoverRating || rating) >= star
                ? 'text-amber-400 fill-amber-400'
                : 'text-slate-300'}`}/>
              </button>))}
          </div>
          <span className="text-xs font-bold text-slate-700 mt-2 block">
            {rating === 5 && 'Exceptional — Exceeded Expectations!'}
            {rating === 4 && 'Great — Very Helpful Session'}
            {rating === 3 && 'Good — Met Expectations'}
            {rating === 2 && 'Fair — Could Be Better'}
            {rating === 1 && 'Poor — Needs Improvement'}
          </span>
        </div>

        {/* Written Review */}
        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1.5">
            Written Review (Optional)
          </label>
          <textarea rows={4} value={reviewText} onChange={(e) => setReviewText(e.target.value)} placeholder={`Share what you learned, how helpful ${mentorName} was, and what made the session valuable...`} className="w-full text-xs border border-slate-200 rounded-xl p-3 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"/>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="ghost" size="sm" type="button" onClick={onClose}>
            {cancelText}
          </Button>
          <Button variant="primary" size="md" type="submit" isLoading={isSubmitting}>
            Submit Review
          </Button>
        </div>
      </form>
    </Modal>);
};
export default ReviewModal;
