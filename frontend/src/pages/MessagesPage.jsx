import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { Send, Search, MessageSquare, Video, Check, CheckCheck, AlertCircle, ArrowLeft, Sparkles, Wifi, WifiOff } from 'lucide-react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useSocket } from '../context/SocketContext';
import { useFeatureFlags } from '../context/FeatureFlagContext';
import { useCall } from '../context/CallContext';
import Button from '../components/common/Button';
import UserAvatar from '../components/common/UserAvatar';

export const MessagesPage = () => {
    const { user } = useAuth();
    const { isConnected, subscribeToTopic, sendStompMessage, subscribeToUserStatus, subscribeToMentorStatus } = useSocket();
    const { features } = useFeatureFlags();
    const { startCall } = useCall();

    const [searchParams, setSearchParams] = useSearchParams();
    const targetConvId = searchParams.get('convId');
    const targetUserId = searchParams.get('user');

    const [conversations, setConversations] = useState([]);
    const [activeConv, setActiveConv] = useState(null);
    const [messages, setMessages] = useState([]);
    const [inputMessage, setInputMessage] = useState('');
    const [userOnlineMap, setUserOnlineMap] = useState({});
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [messagesLoading, setMessagesLoading] = useState(false);
    const [sending, setSending] = useState(false);
    const [sendError, setSendError] = useState(null);
    const [peerTyping, setPeerTyping] = useState(null);
    const [mobileShowChat, setMobileShowChat] = useState(false);

    const messagesEndRef = useRef(null);
    const typingTimeoutRef = useRef(null);
    const activeConvRef = useRef(activeConv);

    useEffect(() => {
        activeConvRef.current = activeConv;
    }, [activeConv]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    // Fetch conversations list
    const fetchConversations = useCallback(async () => {
        try {
            setLoading(true);
            const res = await api.get('/conversations');
            if (res.data?.success && res.data.data) {
                const list = res.data.data.content || res.data.data;
                setConversations(list);
                const initialMap = {};
                list.forEach((c) => {
                    if (c.peerUser?.id) {
                        initialMap[c.peerUser.id] = c.peerUser.onlineStatus === 'ONLINE';
                    }
                });
                setUserOnlineMap((prev) => ({ ...initialMap, ...prev }));
                return list;
            }
            return [];
        } catch (err) {
            console.error('Failed to load conversations', err);
            return [];
        } finally {
            setLoading(false);
        }
    }, []);

    // Live presence sync for all conversation peers
    useEffect(() => {
        const unsubUser = subscribeToUserStatus?.((update) => {
            if (update?.userId) {
                setUserOnlineMap((prev) => ({
                    ...prev,
                    [update.userId]: update.onlineStatus === 'ONLINE',
                }));
            }
        });
        const unsubMentor = subscribeToMentorStatus?.((card) => {
            if (card?.userId) {
                setUserOnlineMap((prev) => ({
                    ...prev,
                    [card.userId]: card.onlineStatus === 'ONLINE',
                }));
            }
        });
        return () => {
            if (unsubUser) unsubUser();
            if (unsubMentor) unsubMentor();
        };
    }, [subscribeToUserStatus, subscribeToMentorStatus]);

    // Initial load
    useEffect(() => {
        fetchConversations().then((list) => {
            if (targetConvId) {
                const found = list.find((c) => c.id === Number(targetConvId));
                if (found) {
                    setActiveConv(found);
                    setMobileShowChat(true);
                } else {
                    loadSingleConversation(Number(targetConvId));
                }
            } else if (targetUserId) {
                initConversationWithUser(Number(targetUserId));
            } else if (list.length > 0 && !activeConv) {
                setActiveConv(list[0]);
            }
        });
    }, [targetConvId, targetUserId, fetchConversations]);

    const loadSingleConversation = async (convId) => {
        try {
            const res = await api.get(`/conversations/${convId}`);
            if (res.data?.success && res.data.data) {
                const conv = res.data.data;
                setActiveConv(conv);
                setMobileShowChat(true);
                setConversations((prev) => {
                    if (prev.some((c) => c.id === conv.id)) return prev;
                    return [conv, ...prev];
                });
            }
        } catch (err) {
            console.error('Failed to load single conversation', err);
        }
    };

    const initConversationWithUser = async (peerId) => {
        try {
            const res = await api.post('/conversations', { peerId });
            if (res.data?.success && res.data.data) {
                const conv = res.data.data;
                setActiveConv(conv);
                setMobileShowChat(true);
                setConversations((prev) => {
                    if (prev.some((c) => c.id === conv.id)) return prev;
                    return [conv, ...prev];
                });
            }
        } catch (err) {
            console.error('Failed to start conversation with user', err);
        }
    };

    const loadMessages = async (convId) => {
        try {
            setMessagesLoading(true);
            const res = await api.get(`/conversations/${convId}/messages`);
            if (res.data?.success && res.data.data) {
                const msgs = res.data.data.content || res.data.data;
                const sorted = Array.isArray(msgs)
                    ? [...msgs].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
                    : [];
                setMessages(sorted);
            }
            // Mark as read on server
            await api.put(`/conversations/${convId}/read`).catch(() => {});
            // Decrement unread count locally
            setConversations((prev) =>
                prev.map((c) => (c.id === convId ? { ...c, unreadCount: 0 } : c))
            );
        } catch (err) {
            console.error('Failed to load messages', err);
        } finally {
            setMessagesLoading(false);
        }
    };

    // Global listener for inbound messages across ALL conversations
    useEffect(() => {
        if (!user?.id) return;

        const unsubUserMessages = subscribeToTopic(`/topic/user/${user.id}/messages`, (newMsg) => {
            if (!newMsg) return;

            const currentActive = activeConvRef.current;
            // 1. If currently viewing this conversation, append message
            if (currentActive && newMsg.conversationId === currentActive.id) {
                setMessages((prev) => {
                    if (prev.some((m) => m.id === newMsg.id)) return prev;
                    return [...prev, newMsg];
                });
                api.put(`/conversations/${currentActive.id}/read`).catch(() => {});
                scrollToBottom();
            }

            // 2. Update conversation list preview & unread count
            setConversations((prev) => {
                const exists = prev.some((c) => c.id === newMsg.conversationId);
                if (exists) {
                    return prev.map((c) =>
                        c.id === newMsg.conversationId
                            ? {
                                  ...c,
                                  lastMessagePreview: newMsg.content,
                                  lastMessageAt: newMsg.createdAt,
                                  unreadCount: c.id === currentActive?.id ? 0 : (c.unreadCount || 0) + 1,
                              }
                            : c
                    );
                } else {
                    // New conversation, reload list
                    fetchConversations();
                    return prev;
                }
            });
        });

        return () => {
            unsubUserMessages();
        };
    }, [user?.id, subscribeToTopic, fetchConversations]);

    // When active conversation changes
    useEffect(() => {
        if (!activeConv) return;
        loadMessages(activeConv.id);
        setPeerTyping(null);
        setSendError(null);

        // 1. Subscribe to incoming messages on this specific conversation
        const unsubMessages = subscribeToTopic(`/topic/conversations/${activeConv.id}`, (newMsg) => {
            setMessages((prev) => {
                if (prev.some((m) => m.id === newMsg.id)) return prev;
                return [...prev, newMsg];
            });

            // Update conversation list preview
            setConversations((prev) =>
                prev.map((c) =>
                    c.id === activeConv.id
                        ? { ...c, lastMessagePreview: newMsg.content, lastMessageAt: newMsg.createdAt }
                        : c
                )
            );

            // If incoming from peer, mark as read immediately
            if (newMsg.senderId !== user?.id) {
                api.put(`/conversations/${activeConv.id}/read`).catch(() => {});
            }
            scrollToBottom();
        });

        // 2. Subscribe to read receipts
        const unsubRead = subscribeToTopic(`/topic/conversations/${activeConv.id}/read`, (event) => {
            if (event && event.readerId !== user?.id) {
                setMessages((prev) =>
                    prev.map((m) => (m.senderId === user?.id ? { ...m, readStatus: 'READ' } : m))
                );
            }
        });

        // 3. Subscribe to typing indicator
        const unsubTyping = subscribeToTopic(`/topic/conversations/${activeConv.id}/typing`, (typing) => {
            if (typing && typing.userId !== user?.id) {
                if (typing.typing) {
                    setPeerTyping(typing.userName || 'Peer');
                } else {
                    setPeerTyping(null);
                }
            }
        });

        return () => {
            unsubMessages();
            unsubRead();
            unsubTyping();
        };
    }, [activeConv?.id, user?.id, subscribeToTopic]);

    useEffect(() => {
        scrollToBottom();
    }, [messages, peerTyping]);

    // Handle typing input with debounced stop signal
    const handleInputChange = (e) => {
        const val = e.target.value;
        setInputMessage(val);
        setSendError(null);

        if (!activeConv || !features.messaging) return;

        // Send typing: true
        sendStompMessage('/app/chat.typing', {
            conversationId: activeConv.id,
            userId: user?.id,
            userName: user?.fullName,
            typing: true,
        });

        if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
        typingTimeoutRef.current = setTimeout(() => {
            sendStompMessage('/app/chat.typing', {
                conversationId: activeConv.id,
                userId: user?.id,
                userName: user?.fullName,
                typing: false,
            });
        }, 2000);
    };

    const handleSendMessage = async (e) => {
        if (e) e.preventDefault();
        if (!inputMessage.trim() || !activeConv || sending || !features.messaging) return;

        const content = inputMessage.trim();
        setInputMessage('');
        setSending(true);
        setSendError(null);

        // Clear typing indicator
        if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
        sendStompMessage('/app/chat.typing', {
            conversationId: activeConv.id,
            userId: user?.id,
            userName: user?.fullName,
            typing: false,
        });

        try {
            const res = await api.post(`/conversations/${activeConv.id}/messages`, {
                content,
                messageType: 'TEXT',
            });

            if (res.data?.success && res.data.data) {
                const savedMsg = res.data.data;
                setMessages((prev) => {
                    if (prev.some((m) => m.id === savedMsg.id)) return prev;
                    return [...prev, savedMsg];
                });
                setConversations((prev) =>
                    prev.map((c) =>
                        c.id === activeConv.id
                            ? { ...c, lastMessagePreview: content, lastMessageAt: new Date().toISOString() }
                            : c
                    )
                );
            }
        } catch (err) {
            console.error('Failed to send message', err);
            setSendError('Unable to deliver message. Please check your connection and try again.');
            setInputMessage(content); // Restore message for easy retry
        } finally {
            setSending(false);
        }
    };

    const handleStartVideoCall = () => {
        if (!activeConv) return;
        startCall(activeConv.peerUser.id, activeConv.peerUser.fullName, activeConv.peerUser.profileImage);
    };

    const filteredConversations = conversations.filter((c) =>
        c.peerUser?.fullName?.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 h-[calc(100vh-5rem)] h-[calc(100dvh-5rem)]">
            {/* Messaging Feature Inactive Global Alert Banner */}
            {!features.messaging && (
                <div className="mb-4 bg-amber-50 border border-amber-200 text-amber-900 rounded-2xl p-4 flex items-center gap-3 text-xs shadow-2xs">
                    <AlertCircle className="w-5 h-5 text-amber-600 shrink-0" />
                    <div className="flex-1">
                        <span className="font-bold">Notice: Direct Messaging is currently inactive.</span>
                        <span className="ml-1 text-amber-700">
                            You can review your existing conversation history, but new messages cannot be sent at this time.
                        </span>
                    </div>
                </div>
            )}

            <div className="bg-white rounded-3xl shadow-xs border border-slate-200/80 flex h-full overflow-hidden">
                {/* Left Sidebar: Conversations List */}
                <div
                    className={`w-full sm:w-80 md:w-96 border-r border-slate-100 flex flex-col h-full bg-slate-50/50 ${
                        mobileShowChat ? 'hidden sm:flex' : 'flex'
                    }`}
                >
                    {/* Sidebar Header */}
                    <div className="p-4 border-b border-slate-100 bg-white">
                        <div className="flex items-center justify-between mb-3">
                            <h2 className="text-lg font-black text-slate-900 tracking-tight flex items-center gap-2">
                                <MessageSquare className="w-5 h-5 text-indigo-600" />
                                <span>Messages</span>
                            </h2>
                            <div className="flex items-center gap-2">
                                {/* Real-Time Connection Indicator */}
                                <span
                                    className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold border ${
                                        isConnected
                                            ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                            : 'bg-amber-50 text-amber-700 border-amber-200'
                                    }`}
                                    title={isConnected ? 'Live real-time messaging connected' : 'Connecting to live messaging...'}
                                >
                                    <span
                                        className={`w-1.5 h-1.5 rounded-full ${
                                            isConnected ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'
                                        }`}
                                    />
                                    <span>{isConnected ? 'Live' : 'Connecting'}</span>
                                </span>
                                <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                                    {conversations.length} {conversations.length === 1 ? 'chat' : 'chats'}
                                </span>
                            </div>
                        </div>

                        <div className="relative">
                            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                            <input
                                type="text"
                                placeholder="Search conversations..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="w-full pl-9 pr-4 py-2 bg-slate-50 text-xs border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                            />
                        </div>
                    </div>

                    {/* Conversations Scroll List */}
                    <div className="flex-1 overflow-y-auto divide-y divide-slate-100">
                        {loading ? (
                            <div className="flex justify-center items-center py-16">
                                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                            </div>
                        ) : filteredConversations.length === 0 ? (
                            <div className="text-center py-16 px-4">
                                <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto mb-3">
                                    <MessageSquare className="w-6 h-6" />
                                </div>
                                <p className="text-xs font-semibold text-slate-700">No conversations yet</p>
                                <p className="text-[11px] text-slate-400 mt-1 max-w-xs mx-auto">
                                    Browse verified mentors to start a 1-on-1 mentorship dialogue.
                                </p>
                                <Link to="/mentors" className="mt-3 inline-block">
                                    <Button variant="primary" size="sm" className="text-xs">
                                        Find a Mentor
                                    </Button>
                                </Link>
                            </div>
                        ) : (
                            filteredConversations.map((conv) => {
                                const isSelected = activeConv?.id === conv.id;
                                return (
                                    <button
                                        key={conv.id}
                                        onClick={() => {
                                            setActiveConv(conv);
                                            setMobileShowChat(true);
                                            setSearchParams({ convId: conv.id.toString() });
                                        }}
                                        className={`w-full text-left p-3.5 sm:p-4 flex items-center gap-3 transition-colors cursor-pointer ${
                                            isSelected
                                                ? 'bg-indigo-50/80 border-r-4 border-indigo-600'
                                                : 'hover:bg-slate-100/60'
                                        }`}
                                    >
                                        <div className="relative shrink-0">
                                            <UserAvatar
                                                src={conv.peerUser.profileImage}
                                                name={conv.peerUser.fullName}
                                                size="md"
                                                className="w-11 h-11 rounded-2xl object-cover"
                                            />
                                            {(() => {
                                                const isPeerOnline = userOnlineMap[conv.peerUser.id] ?? (conv.peerUser.onlineStatus === 'ONLINE');
                                                return (
                                                    <span className={`absolute bottom-0 right-0 w-3 h-3 rounded-full ring-2 ring-white ${isPeerOnline ? 'bg-emerald-500' : 'bg-slate-300'}`} />
                                                );
                                            })()}
                                        </div>

                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center justify-between">
                                                <h4 className="font-bold text-xs sm:text-sm text-slate-900 truncate">
                                                    {conv.peerUser.fullName}
                                                </h4>
                                                {conv.lastMessageAt && (
                                                    <span className="text-[10px] text-slate-400 font-medium">
                                                        {new Date(conv.lastMessageAt).toLocaleTimeString([], {
                                                            hour: '2-digit',
                                                            minute: '2-digit',
                                                        })}
                                                    </span>
                                                )}
                                            </div>
                                            <p className="text-[11px] text-slate-500 truncate mt-0.5 font-normal">
                                                {conv.lastMessagePreview || 'Say hello...'}
                                            </p>
                                        </div>

                                        {conv.unreadCount > 0 && (
                                            <span className="w-5 h-5 bg-indigo-600 text-white text-[10px] font-bold rounded-full flex items-center justify-center shrink-0">
                                                {conv.unreadCount}
                                            </span>
                                        )}
                                    </button>
                                );
                            })
                        )}
                    </div>
                </div>

                {/* Right Pane: Active Chat Conversation */}
                <div className={`flex-1 flex-col h-full bg-white ${mobileShowChat ? 'flex' : 'hidden sm:flex'}`}>
                    {activeConv ? (
                        <>
                            {/* Chat Header */}
                            <div className="px-4 sm:px-6 py-3.5 border-b border-slate-100 flex items-center justify-between bg-white/90 backdrop-blur-xs">
                                <div className="flex items-center gap-3">
                                    {/* Mobile Back Button */}
                                    <button
                                        onClick={() => setMobileShowChat(false)}
                                        className="sm:hidden p-1 text-slate-500 hover:text-slate-800 cursor-pointer"
                                    >
                                        <ArrowLeft className="w-5 h-5" />
                                    </button>

                                    {(() => {
                                        const isActivePeerOnline = userOnlineMap[activeConv.peerUser.id] ?? (activeConv.peerUser.onlineStatus === 'ONLINE');
                                        return (
                                            <>
                                                <div className="relative">
                                                    <UserAvatar
                                                        src={activeConv.peerUser.profileImage}
                                                        name={activeConv.peerUser.fullName}
                                                        size="sm"
                                                        className="w-10 h-10 rounded-2xl object-cover"
                                                    />
                                                    <span className={`absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full ring-2 ring-white ${isActivePeerOnline ? 'bg-emerald-500' : 'bg-slate-300'}`} />
                                                </div>

                                                <div>
                                                    <h3 className="font-bold text-slate-900 text-xs sm:text-sm leading-none">
                                                        {activeConv.peerUser.fullName}
                                                    </h3>
                                                    <div className="flex items-center gap-2 mt-1">
                                                        <p className={`text-[11px] font-medium flex items-center gap-1 ${isActivePeerOnline ? 'text-emerald-600' : 'text-slate-500'}`}>
                                                            <span className={`w-1.5 h-1.5 rounded-full inline-block ${isActivePeerOnline ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                                                            <span>{isActivePeerOnline ? 'Online on Mentivo' : 'Offline'}</span>
                                                        </p>
                                                        {isConnected && (
                                                            <span className="text-[10px] text-slate-400 font-semibold">• Live sync</span>
                                                        )}
                                                    </div>
                                                </div>
                                            </>
                                        );
                                    })()}
                                </div>

                                {/* Header Action Buttons */}
                                <div className="flex items-center gap-2">
                                    {/* 1-to-1 Video Call Button */}
                                    <Button
                                        size="sm"
                                        variant={features.videoCalling ? 'primary' : 'outline'}
                                        disabled={!features.videoCalling}
                                        onClick={handleStartVideoCall}
                                        className="gap-1.5 text-xs py-1.5 px-3 min-h-[36px]"
                                        title={
                                            features.videoCalling
                                                ? 'Start free 1-on-1 video call'
                                                : 'Video calling is currently inactive'
                                        }
                                    >
                                        <Video className="w-3.5 h-3.5" />
                                        <span className="hidden sm:inline">
                                            {features.videoCalling ? 'Video Call' : 'Call Disabled'}
                                        </span>
                                    </Button>

                                    {/* Profile Link */}
                                    {activeConv.peerUser.mentorProfileId && (
                                        <Link to={`/mentors/${activeConv.peerUser.mentorProfileId}`}>
                                            <Button size="sm" variant="outline" className="text-xs py-1.5 px-2.5 min-h-[36px]">
                                                Profile
                                            </Button>
                                        </Link>
                                    )}
                                </div>
                            </div>

                            {/* Chat Message Stream */}
                            <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-3 sm:space-y-4 bg-slate-50/50">
                                {messagesLoading ? (
                                    <div className="flex justify-center items-center py-16">
                                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                                    </div>
                                ) : messages.length === 0 ? (
                                    <div className="text-center py-16">
                                        <div className="w-10 h-10 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto mb-2">
                                            <Sparkles className="w-5 h-5" />
                                        </div>
                                        <p className="text-xs font-semibold text-slate-700">
                                            Beginning of conversation with {activeConv.peerUser.fullName}
                                        </p>
                                        <p className="text-[11px] text-slate-400 mt-1">
                                            Say hello or discuss session topics!
                                        </p>
                                    </div>
                                ) : (
                                    messages.map((msg) => {
                                        const isOutgoing = msg.senderId === user?.id;
                                        return (
                                            <div
                                                key={msg.id}
                                                className={`flex flex-col ${isOutgoing ? 'items-end' : 'items-start'}`}
                                            >
                                                {!isOutgoing && (
                                                    <span className="text-[10px] font-bold text-indigo-700 mb-0.5 px-1 tracking-tight">
                                                        {msg.senderName || activeConv.peerUser.fullName}
                                                    </span>
                                                )}
                                                <div
                                                    className={`max-w-[85%] sm:max-w-md px-4 py-2.5 rounded-2xl text-xs sm:text-sm leading-relaxed shadow-2xs ${
                                                        isOutgoing
                                                            ? 'bg-indigo-600 text-white rounded-br-xs'
                                                            : 'bg-white text-slate-900 border border-slate-200/80 rounded-bl-xs'
                                                    }`}
                                                >
                                                    <p className="whitespace-pre-wrap break-words">{msg.content}</p>
                                                </div>
                                                <div className="flex items-center gap-1 mt-1 text-[10px] text-slate-400 px-1 font-medium">
                                                    <span>
                                                        {new Date(msg.createdAt).toLocaleTimeString([], {
                                                            hour: '2-digit',
                                                            minute: '2-digit',
                                                        })}
                                                    </span>
                                                    {isOutgoing &&
                                                        (msg.readStatus === 'READ' ? (
                                                            <span title="Read">
                                                                <CheckCheck className="w-3 h-3 text-indigo-600" />
                                                            </span>
                                                        ) : (
                                                            <span title="Sent">
                                                                <Check className="w-3 h-3 text-slate-400" />
                                                            </span>
                                                        ))}
                                                </div>
                                            </div>
                                        );
                                    })
                                )}

                                {/* Typing Indicator Bubble */}
                                {peerTyping && (
                                    <div className="flex items-center gap-2 text-xs text-slate-500 italic bg-white/80 border border-slate-200/80 rounded-2xl px-3.5 py-1.5 w-fit">
                                        <span className="flex gap-1 items-center">
                                            <span className="w-1.5 h-1.5 rounded-full bg-indigo-600 animate-bounce" />
                                            <span className="w-1.5 h-1.5 rounded-full bg-indigo-600 animate-bounce [animation-delay:0.2s]" />
                                            <span className="w-1.5 h-1.5 rounded-full bg-indigo-600 animate-bounce [animation-delay:0.4s]" />
                                        </span>
                                        <span>{peerTyping} is typing...</span>
                                    </div>
                                )}

                                <div ref={messagesEndRef} />
                            </div>

                            {/* Error Alert Banner if delivery failed */}
                            {sendError && (
                                <div className="px-4 py-2 bg-rose-50 border-t border-rose-200 text-rose-700 text-xs flex items-center justify-between">
                                    <span>{sendError}</span>
                                    <button
                                        onClick={() => setSendError(null)}
                                        className="text-rose-500 hover:text-rose-800 font-bold ml-2 cursor-pointer"
                                    >
                                        Dismiss
                                    </button>
                                </div>
                            )}

                            {/* Message Input Bar (Supports multiline and Enter-to-send) */}
                            <form
                                onSubmit={handleSendMessage}
                                className="p-3 sm:p-4 border-t border-slate-100 bg-white flex items-end gap-2 sm:gap-3"
                            >
                                <textarea
                                    rows={1}
                                    placeholder={
                                        features.messaging
                                            ? `Message ${activeConv.peerUser.fullName}... (Enter to send, Shift+Enter for new line)`
                                            : 'Direct messaging is currently disabled by administrator'
                                    }
                                    value={inputMessage}
                                    disabled={!features.messaging || sending}
                                    onChange={handleInputChange}
                                    onKeyDown={(e) => {
                                        if (e.key === 'Enter' && !e.shiftKey) {
                                            e.preventDefault();
                                            handleSendMessage();
                                        }
                                    }}
                                    className="flex-1 px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white resize-none max-h-32 disabled:opacity-50 disabled:cursor-not-allowed leading-relaxed"
                                />
                                <Button
                                    type="submit"
                                    variant="primary"
                                    isLoading={sending}
                                    disabled={!features.messaging || !inputMessage.trim()}
                                    className="px-4 sm:px-5 py-2.5 min-h-[42px] rounded-xl shrink-0 flex items-center justify-center gap-1.5 text-xs touch-manipulation"
                                >
                                    <Send className="w-4 h-4" />
                                    <span className="hidden sm:inline">Send</span>
                                </Button>
                            </form>
                        </>
                    ) : (
                        <div className="flex-1 flex flex-col items-center justify-center p-6 text-center">
                            <div className="w-16 h-16 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mb-4 shadow-sm">
                                <MessageSquare className="w-8 h-8" />
                            </div>
                            <h3 className="text-lg font-bold text-slate-900">Direct Mentorship Messages</h3>
                            <p className="text-xs text-slate-500 max-w-sm mt-1">
                                Select a conversation from the left or visit a mentor profile to start a new discussion.
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default MessagesPage;
