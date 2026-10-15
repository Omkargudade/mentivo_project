import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Users, UserCheck, Clock, MessageSquare, Check, X, Search, UserPlus } from 'lucide-react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
export const MyConnectionsPage = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [connections, setConnections] = useState([]);
    const [pendingRequests, setPendingRequests] = useState([]);
    const [activeTab, setActiveTab] = useState('active');
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        fetchConnections();
    }, []);
    const fetchConnections = async () => {
        try {
            setLoading(true);
            const [connRes, pendRes] = await Promise.all([
                api.get('/connections'),
                api.get('/connections/pending'),
            ]);
            if (connRes.data?.success && connRes.data.data) {
                setConnections(connRes.data.data);
            }
            if (pendRes.data?.success && pendRes.data.data) {
                setPendingRequests(pendRes.data.data);
            }
        }
        catch (err) {
            console.error('Failed to load connections', err);
        }
        finally {
            setLoading(false);
        }
    };
    const handleAccept = async (id) => {
        try {
            await api.patch(`/connections/${id}/accept`);
            fetchConnections();
        }
        catch (err) {
            console.error('Failed to accept connection', err);
        }
    };
    const handleReject = async (id) => {
        try {
            await api.patch(`/connections/${id}/reject`);
            fetchConnections();
        }
        catch (err) {
            console.error('Failed to reject connection', err);
        }
    };
    const filteredActive = connections.filter((conn) => {
        const peer = conn.sender.id === user?.id ? conn.receiver : conn.sender;
        return peer.fullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
            peer.email.toLowerCase().includes(searchQuery.toLowerCase());
    });
    return (<div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Network & Connections</h1>
          <p className="text-sm text-gray-500 mt-1">
            Build meaningful relationships with mentors and fellow learners across Mentivo.
          </p>
        </div>

        <Link to="/mentors">
          <Button variant="primary" className="flex items-center gap-2">
            <UserPlus className="w-4 h-4"/> Find Mentors
          </Button>
        </Link>
      </div>

      {/* Tabs and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 border-b border-gray-200 pb-4 mb-6">
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <button onClick={() => setActiveTab('active')} className={`px-4 py-2 text-sm font-semibold rounded-lg transition-colors flex items-center gap-2 ${activeTab === 'active'
            ? 'bg-indigo-50 text-indigo-700'
            : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'}`}>
            <UserCheck className="w-4 h-4"/>
            Connected ({connections.length})
          </button>
          <button onClick={() => setActiveTab('pending')} className={`px-4 py-2 text-sm font-semibold rounded-lg transition-colors flex items-center gap-2 ${activeTab === 'pending'
            ? 'bg-indigo-50 text-indigo-700'
            : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'}`}>
            <Clock className="w-4 h-4"/>
            Requests ({pendingRequests.length})
            {pendingRequests.length > 0 && (<span className="w-2 h-2 rounded-full bg-amber-500"></span>)}
          </button>
        </div>

        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2"/>
          <input type="text" placeholder="Search connections..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} className="w-full pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"/>
        </div>
      </div>

      {loading ? (<div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600"></div>
        </div>) : activeTab === 'active' ? (filteredActive.length === 0 ? (<div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
            <Users className="w-12 h-12 text-gray-300 mx-auto mb-3"/>
            <h3 className="text-base font-semibold text-gray-900">No connections yet</h3>
            <p className="text-xs text-gray-500 max-w-sm mx-auto mt-1 mb-6">
              Connect with experienced mentors and enthusiastic learners to chat, share knowledge, and schedule sessions.
            </p>
            <Link to="/mentors">
              <Button variant="outline" size="sm">Explore Mentors</Button>
            </Link>
          </div>) : (<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {filteredActive.map((conn) => {
                const peer = conn.sender.id === user?.id ? conn.receiver : conn.sender;
                return (<div key={conn.id} className="bg-white rounded-xl border border-gray-100 p-5 shadow-xs hover:shadow-md transition-shadow flex flex-col justify-between">
                  <div>
                    <div className="flex items-start gap-3 mb-3">
                      <img src={peer.profileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${peer.fullName}`} alt={peer.fullName} className="w-12 h-12 rounded-full object-cover border border-gray-100"/>
                      <div className="flex-1 min-w-0">
                        <h4 className="font-semibold text-gray-900 truncate">{peer.fullName}</h4>
                        <p className="text-xs text-gray-500 truncate">{peer.headline || peer.email}</p>
                        <div className="mt-1">
                          <Badge variant="primary">
                            {peer.primaryRole === 'BOTH' ? 'Mentor & Learner' : peer.primaryRole}
                          </Badge>
                        </div>
                      </div>
                    </div>

                    {conn.message && (<p className="text-xs text-gray-600 bg-gray-50 p-2.5 rounded-lg italic mb-4 line-clamp-2">
                        "{conn.message}"
                      </p>)}
                  </div>

                  <div className="flex items-center gap-2 pt-3 border-t border-gray-50">
                    <Button size="sm" variant="primary" onClick={() => navigate(`/messages?user=${peer.id}`)} className="flex-1 flex items-center justify-center gap-1.5">
                      <MessageSquare className="w-3.5 h-3.5"/> Message
                    </Button>
                    {peer.mentorProfileId && (<Link to={`/mentors/${peer.mentorProfileId}`}>
                        <Button size="sm" variant="outline">
                          Profile
                        </Button>
                      </Link>)}
                  </div>
                </div>);
            })}
          </div>)) : (
        /* Pending Requests Tab */
        pendingRequests.length === 0 ? (<div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
            <Clock className="w-12 h-12 text-gray-300 mx-auto mb-3"/>
            <h3 className="text-base font-semibold text-gray-900">No pending requests</h3>
            <p className="text-xs text-gray-500 max-w-sm mx-auto mt-1">
              When other members request to connect with you, they will show up here.
            </p>
          </div>) : (<div className="space-y-3">
            {pendingRequests.map((req) => (<div key={req.id} className="bg-white p-5 rounded-xl border border-gray-100 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-start gap-4">
                  <img src={req.sender.profileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${req.sender.fullName}`} alt={req.sender.fullName} className="w-12 h-12 rounded-full object-cover"/>
                  <div>
                    <h4 className="font-semibold text-gray-900">{req.sender.fullName}</h4>
                    <p className="text-xs text-gray-500">{req.sender.headline || req.sender.email}</p>
                    {req.message && (<p className="text-xs text-gray-600 bg-gray-50 p-2 rounded-md mt-2 max-w-lg italic">
                        "{req.message}"
                      </p>)}
                    <span className="text-[11px] text-gray-400 block mt-1">
                      Requested {new Date(req.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-center">
                  <Button size="sm" variant="success" onClick={() => handleAccept(req.id)} className="flex items-center gap-1">
                    <Check className="w-4 h-4"/> Accept
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => handleReject(req.id)} className="text-gray-600 hover:text-red-600">
                    <X className="w-4 h-4"/> Decline
                  </Button>
                </div>
              </div>))}
          </div>))}
    </div>);
};
export default MyConnectionsPage;
