import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from './AuthContext';
const SocketContext = createContext(undefined);
export const SocketProvider = ({ children }) => {
    const { user, token } = useAuth();
    const stompClientRef = useRef(null);
    const [isConnected, setIsConnected] = useState(false);
    const pendingSubscriptionsRef = useRef(new Map());
    const activeSubsRef = useRef(new Map());
    useEffect(() => {
        if (!user || !token) {
            if (stompClientRef.current) {
                stompClientRef.current.deactivate();
                stompClientRef.current = null;
            }
            setIsConnected(false);
            return;
        }
        const wsUrl = `/ws?token=${encodeURIComponent(token)}`;
        const client = new Client({
            webSocketFactory: () => new SockJS(wsUrl),
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            reconnectDelay: 3000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });
        client.onConnect = () => {
            setIsConnected(true);
            // Resubscribe any pending listeners
            pendingSubscriptionsRef.current.forEach((callbacks, topic) => {
                if (!activeSubsRef.current.has(topic)) {
                    const sub = client.subscribe(topic, (message) => {
                        try {
                            const payload = JSON.parse(message.body);
                            const cbs = pendingSubscriptionsRef.current.get(topic);
                            if (cbs) {
                                cbs.forEach((cb) => cb(payload));
                            }
                        }
                        catch (err) {
                            console.error(`Failed to parse message from topic ${topic}:`, err);
                        }
                    });
                    activeSubsRef.current.set(topic, sub);
                }
            });
        };
        client.onDisconnect = () => {
            setIsConnected(false);
            activeSubsRef.current.clear();
        };
        client.onStompError = (frame) => {
            console.warn('STOMP protocol error:', frame.headers['message'] || frame);
        };
        client.onWebSocketError = (event) => {
            console.warn('WebSocket transport error:', event);
        };
        client.onWebSocketClose = () => {
            setIsConnected(false);
            activeSubsRef.current.clear();
        };
        client.activate();
        stompClientRef.current = client;
        return () => {
            activeSubsRef.current.forEach((sub) => {
                try {
                    sub.unsubscribe();
                }
                catch { }
            });
            activeSubsRef.current.clear();
            client.deactivate();
            setIsConnected(false);
        };
    }, [user, token]);
    const subscribeToTopic = useCallback((topic, callback) => {
        if (!pendingSubscriptionsRef.current.has(topic)) {
            pendingSubscriptionsRef.current.set(topic, new Set());
        }
        pendingSubscriptionsRef.current.get(topic).add(callback);
        const client = stompClientRef.current;
        if (client && client.connected && !activeSubsRef.current.has(topic)) {
            const sub = client.subscribe(topic, (message) => {
                try {
                    const payload = JSON.parse(message.body);
                    const cbs = pendingSubscriptionsRef.current.get(topic);
                    if (cbs) {
                        cbs.forEach((cb) => cb(payload));
                    }
                }
                catch (err) {
                    console.error(`Failed to parse message on topic ${topic}:`, err);
                }
            });
            activeSubsRef.current.set(topic, sub);
        }
        return () => {
            const cbs = pendingSubscriptionsRef.current.get(topic);
            if (cbs) {
                cbs.delete(callback);
                if (cbs.size === 0) {
                    pendingSubscriptionsRef.current.delete(topic);
                    const sub = activeSubsRef.current.get(topic);
                    if (sub) {
                        try {
                            sub.unsubscribe();
                        }
                        catch { }
                        activeSubsRef.current.delete(topic);
                    }
                }
            }
        };
    }, []);
    const sendStompMessage = useCallback((destination, payload) => {
        const client = stompClientRef.current;
        if (!client || !client.connected) {
            console.warn(`Cannot send message to ${destination}: WebSocket not connected`);
            return false;
        }
        try {
            client.publish({
                destination,
                body: JSON.stringify(payload),
            });
            return true;
        }
        catch (err) {
            console.error(`Failed to send STOMP message to ${destination}:`, err);
            return false;
        }
    }, []);
    const subscribeToConversation = useCallback((convId, callback) => {
        return subscribeToTopic(`/topic/conversations/${convId}`, callback);
    }, [subscribeToTopic]);
    const subscribeToMentorStatus = useCallback((callback) => {
        return subscribeToTopic('/topic/mentors/status', callback);
    }, [subscribeToTopic]);
    const subscribeToUserStatus = useCallback((callback) => {
        return subscribeToTopic('/topic/users/status', callback);
    }, [subscribeToTopic]);
    return (<SocketContext.Provider value={{
            isConnected,
            subscribeToTopic,
            sendStompMessage,
            subscribeToConversation,
            subscribeToMentorStatus,
            subscribeToUserStatus,
        }}>
      {children}
    </SocketContext.Provider>);
};
export const useSocket = () => {
    const context = useContext(SocketContext);
    if (!context)
        throw new Error('useSocket must be used within a SocketProvider');
    return context;
};
