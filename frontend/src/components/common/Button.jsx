import React from 'react';
export const Button = ({ children, variant = 'primary', size = 'md', isLoading = false, className = '', disabled, ...props }) => {
    const baseStyles = 'inline-flex items-center justify-center font-medium transition-all duration-150 rounded-xl focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer';
    const sizeStyles = {
        sm: 'px-3 py-1.5 text-xs',
        md: 'px-4 py-2 text-sm',
        lg: 'px-6 py-3 text-base',
    };
    const variantStyles = {
        primary: 'bg-indigo-600 text-white hover:bg-indigo-700 focus:ring-indigo-500 shadow-sm shadow-indigo-200',
        secondary: 'bg-slate-100 text-slate-800 hover:bg-slate-200 focus:ring-slate-400',
        outline: 'border border-slate-300 text-slate-700 bg-white hover:bg-slate-50 focus:ring-indigo-500',
        ghost: 'text-slate-600 hover:bg-slate-100 focus:ring-slate-400',
        danger: 'bg-rose-600 text-white hover:bg-rose-700 focus:ring-rose-500 shadow-sm shadow-rose-200',
        success: 'bg-emerald-600 text-white hover:bg-emerald-700 focus:ring-emerald-500 shadow-sm shadow-emerald-200',
    };
    return (<button className={`${baseStyles} ${sizeStyles[size]} ${variantStyles[variant]} ${className}`} disabled={disabled || isLoading} {...props}>
      {isLoading ? (<span className="flex items-center gap-2">
          <svg className="animate-spin h-4 w-4 text-current" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
          </svg>
          Loading...
        </span>) : (children)}
    </button>);
};
export default Button;
