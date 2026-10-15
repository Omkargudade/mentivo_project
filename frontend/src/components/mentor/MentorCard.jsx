import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { OnlineBadge, VerificationBadge } from '../common/Badge';
import { Button } from '../common/Button';
import { UserAvatar } from '../common/UserAvatar';
import { useAuth } from '../../context/AuthContext';
import { useNotification } from '../../context/NotificationContext';
import api from '../../api/client';
import { Star, Clock, Briefcase, MessageSquare, UserPlus, Calendar } from 'lucide-react';
export const MentorCard = ({ mentor, onBookClick }) => {
    const { user } = useAuth();
    const { showToast } = useNotification();
    const [isConnecting, setIsConnecting] = useState(false);
    const [connected, setConnected] = useState(false);
    const navigate = useNavigate();
    const handleConnect = async (e) => {
        e.stopPropagation();
        if (!user) {
            navigate('/login');
            return;
        }
        if (user.id === mentor.userId) {
            showToast("You cannot connect with yourself", 'error');
            return;
        }
        try {
            setIsConnecting(true);
            const res = await api.post('/connections', {
                receiverId: mentor.userId,
                message: `Hi ${mentor.name}, I'd love to connect with you on Mentivo!`,
            });
            if (res.data?.success) {
                setConnected(true);
                showToast(`Connection request sent to ${mentor.name}!`, 'success');
            }
        }
        catch (err) {
            const msg = err.response?.data?.message || 'Failed to send connection request';
            showToast(msg, 'info');
        }
        finally {
            setIsConnecting(false);
        }
    };
    const handleMessage = async (e) => {
        e.stopPropagation();
        if (!user) {
            navigate('/login');
            return;
        }
        try {
            const res = await api.post('/conversations', { peerId: mentor.userId });
            if (res.data?.success && res.data.data) {
                navigate('/messages?convId=' + res.data.data.id);
            }
            else {
                navigate('/messages');
            }
        }
        catch {
            navigate('/messages');
        }
    };
    return (<div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs hover:shadow-md transition-all duration-200 flex flex-col justify-between group">
      <div>
        {/* Top Header: Avatar, Name, Headline, Online Pill */}
        <div className="flex items-start gap-3.5 mb-3.5">
          <div className="relative shrink-0">
            <UserAvatar src={mentor.profileImage} name={mentor.name} size="lg" className="border border-slate-200 group-hover:border-indigo-200 transition-colors"/>
            <span className="absolute -bottom-1 -right-1">
              <span className={`block w-3.5 h-3.5 rounded-full border-2 border-white ${mentor.onlineStatus === 'ONLINE' ? 'bg-emerald-500' : 'bg-slate-300'}`}/>
            </span>
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex items-center justify-between gap-1 mb-0.5">
              <Link to={`/mentors/${mentor.id}`} className="font-bold text-slate-900 hover:text-indigo-600 transition-colors text-sm truncate">
                {mentor.name}
              </Link>
              <OnlineBadge status={mentor.onlineStatus}/>
            </div>

            <p className="text-xs text-slate-500 line-clamp-1 mb-1">
              {mentor.headline || 'Knowledge Mentor'}
            </p>

            <div className="flex items-center gap-2 flex-wrap">
              <VerificationBadge level={mentor.verificationLevel}/>
              {mentor.location && (<span className="text-[11px] text-slate-400 truncate max-w-[120px]">
                  {mentor.location}
                </span>)}
            </div>
          </div>
        </div>

        {/* Bio excerpt */}
        {mentor.bio && (<p className="text-xs text-slate-600 line-clamp-2 mb-3 leading-relaxed">
            {mentor.bio}
          </p>)}

        {/* Key Metrics row: Rating, Experience, Response Time */}
        <div className="grid grid-cols-3 gap-2 py-2.5 px-3 bg-slate-50/80 rounded-xl mb-3 text-center border border-slate-100">
          <div>
            <div className="flex items-center justify-center gap-1 text-xs font-bold text-slate-900">
              <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-400"/>
              <span>{mentor.averageRating > 0 ? mentor.averageRating.toFixed(1) : 'New'}</span>
            </div>
            <p className="text-[10px] text-slate-400">{mentor.totalReviews} reviews</p>
          </div>

          <div className="border-x border-slate-200">
            <div className="flex items-center justify-center gap-1 text-xs font-bold text-slate-900">
              <Briefcase className="w-3.5 h-3.5 text-slate-400"/>
              <span>{mentor.experienceYears}y</span>
            </div>
            <p className="text-[10px] text-slate-400">Experience</p>
          </div>

          <div>
            <div className="flex items-center justify-center gap-1 text-xs font-bold text-slate-900">
              <Clock className="w-3.5 h-3.5 text-slate-400"/>
              <span>{mentor.responseTimeMinutes}m</span>
            </div>
            <p className="text-[10px] text-slate-400">Response</p>
          </div>
        </div>

        {/* Top Skills Tags */}
        <div className="flex flex-wrap gap-1.5 mb-4">
          {mentor.skills?.slice(0, 3).map((skill) => (<span key={skill.id} className="px-2 py-0.5 rounded-md text-[11px] font-medium bg-slate-100 text-slate-700 hover:bg-indigo-50 hover:text-indigo-700 transition-colors">
              {skill.name}
            </span>))}
          {mentor.topics?.slice(0, 2).map((topic) => (<span key={topic.id} className="px-2 py-0.5 rounded-md text-[11px] font-medium bg-indigo-50/70 text-indigo-700">
              #{topic.name}
            </span>))}
        </div>
      </div>

      {/* Bottom Footer: Price & Action Buttons */}
      <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
        <div>
          {Number(mentor.hourlyRate) === 0 ? (<div className="flex items-center gap-1.5">
              <span className="text-base font-black text-emerald-600">Free</span>
              <span className="px-1.5 py-0.5 rounded-md text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                ₹0/hr
              </span>
            </div>) : (<div>
              <span className="text-base font-black text-slate-900">₹{mentor.hourlyRate}</span>
              <span className="text-[11px] text-slate-500 font-normal">/hour</span>
            </div>)}
        </div>

        <div className="flex items-center gap-1.5">
          <button onClick={handleConnect} disabled={connected || isConnecting} className={`p-2 rounded-xl border text-xs font-medium transition-colors cursor-pointer ${connected
            ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
            : 'border-slate-200 text-slate-600 hover:bg-slate-100'}`} title="Connect">
            <UserPlus className="w-4 h-4"/>
          </button>

          <button onClick={handleMessage} className="p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer" title="Message">
            <MessageSquare className="w-4 h-4"/>
          </button>

          <Link to={`/mentors/${mentor.id}`}>
            <Button size="sm" variant="outline" className="px-2.5 text-xs">
              View
            </Button>
          </Link>

          {onBookClick && (<Button size="sm" variant={Number(mentor.hourlyRate) === 0 ? "secondary" : "primary"} onClick={() => onBookClick(mentor)} className={`px-3 text-xs gap-1 ${Number(mentor.hourlyRate) === 0
                ? 'bg-emerald-600 hover:bg-emerald-700 text-white border-transparent'
                : ''}`}>
              <Calendar className="w-3.5 h-3.5"/>
              {Number(mentor.hourlyRate) === 0 ? 'Free' : 'Book'}
            </Button>)}
        </div>
      </div>
    </div>);
};
