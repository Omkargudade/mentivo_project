import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, CheckCircle2, Sparkles, KeyRound } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Button from '../components/common/Button';

export const ForgotPasswordPage = () => {
    const { forgotPassword } = useAuth();
    const [email, setEmail] = useState('');
    const [submitted, setSubmitted] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        const trimmedEmail = email.trim();
        if (!trimmedEmail) {
            setError('Please enter your email address.');
            return;
        }

        try {
            setError('');
            setIsLoading(true);
            const res = await forgotPassword(trimmedEmail);
            setMessage(res?.message || 'If an account exists with this email, a password reset link has been sent.');
            setSubmitted(true);
        } catch (err) {
            // Even if an unexpected error occurs, show a safe user-friendly message
            const apiError = err.response?.data?.message;
            if (apiError) {
                setError(apiError);
            } else {
                setError('Unable to send password reset link right now. Please try again later.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-[80vh] flex flex-col justify-center py-12 sm:px-6 lg:px-8 bg-slate-50">
            <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
                <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-100 text-indigo-600 mb-4 shadow-xs">
                    <KeyRound className="w-6 h-6" />
                </div>
                <h2 className="text-2xl font-bold tracking-tight text-gray-900 sm:text-3xl">
                    Forgot Password?
                </h2>
                <p className="mt-2 text-xs sm:text-sm text-gray-500 max-w-sm mx-auto">
                    Enter your registered email address and we'll send you a secure link to reset your password.
                </p>
            </div>

            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
                <div className="bg-white py-8 px-6 sm:px-10 shadow-xs rounded-2xl border border-gray-100">
                    {submitted ? (
                        <div className="text-center py-4">
                            <div className="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-4 border border-emerald-100">
                                <CheckCircle2 className="w-6 h-6" />
                            </div>
                            <h3 className="text-base font-semibold text-gray-900 mb-2">Check Your Email</h3>
                            <p className="text-xs text-gray-600 mb-6 leading-relaxed">
                                {message}
                            </p>
                            <div className="bg-amber-50/70 border border-amber-200/60 rounded-xl p-3 text-left mb-6">
                                <p className="text-[11px] text-amber-800">
                                    <span className="font-semibold">Security Note:</span> Reset links expire in 15 minutes. If you do not see the email in your inbox within a couple of minutes, check your spam or junk folder.
                                </p>
                            </div>
                            <div className="space-y-3">
                                <Button
                                    type="button"
                                    variant="outline"
                                    onClick={() => { setSubmitted(false); setEmail(''); }}
                                    className="w-full py-2.5 text-xs rounded-xl"
                                >
                                    Try Another Email
                                </Button>
                                <Link
                                    to="/login"
                                    className="inline-flex items-center justify-center w-full text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition-colors py-2"
                                >
                                    <ArrowLeft className="w-3.5 h-3.5 mr-1.5" /> Back to Sign In
                                </Link>
                            </div>
                        </div>
                    ) : (
                        <form className="space-y-4" onSubmit={handleSubmit}>
                            {error && (
                                <div className="bg-rose-50 border border-rose-200 text-rose-700 text-xs p-3.5 rounded-xl flex items-center gap-2">
                                    <span className="font-semibold">Error:</span> {error}
                                </div>
                            )}

                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                                    Email Address
                                </label>
                                <div className="relative">
                                    <Mail className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                    <input
                                        type="email"
                                        required
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        placeholder="name@example.com"
                                        className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                                    />
                                </div>
                            </div>

                            <Button
                                type="submit"
                                variant="primary"
                                isLoading={isLoading}
                                className="w-full py-2.5 mt-2 rounded-xl"
                            >
                                Send Reset Link
                            </Button>

                            <div className="pt-4 text-center border-t border-gray-100">
                                <Link
                                    to="/login"
                                    className="inline-flex items-center text-xs font-medium text-gray-500 hover:text-gray-900 transition-colors"
                                >
                                    <ArrowLeft className="w-3.5 h-3.5 mr-1.5" /> Return to Sign In
                                </Link>
                            </div>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ForgotPasswordPage;
