import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Mail, Lock, ArrowRight, Sparkles, Shield, User, GraduationCap } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Button from '../components/common/Button';
export const LoginPage = () => {
    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const from = location.state?.from?.pathname || '/dashboard';
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!email || !password) {
            setError('Please fill in both email and password.');
            return;
        }
        try {
            setError('');
            setIsLoading(true);
            await login(email, password);
            navigate(from, { replace: true });
        }
        catch (err) {
            setError(err.response?.data?.message || 'Invalid email or password.');
        }
        finally {
            setIsLoading(false);
        }
    };
    const handleDemoLogin = async (demoEmail, demoPass) => {
        setEmail(demoEmail);
        setPassword(demoPass);
        try {
            setError('');
            setIsLoading(true);
            await login(demoEmail, demoPass);
            navigate(from, { replace: true });
        }
        catch (err) {
            setError(err.response?.data?.message || 'Demo login failed.');
        }
        finally {
            setIsLoading(false);
        }
    };
    return (<div className="min-h-[85vh] flex flex-col justify-center py-12 sm:px-6 lg:px-8 bg-gray-50/50">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <div className="w-12 h-12 rounded-2xl bg-indigo-600 flex items-center justify-center text-white shadow-md shadow-indigo-200">
            <Sparkles className="w-6 h-6"/>
          </div>
        </div>
        <h2 className="mt-4 text-center text-2xl font-bold tracking-tight text-gray-900">
          Sign in to Mentivo
        </h2>
        <p className="mt-2 text-center text-xs text-gray-500">
          Or{' '}
          <Link to="/register" className="font-semibold text-indigo-600 hover:text-indigo-500">
            create a new account
          </Link>
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 sm:px-10 shadow-sm rounded-2xl border border-gray-100">
          {error && (<div className="mb-5 bg-rose-50 border border-rose-200 text-rose-700 text-xs p-3.5 rounded-xl flex items-center gap-2">
              <span className="font-semibold">Error:</span> {error}
            </div>)}

          <form className="space-y-4" onSubmit={handleSubmit}>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                Email Address
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2"/>
                <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@example.com" className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:bg-white"/>
              </div>
            </div>

            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="block text-xs font-semibold text-gray-700">
                  Password
                </label>
                <Link to="/forgot-password" className="text-[11px] font-medium text-indigo-600 hover:underline">
                  Forgot?
                </Link>
              </div>
              <div className="relative">
                <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2"/>
                <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:bg-white"/>
              </div>
            </div>

            <Button type="submit" variant="primary" isLoading={isLoading} className="w-full py-2.5 mt-2 rounded-xl">
              Sign In
            </Button>
          </form>

          {/* Quick Demo Logins */}
          <div className="mt-8 pt-6 border-t border-gray-100">
            <p className="text-center text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-3">
              ⚡ Instant Demo Profiles
            </p>
            <div className="space-y-2">
              <button type="button" onClick={() => handleDemoLogin('priya.learner@example.com', 'Password@123')} className="w-full flex items-center justify-between p-2.5 bg-slate-50 hover:bg-slate-100 rounded-xl text-xs text-slate-700 transition-colors border border-slate-200/60 cursor-pointer">
                <div className="flex items-center gap-2.5">
                  <div className="w-7 h-7 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                    <GraduationCap className="w-4 h-4"/>
                  </div>
                  <div className="text-left">
                    <span className="font-semibold block text-slate-900">Priya Learner</span>
                    <span className="text-[10px] text-slate-500">Learning Mode</span>
                  </div>
                </div>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400"/>
              </button>

              <button type="button" onClick={() => handleDemoLogin('rahul.sharma@example.com', 'Password@123')} className="w-full flex items-center justify-between p-2.5 bg-indigo-50/60 hover:bg-indigo-100/70 rounded-xl text-xs text-indigo-900 transition-colors border border-indigo-100 cursor-pointer">
                <div className="flex items-center gap-2.5">
                  <div className="w-7 h-7 rounded-lg bg-indigo-600 text-white flex items-center justify-center font-bold">
                    <User className="w-4 h-4"/>
                  </div>
                  <div className="text-left">
                    <span className="font-semibold block text-indigo-950">Rahul Sharma</span>
                    <span className="text-[10px] text-indigo-700">Top Mentor (Java & Spring)</span>
                  </div>
                </div>
                <ArrowRight className="w-3.5 h-3.5 text-indigo-500"/>
              </button>

              <button type="button" onClick={() => handleDemoLogin('admin@mentivo.com', 'Admin@123')} className="w-full flex items-center justify-between p-2.5 bg-purple-50/60 hover:bg-purple-100/70 rounded-xl text-xs text-purple-900 transition-colors border border-purple-100 cursor-pointer">
                <div className="flex items-center gap-2.5">
                  <div className="w-7 h-7 rounded-lg bg-purple-600 text-white flex items-center justify-center font-bold">
                    <Shield className="w-4 h-4"/>
                  </div>
                  <div className="text-left">
                    <span className="font-semibold block text-purple-950">Super Admin</span>
                    <span className="text-[10px] text-purple-700">Platform Governance</span>
                  </div>
                </div>
                <ArrowRight className="w-3.5 h-3.5 text-purple-500"/>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>);
};
export default LoginPage;
