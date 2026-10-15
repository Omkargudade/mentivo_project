import React, { useState } from 'react';
const sizeClasses = {
    xs: { container: 'w-6 h-6 rounded-md', text: 'text-[10px]', icon: 'w-3.5 h-3.5' },
    sm: { container: 'w-8 h-8 rounded-lg', text: 'text-xs', icon: 'w-4 h-4' },
    md: { container: 'w-10 h-10 rounded-xl', text: 'text-sm font-semibold', icon: 'w-5 h-5' },
    lg: { container: 'w-14 h-14 rounded-2xl', text: 'text-base font-bold', icon: 'w-7 h-7' },
    xl: { container: 'w-20 h-20 rounded-2xl', text: 'text-xl font-bold', icon: 'w-10 h-10' },
    '2xl': { container: 'w-24 h-24 sm:w-28 sm:h-28 rounded-2xl sm:rounded-3xl', text: 'text-2xl font-black', icon: 'w-12 h-12' },
};
// Gender detection heuristic from common Indian & Global names if not explicitly specified
export const detectGenderFromName = (name) => {
    if (!name)
        return 'other';
    const lower = name.toLowerCase().trim();
    const femaleNames = [
        'priya', 'sneha', 'ananya', 'meera', 'tanvi', 'pooja', 'divya', 'ritu', 'neha', 'deepa',
        'kavya', 'aishwarya', 'shreya', 'anushka', 'radhika', 'swati', 'pallavi', 'rashmi', 'sunita',
        'sarah', 'emily', 'jessica', 'rachel', 'emma', 'sophia', 'olivia', 'maria', 'elena'
    ];
    const maleNames = [
        'rahul', 'rohan', 'aditya', 'vivek', 'aarav', 'karan', 'vikram', 'siddharth', 'manish',
        'arjun', 'ashok', 'amit', 'rajesh', 'suresh', 'vijay', 'anil', 'sanjay', 'deepak', 'gaurav',
        'john', 'david', 'michael', 'alex', 'daniel', 'james', 'robert', 'william', 'thomas'
    ];
    const first = lower.split(' ')[0] || '';
    if (femaleNames.some((f) => first.startsWith(f) || lower.includes(f)))
        return 'female';
    if (maleNames.some((m) => first.startsWith(m) || lower.includes(m)))
        return 'male';
    return 'other';
};
export const UserAvatar = ({ src, name = 'Mentivo User', gender, size = 'md', className = '', alt, }) => {
    const [imgError, setImgError] = useState(false);
    const effectiveGender = gender || detectGenderFromName(name);
    const { container, text } = sizeClasses[size];
    const getInitials = (n) => {
        const parts = n.trim().split(' ');
        if (parts.length >= 2) {
            return (parts[0][0] + parts[1][0]).toUpperCase();
        }
        return n.slice(0, 2).toUpperCase() || 'MU';
    };
    // If a valid image URL is provided and has not errored
    if (src && !imgError) {
        return (<img src={src} alt={alt || name} onError={() => setImgError(true)} className={`${container} object-cover border border-slate-200/80 shadow-2xs ${className}`}/>);
    }
    // Fallback: Gender-specific SVG avatar illustration
    if (effectiveGender === 'female') {
        return (<div className={`${container} bg-gradient-to-tr from-rose-100 via-pink-50 to-indigo-100 border border-rose-200 flex items-center justify-center overflow-hidden shrink-0 shadow-2xs ${className}`} title={name}>
        <svg viewBox="0 0 100 100" className="w-full h-full object-cover" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="50" cy="50" r="48" fill="#FFF1F2"/>
          <path d="M24 55C24 32 35 18 50 18C65 18 76 32 76 55C76 68 73 78 70 85C66 85 34 85 30 85C27 78 24 68 24 55Z" fill="#881337"/>
          <rect x="44" y="60" width="12" height="15" rx="3" fill="#FBCFE8"/>
          <path d="M30 88C30 75 40 70 50 70C60 70 70 75 70 88C70 94 30 94 30 88Z" fill="#E11D48"/>
          <ellipse cx="50" cy="46" rx="18" ry="20" fill="#FDF2F8" stroke="#F472B6" strokeWidth="1"/>
          <path d="M32 40C36 30 44 26 50 26C56 26 64 30 68 40C64 35 58 32 50 32C42 32 36 35 32 40Z" fill="#9F1239"/>
          <circle cx="43" cy="45" r="2.2" fill="#4C0519"/>
          <circle cx="57" cy="45" r="2.2" fill="#4C0519"/>
          <path d="M45 52C47 55 53 55 55 52" stroke="#BE123C" strokeWidth="1.8" strokeLinecap="round"/>
          <circle cx="50" cy="40" r="1.2" fill="#E11D48"/>
          <circle cx="31" cy="48" r="1.5" fill="#F59E0B"/>
          <circle cx="69" cy="48" r="1.5" fill="#F59E0B"/>
        </svg>
      </div>);
    }
    if (effectiveGender === 'male') {
        return (<div className={`${container} bg-gradient-to-tr from-sky-100 via-blue-50 to-indigo-100 border border-sky-200 flex items-center justify-center overflow-hidden shrink-0 shadow-2xs ${className}`} title={name}>
        <svg viewBox="0 0 100 100" className="w-full h-full object-cover" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="50" cy="50" r="48" fill="#F0F9FF"/>
          <path d="M30 38C30 26 38 20 50 20C62 20 70 26 70 38C70 42 68 46 68 46C65 30 35 30 32 46C32 46 30 42 30 38Z" fill="#1E293B"/>
          <rect x="44" y="60" width="12" height="15" rx="3" fill="#BAE6FD"/>
          <path d="M26 88C26 74 38 68 50 68C62 68 74 74 74 88C74 94 26 94 26 88Z" fill="#0284C7"/>
          <path d="M44 68L50 75L56 68" stroke="#FFFFFF" strokeWidth="2" strokeLinecap="round"/>
          <ellipse cx="50" cy="46" rx="17" ry="19" fill="#F8FAFC" stroke="#94A3B8" strokeWidth="1"/>
          <circle cx="43" cy="45" r="2.2" fill="#0F172A"/>
          <circle cx="57" cy="45" r="2.2" fill="#0F172A"/>
          <path d="M40 40C42 39 45 40 46 41" stroke="#334155" strokeWidth="1.5" strokeLinecap="round"/>
          <path d="M54 41C55 40 58 39 60 40" stroke="#334155" strokeWidth="1.5" strokeLinecap="round"/>
          <path d="M45 53C47 56 53 56 55 53" stroke="#0369A1" strokeWidth="1.8" strokeLinecap="round"/>
        </svg>
      </div>);
    }
    // Fallback: Initials with modern indigo gradient
    return (<div className={`${container} bg-gradient-to-tr from-indigo-600 to-violet-600 text-white flex items-center justify-center font-bold tracking-tight shadow-2xs select-none ${text} ${className}`} title={name}>
      {getInitials(name)}
    </div>);
};
export default UserAvatar;
