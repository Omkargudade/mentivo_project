import React, { useEffect } from 'react';
import { X } from 'lucide-react';
export const Modal = ({ isOpen, onClose, title, subtitle, children, maxWidth = 'md', }) => {
    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key === 'Escape')
                onClose();
        };
        if (isOpen) {
            document.body.style.overflow = 'hidden';
            window.addEventListener('keydown', handleKeyDown);
        }
        return () => {
            document.body.style.overflow = 'unset';
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [isOpen, onClose]);
    if (!isOpen)
        return null;
    const maxWidthStyles = {
        sm: 'max-w-sm',
        md: 'max-w-md',
        lg: 'max-w-lg',
        xl: 'max-w-2xl',
    };
    return (<div className="fixed inset-0 z-50 overflow-y-auto">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs transition-opacity" onClick={onClose}/>

      {/* Modal Card */}
      <div className="flex min-h-full items-center justify-center p-4 text-center">
        <div className={`w-full ${maxWidthStyles[maxWidth]} transform overflow-hidden rounded-2xl bg-white p-6 text-left align-middle shadow-2xl transition-all border border-slate-100`}>
          <div className="flex items-start justify-between pb-4 border-b border-slate-100">
            <div>
              <h3 className="text-lg font-bold text-slate-900 leading-6">{title}</h3>
              {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
            </div>
            <button onClick={onClose} className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors">
              <X className="w-5 h-5"/>
            </button>
          </div>

          <div className="mt-4">{children}</div>
        </div>
      </div>
    </div>);
};
export default Modal;
