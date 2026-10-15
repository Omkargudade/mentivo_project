import React from 'react';
import { useCall } from '../../context/CallContext';
import UserAvatar from '../common/UserAvatar';
import { Video, PhoneOff, PhoneCall } from 'lucide-react';
export const IncomingCallModal = () => {
    const { incomingCall, acceptCall, rejectCall, callStatus } = useCall();
    // Strictly check that callStatus is INCOMING
    if (callStatus !== 'INCOMING' || !incomingCall)
        return null;
    return (<div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/70 backdrop-blur-md p-4 animate-in fade-in duration-200">
      <div className="w-full max-w-sm bg-white rounded-3xl shadow-2xl border border-slate-100 p-8 text-center relative overflow-hidden">
        {/* Animated Background Ring Accent */}
        <div className="absolute -top-12 -left-12 w-36 h-36 bg-indigo-100 rounded-full blur-2xl pointer-events-none opacity-60 animate-pulse"/>
        <div className="absolute -bottom-12 -right-12 w-36 h-36 bg-emerald-100 rounded-full blur-2xl pointer-events-none opacity-60 animate-pulse"/>

        {/* Pulsing Avatar */}
        <div className="relative mx-auto w-24 h-24 mb-5">
          <div className="absolute inset-0 rounded-full bg-indigo-500/20 animate-ping"/>
          <div className="relative w-24 h-24 rounded-full ring-4 ring-indigo-500/30 overflow-hidden shadow-lg mx-auto flex items-center justify-center bg-slate-100">
            <UserAvatar src={incomingCall.callerImage} name={incomingCall.callerName} size="xl" className="w-full h-full object-cover"/>
          </div>
        </div>

        {/* Call Info */}
        <div className="space-y-1 mb-6">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-bold uppercase tracking-wider">
            <Video className="w-3.5 h-3.5 text-indigo-600"/>
            {incomingCall.sessionTitle ? `Session: ${incomingCall.sessionTitle}` : '1-on-1 Video Call'}
          </span>
          <h3 className="text-xl font-black text-slate-900 mt-2 tracking-tight">
            {incomingCall.callerName}
          </h3>
          <p className="text-xs text-slate-500 font-medium">
            {incomingCall.sessionTitle ? 'is starting your mentorship session on Mentivo...' : 'is calling you on Mentivo...'}
          </p>
        </div>

        {/* Action Buttons (Touch friendly min 56x56px) */}
        <div className="flex items-center justify-center gap-8 pt-2">
          {/* Decline Button */}
          <div className="text-center">
            <button onClick={rejectCall} className="w-16 h-16 min-w-[64px] min-h-[64px] rounded-full bg-rose-500 hover:bg-rose-600 active:bg-rose-700 text-white flex items-center justify-center shadow-lg shadow-rose-500/30 transition-all hover:scale-105 active:scale-95 cursor-pointer mx-auto touch-manipulation" title="Decline Call">
              <PhoneOff className="w-7 h-7"/>
            </button>
            <span className="text-xs font-semibold text-slate-500 mt-2 block">
              Decline
            </span>
          </div>

          {/* Accept Button */}
          <div className="text-center">
            <button onClick={acceptCall} className="w-16 h-16 min-w-[64px] min-h-[64px] rounded-full bg-emerald-500 hover:bg-emerald-600 active:bg-emerald-700 text-white flex items-center justify-center shadow-lg shadow-emerald-500/30 transition-all hover:scale-105 active:scale-95 animate-bounce cursor-pointer mx-auto touch-manipulation" title="Accept Call">
              <PhoneCall className="w-7 h-7"/>
            </button>
            <span className="text-xs font-bold text-slate-800 mt-2 block">
              Accept
            </span>
          </div>
        </div>
      </div>
    </div>);
};
export default IncomingCallModal;
