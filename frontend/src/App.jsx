import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import { SocketProvider } from './context/SocketContext';
import { FeatureFlagProvider } from './context/FeatureFlagContext';
import { CallProvider, useCall } from './context/CallContext';
import { IncomingCallModal } from './components/video/IncomingCallModal';
import { VideoCallModal } from './components/video/VideoCallModal';
import { ReviewModal } from './components/review/ReviewModal';
import Navbar from './components/layout/Navbar';
import Footer from './components/layout/Footer';
// Pages
import LandingPage from './pages/LandingPage';
import MentorDiscoveryPage from './pages/MentorDiscoveryPage';
import MentorProfilePage from './pages/MentorProfilePage';
import LearnerDashboardPage from './pages/LearnerDashboardPage';
import MentorDashboardPage from './pages/MentorDashboardPage';
import MyConnectionsPage from './pages/MyConnectionsPage';
import MessagesPage from './pages/MessagesPage';
import SessionsPage from './pages/SessionsPage';
import CategoriesPage from './pages/CategoriesPage';
import ProfileSettingsPage from './pages/ProfileSettingsPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
const ProtectedRoute = ({ children, requiredRole, }) => {
    const { user, token, isLoading } = useAuth();
    const location = useLocation();
    if (isLoading) {
        return (<div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>);
    }
    if (!token || !user) {
        return <Navigate to="/login" state={{ from: location }} replace/>;
    }
    if (requiredRole === 'ADMIN' && user.primaryRole !== 'ADMIN') {
        return <Navigate to="/dashboard" replace/>;
    }
    return <>{children}</>;
};
const AppRoutes = () => {
    const { currentMode } = useAuth();
    return (<div className="min-h-screen flex flex-col bg-slate-50 font-sans antialiased text-slate-900">
      <Navbar />
      <main className="flex-1">
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<LandingPage />}/>
          <Route path="/mentors" element={<MentorDiscoveryPage />}/>
          <Route path="/mentors/:id" element={<MentorProfilePage />}/>
          <Route path="/categories" element={<CategoriesPage />}/>
          <Route path="/sessions" element={<SessionsPage />}/>
          <Route path="/bookings" element={<SessionsPage defaultTab="BOOKINGS" />}/>
          <Route path="/my-bookings" element={<SessionsPage defaultTab="BOOKINGS" />}/>
          <Route path="/how-it-works" element={<LandingPage />}/>
          <Route path="/login" element={<LoginPage />}/>
          <Route path="/register" element={<RegisterPage />}/>
          <Route path="/forgot-password" element={<ForgotPasswordPage />}/>
          <Route path="/reset-password" element={<ResetPasswordPage />}/>

          {/* Dynamic Dashboard based on Mode */}
          <Route path="/dashboard" element={<ProtectedRoute>
                {currentMode === 'MENTOR' ? <MentorDashboardPage /> : <LearnerDashboardPage />}
              </ProtectedRoute>}/>
          <Route path="/mentor/dashboard" element={<ProtectedRoute>
                <MentorDashboardPage />
              </ProtectedRoute>}/>

          {/* Authenticated Application Routes */}
          <Route path="/connections" element={<ProtectedRoute>
                <MyConnectionsPage />
              </ProtectedRoute>}/>
          <Route path="/messages" element={<ProtectedRoute>
                <MessagesPage />
              </ProtectedRoute>}/>
          <Route path="/profile" element={<ProtectedRoute>
                <ProfileSettingsPage />
              </ProtectedRoute>}/>

          {/* Admin Route */}
          <Route path="/admin" element={<ProtectedRoute requiredRole="ADMIN">
                <AdminDashboardPage />
              </ProtectedRoute>}/>

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/" replace/>}/>
        </Routes>
      </main>
      <Footer />
    </div>);
};
const PostCallReviewModal = () => {
    const { pendingReviewSession, clearPendingReviewSession } = useCall();
    if (!pendingReviewSession) return null;
    return (
        <ReviewModal
            isOpen={Boolean(pendingReviewSession)}
            bookingId={pendingReviewSession.bookingId}
            mentorName={pendingReviewSession.mentorName}
            onClose={clearPendingReviewSession}
            onSuccess={clearPendingReviewSession}
            cancelText="Skip for Now"
        />
    );
};

function App() {
    return (<Router>
      <AuthProvider>
        <NotificationProvider>
          <SocketProvider>
            <FeatureFlagProvider>
              <CallProvider>
                <AppRoutes />
                <IncomingCallModal />
                <VideoCallModal />
                <PostCallReviewModal />
              </CallProvider>
            </FeatureFlagProvider>
          </SocketProvider>
        </NotificationProvider>
      </AuthProvider>
    </Router>);
}
export default App;
