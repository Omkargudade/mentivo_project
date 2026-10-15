import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Lock, CheckCircle2, AlertCircle, ArrowLeft, KeyRound, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Button from '../components/common/Button';

export const ResetPasswordPage = () => {
    const { resetPassword } = useAuth();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token') || '';

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!token) {
            setError('Missing password reset token. Please request a new reset link.');
            return;
        }
        if (!newPassword || !confirmPassword) {
            setError('Please fill in both password fields.');
            return;
        }
        if (newPassword.length < 6) {
            setError('Password must be at least 6 characters long.');
            return;
        }
        if (newPassword !== confirmPassword) {
            setError('Passwords do not match.');
            return;
        }

        try {
            setError('');
            setIsLoading(true);
            await resetPassword(token, newPassword, confirmPassword);
            setSuccess(true);
        } catch (err) {
            const apiError = err.response?.data?.message;
            if (apiError) {
                setError(apiError);
            } else {
                setError('Failed to reset password. The link may have expired or already been used.');
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
                    Set New Password
                </h2>
                <p className="mt-2 text-xs sm:text-sm text-gray-500 max-w-sm mx-auto">
                    Please create a strong new password for your Mentivo account.
                </p>
            </div>

            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
                <div className="bg-white py-8 px-6 sm:px-10 shadow-xs rounded-2xl border border-gray-100">
                    {success ? (
                        <div className="text-center py-4">
                            <div className="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-4 border border-emerald-100">
                                <CheckCircle2 className="w-6 h-6" />
                            </div>
                            <h3 className="text-base font-semibold text-gray-900 mb-2">Password Reset Complete</h3>
                            <p className="text-xs text-gray-600 mb-6 leading-relaxed">
                                Your account password has been updated securely. You can now sign in with your new credentials.
                            </p>
                            <Button
                                type="button"
                                variant="primary"
                                onClick={() => navigate('/login')}
                                className="w-full py-2.5 text-xs rounded-xl"
                            >
                                Continue to Sign In
                            </Button>
                        </div>
                    ) : !token ? (
                        <div className="text-center py-4">
                            <div className="w-12 h-12 bg-rose-50 text-rose-600 rounded-full flex items-center justify-center mx-auto mb-4 border border-rose-100">
                                <AlertCircle className="w-6 h-6" />
                            </div>
                            <h3 className="text-base font-semibold text-gray-900 mb-2">Invalid or Missing Reset Link</h3>
                            <p className="text-xs text-gray-600 mb-6 leading-relaxed">
                                This password reset link is invalid or incomplete. Please request a new password reset email.
                            </p>
                            <Link to="/forgot-password">
                                <Button variant="primary" className="w-full py-2.5 text-xs rounded-xl">
                                    Request New Reset Link
                                </Button>
                            </Link>
                        </div>
                    ) : (
                        <form className="space-y-4" onSubmit={handleSubmit}>
                            {error && (
                                <div className="bg-rose-50 border border-rose-200 text-rose-700 text-xs p-3.5 rounded-xl flex items-center gap-2">
                                    <AlertCircle className="w-4 h-4 shrink-0" />
                                    <span>{error}</span>
                                </div>
                            )}

                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                                    New Password
                                </label>
                                <div className="relative">
                                    <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                    <input
                                        type={showPassword ? 'text' : 'password'}
                                        required
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
                                        placeholder="••••••••"
                                        className="w-full pl-10 pr-10 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword(!showPassword)}
                                        className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                                    >
                                        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                    </button>
                                </div>
                                <p className="mt-1 text-[11px] text-gray-500">Minimum 6 characters</p>
                            </div>

                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                                    Confirm New Password
                                </label>
                                <div className="relative">
                                    <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                    <input
                                        type={showPassword ? 'text' : 'password'}
                                        required
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        placeholder="••••••••"
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
                                Reset Password
                            </Button>

                            <div className="pt-4 text-center border-t border-gray-100">
                                <Link
                                    to="/login"
                                    className="inline-flex items-center text-xs font-medium text-gray-500 hover:text-gray-900 transition-colors"
                                >
                                    <ArrowLeft className="w-3.5 h-3.5 mr-1.5" /> Back to Sign In
                                </Link>
                            </div>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ResetPasswordPage;
