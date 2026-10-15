import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, Sparkles, GraduationCap, Users, CheckCircle2, ArrowRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Button from '../components/common/Button';

export const RegisterPage = () => {
    const { register } = useAuth();
    const navigate = useNavigate();

    const [fullName, setFullName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [primaryRole, setPrimaryRole] = useState('BOTH');

    const [isSuccess, setIsSuccess] = useState(false);
    const [successMessage, setSuccessMessage] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleRegisterSubmit = async (e) => {
        e.preventDefault();
        if (!fullName || !email || !password || !confirmPassword) {
            setError('Please fill in all required fields.');
            return;
        }
        if (password !== confirmPassword) {
            setError('Passwords do not match.');
            return;
        }
        if (password.length < 6) {
            setError('Password must be at least 6 characters long.');
            return;
        }

        try {
            setError('');
            setIsLoading(true);
            const res = await register({
                fullName: fullName.trim(),
                email: email.trim(),
                password,
                confirmPassword,
                primaryRole,
            });

            setIsSuccess(true);
            setSuccessMessage(res?.message || 'Account created successfully! Welcome to Mentivo.');
        } catch (err) {
            setError(err.response?.data?.message || 'Registration failed. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-[85vh] flex flex-col justify-center py-12 sm:px-6 lg:px-8 bg-gray-50/50">
            <div className="sm:mx-auto sm:w-full sm:max-w-md">
                <div className="flex justify-center">
                    <div className="w-12 h-12 rounded-2xl bg-indigo-600 flex items-center justify-center text-white shadow-md shadow-indigo-200">
                        {isSuccess ? <CheckCircle2 className="w-6 h-6 text-white" /> : <Sparkles className="w-6 h-6" />}
                    </div>
                </div>
                <h2 className="mt-4 text-center text-2xl font-bold tracking-tight text-gray-900">
                    {isSuccess ? 'Account Created Successfully!' : 'Create your Mentivo account'}
                </h2>
                <p className="mt-2 text-center text-xs text-gray-500">
                    {isSuccess ? (
                        'Your account is ready. You can now sign in.'
                    ) : (
                        <>
                            Already have an account?{' '}
                            <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-500">
                                Sign in
                            </Link>
                        </>
                    )}
                </p>
            </div>

            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-lg">
                <div className="bg-white py-8 px-6 sm:px-10 shadow-sm rounded-2xl border border-gray-100">
                    {error && (
                        <div className="mb-5 bg-rose-50 border border-rose-200 text-rose-700 text-xs p-3.5 rounded-xl flex items-center gap-2">
                            <span className="font-semibold">Error:</span> {error}
                        </div>
                    )}

                    {isSuccess ? (
                        <div className="text-center py-4 space-y-6">
                            <div className="w-16 h-16 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto border border-emerald-100">
                                <CheckCircle2 className="w-8 h-8" />
                            </div>

                            <div className="space-y-1">
                                <h3 className="text-base font-semibold text-gray-900">Welcome to Mentivo!</h3>
                                <p className="text-xs text-gray-600 max-w-sm mx-auto">
                                    {successMessage}
                                </p>
                            </div>

                            <div className="pt-2">
                                <Button
                                    type="button"
                                    variant="primary"
                                    onClick={() => navigate('/login')}
                                    className="w-full py-2.5 rounded-xl flex items-center justify-center gap-2"
                                >
                                    <span>Proceed to Sign In</span>
                                    <ArrowRight className="w-4 h-4" />
                                </Button>
                            </div>
                        </div>
                    ) : (
                        <form className="space-y-4" onSubmit={handleRegisterSubmit}>
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                                    Full Name
                                </label>
                                <div className="relative">
                                    <User className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                    <input
                                        type="text"
                                        required
                                        value={fullName}
                                        onChange={(e) => setFullName(e.target.value)}
                                        placeholder="e.g. Ananya Roy"
                                        className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                                    />
                                </div>
                            </div>

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
                                        placeholder="ananya@example.com"
                                        className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                                    />
                                </div>
                            </div>

                            {/* Role Selection */}
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-2">
                                    I want to join Mentivo as:
                                </label>
                                <div className="grid grid-cols-3 gap-2.5">
                                    <button
                                        type="button"
                                        onClick={() => setPrimaryRole('LEARNER')}
                                        className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                                            primaryRole === 'LEARNER'
                                                ? 'border-indigo-600 bg-indigo-50/60 text-indigo-900 font-semibold'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        }`}
                                    >
                                        <GraduationCap className="w-5 h-5 mx-auto mb-1 text-indigo-600" />
                                        <span className="text-xs block">Learner</span>
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() => setPrimaryRole('MENTOR')}
                                        className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                                            primaryRole === 'MENTOR'
                                                ? 'border-indigo-600 bg-indigo-50/60 text-indigo-900 font-semibold'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        }`}
                                    >
                                        <Users className="w-5 h-5 mx-auto mb-1 text-indigo-600" />
                                        <span className="text-xs block">Mentor</span>
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() => setPrimaryRole('BOTH')}
                                        className={`p-3 rounded-xl border text-center transition-all cursor-pointer ${
                                            primaryRole === 'BOTH'
                                                ? 'border-indigo-600 bg-indigo-50/60 text-indigo-900 font-semibold'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        }`}
                                    >
                                        <Sparkles className="w-5 h-5 mx-auto mb-1 text-indigo-600" />
                                        <span className="text-xs block">Both (Learn & Teach)</span>
                                    </button>
                                </div>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                <div>
                                    <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                                        Password
                                    </label>
                                    <div className="relative">
                                        <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                        <input
                                            type="password"
                                            required
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            placeholder="••••••••"
                                            className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                                        />
                                    </div>
                                </div>

                                <div>
                                    <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                                        Confirm Password
                                    </label>
                                    <div className="relative">
                                        <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                                        <input
                                            type="password"
                                            required
                                            value={confirmPassword}
                                            onChange={(e) => setConfirmPassword(e.target.value)}
                                            placeholder="••••••••"
                                            className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                                        />
                                    </div>
                                </div>
                            </div>

                            <Button
                                type="submit"
                                variant="primary"
                                isLoading={isLoading}
                                className="w-full py-2.5 mt-4 rounded-xl font-semibold shadow-sm"
                            >
                                Create Account
                            </Button>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;
