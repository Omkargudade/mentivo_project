import React from 'react';
import { CheckCircle2, ShieldCheck, Award } from 'lucide-react';
export const OnlineBadge = ({ status }) => {
    const isOnline = status === 'ONLINE';
    return (<span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${isOnline
            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
            : 'bg-slate-100 text-slate-500 border border-slate-200'}`}>
      <span className={`w-2 h-2 rounded-full ${isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'}`}/>
      {isOnline ? 'Available Now' : 'Offline'}
    </span>);
};
export const VerificationBadge = ({ level }) => {
    if (level === 'UNVERIFIED')
        return null;
    const config = {
        IDENTITY_VERIFIED: {
            label: 'ID Verified',
            icon: CheckCircle2,
            style: 'bg-blue-50 text-blue-700 border-blue-200',
        },
        SKILL_VERIFIED: {
            label: 'Skill Verified',
            icon: ShieldCheck,
            style: 'bg-indigo-50 text-indigo-700 border-indigo-200',
        },
        EXPERT_VERIFIED: {
            label: 'Expert Verified',
            icon: Award,
            style: 'bg-amber-50 text-amber-800 border-amber-300',
        },
    }[level];
    if (!config)
        return null;
    const Icon = config.icon;
    return (<span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium border ${config.style}`} title={config.label}>
      <Icon className="w-3.5 h-3.5"/>
      <span>{config.label}</span>
    </span>);
};
export const BookingBadge = ({ status }) => {
    const styles = {
        PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
        CONFIRMED: 'bg-blue-50 text-blue-700 border-blue-200',
        IN_PROGRESS: 'bg-purple-50 text-purple-700 border-purple-200 animate-pulse',
        COMPLETED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        CANCELLED: 'bg-rose-50 text-rose-700 border-rose-200',
        NO_SHOW: 'bg-slate-100 text-slate-600 border-slate-200',
    };
    const labels = {
        PENDING: 'Payment Pending',
        CONFIRMED: 'Confirmed',
        IN_PROGRESS: 'In Session',
        COMPLETED: 'Completed',
        CANCELLED: 'Cancelled',
        NO_SHOW: 'No Show',
    };
    return (<span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${styles[status]}`}>
      {labels[status]}
    </span>);
};
export const Badge = ({ children, variant = 'default', className = '' }) => {
    const variantStyles = {
        default: 'bg-gray-100 text-gray-700 border-gray-200',
        primary: 'bg-indigo-50 text-indigo-700 border-indigo-200',
        secondary: 'bg-slate-100 text-slate-700 border-slate-200',
        success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        warning: 'bg-amber-50 text-amber-800 border-amber-200',
        danger: 'bg-rose-50 text-rose-700 border-rose-200',
        online: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        offline: 'bg-gray-100 text-gray-500 border-gray-200',
    };
    return (<span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${variantStyles[variant]} ${className}`}>
      {variant === 'online' && <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"/>}
      {variant === 'offline' && <span className="w-1.5 h-1.5 rounded-full bg-gray-400"/>}
      {children}
    </span>);
};
export default Badge;
