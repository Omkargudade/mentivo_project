import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import api from '../api/client';
import { useAuth } from './AuthContext';
import { useSocket } from './SocketContext';
import { useFeatureFlags } from './FeatureFlagContext';

const CallContext = createContext(undefined);

const DEFAULT_RTC_CONFIG = {
    iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
        { urls: 'stun:stun2.l.google.com:19302' },
    ],
};

// Web Audio API Ringtone Generator (No external MP3 dependencies)
class RingtonePlayer {
    ctx = null;
    intervalId = null;

    startOutgoing() {
        this.stop();
        try {
            const AudioCtx = window.AudioContext || window.webkitAudioContext;
            if (!AudioCtx) return;
            this.ctx = new AudioCtx();
            const playTone = () => {
                if (!this.ctx) return;
                const osc = this.ctx.createOscillator();
                const gain = this.ctx.createGain();
                osc.type = 'sine';
                osc.frequency.setValueAtTime(440, this.ctx.currentTime);
                gain.gain.setValueAtTime(0.08, this.ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 1.2);
                osc.connect(gain);
                gain.connect(this.ctx.destination);
                osc.start();
                osc.stop(this.ctx.currentTime + 1.2);
            };
            playTone();
            this.intervalId = setInterval(playTone, 3000);
        } catch {}
    }

    startIncoming() {
        this.stop();
        try {
            const AudioCtx = window.AudioContext || window.webkitAudioContext;
            if (!AudioCtx) return;
            this.ctx = new AudioCtx();
            const playChime = () => {
                if (!this.ctx) return;
                const osc1 = this.ctx.createOscillator();
                const osc2 = this.ctx.createOscillator();
                const gain = this.ctx.createGain();
                osc1.type = 'sine';
                osc2.type = 'sine';
                osc1.frequency.setValueAtTime(523.25, this.ctx.currentTime); // C5
                osc2.frequency.setValueAtTime(659.25, this.ctx.currentTime + 0.15); // E5
                gain.gain.setValueAtTime(0.12, this.ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 1.5);
                osc1.connect(gain);
                osc2.connect(gain);
                gain.connect(this.ctx.destination);
                osc1.start();
                osc1.stop(this.ctx.currentTime + 0.5);
                osc2.start(this.ctx.currentTime + 0.15);
                osc2.stop(this.ctx.currentTime + 1.5);
            };
            playChime();
            this.intervalId = setInterval(playChime, 2500);
        } catch {}
    }

    stop() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }
        if (this.ctx) {
            try {
                this.ctx.close();
            } catch {}
            this.ctx = null;
        }
    }
}

const ringtones = new RingtonePlayer();

export const CallProvider = ({ children }) => {
    const { user } = useAuth();
    const { subscribeToTopic, sendStompMessage } = useSocket();
    const { features } = useFeatureFlags();

    const [callStatus, setCallStatus] = useState('IDLE');
    const [incomingCall, setIncomingCall] = useState(null);
    const [activePeer, setActivePeer] = useState(null);
    const [localStream, setLocalStream] = useState(null);
    const [remoteStream, setRemoteStream] = useState(null);
    const [isMuted, setIsMuted] = useState(false);
    const [isCameraOff, setIsCameraOff] = useState(false);
    const [isAudioOnly, setIsAudioOnly] = useState(false);
    const [facingMode, setFacingMode] = useState('user');
    const [hasMultipleCameras, setHasMultipleCameras] = useState(false);
    const [callDuration, setCallDuration] = useState(0);
    const [errorMessage, setErrorMessage] = useState(null);
    const [rtcConfig, setRtcConfig] = useState(DEFAULT_RTC_CONFIG);
    const [pendingReviewSession, setPendingReviewSession] = useState(null);

    const pcRef = useRef(null);
    const localStreamRef = useRef(null);
    const remoteStreamRef = useRef(null);
    const callStatusRef = useRef('IDLE');
    const callIdRef = useRef('');
    const targetUserIdRef = useRef(null);
    const timerRef = useRef(null);
    const callTimeoutRef = useRef(null);
    const incomingTimeoutRef = useRef(null);
    const pendingCandidatesRef = useRef([]);
    const durationRef = useRef(0);
    const wasConnectedRef = useRef(false);
    const sessionDetailsRef = useRef(null);

    // Keep callStatusRef synchronized with state
    useEffect(() => {
        callStatusRef.current = callStatus;
    }, [callStatus]);

    // Check available devices for dual-camera support
    const checkCameras = useCallback(async () => {
        if (typeof navigator !== 'undefined' && navigator.mediaDevices?.enumerateDevices) {
            try {
                const devices = await navigator.mediaDevices.enumerateDevices();
                const videoInputs = devices.filter((d) => d.kind === 'videoinput');
                setHasMultipleCameras(videoInputs.length > 1);
            } catch {
                setHasMultipleCameras(false);
            }
        }
    }, []);

    // Load ICE servers from backend on mount
    useEffect(() => {
        checkCameras();
        api.get('/call/ice-servers')
            .then((res) => {
                if (res.data?.success && res.data.data?.iceServers) {
                    setRtcConfig({ iceServers: res.data.data.iceServers });
                }
            })
            .catch(() => {
                setRtcConfig(DEFAULT_RTC_CONFIG);
            });
    }, [checkCameras]);

    // Cleanup WebRTC connections and media tracks
    const cleanupCall = useCallback(() => {
        ringtones.stop();
        if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
        }
        if (callTimeoutRef.current) {
            clearTimeout(callTimeoutRef.current);
            callTimeoutRef.current = null;
        }
        if (incomingTimeoutRef.current) {
            clearTimeout(incomingTimeoutRef.current);
            incomingTimeoutRef.current = null;
        }
        if (pcRef.current) {
            pcRef.current.onicecandidate = null;
            pcRef.current.ontrack = null;
            pcRef.current.onconnectionstatechange = null;
            pcRef.current.oniceconnectionstatechange = null;
            try {
                pcRef.current.close();
            } catch (err) {
                console.warn('Error closing peer connection:', err);
            }
            pcRef.current = null;
        }
        if (localStreamRef.current) {
            localStreamRef.current.getTracks().forEach((track) => {
                try {
                    track.stop();
                    track.enabled = false;
                } catch (err) {
                    console.warn('Error stopping media track:', err);
                }
            });
            localStreamRef.current = null;
        }
        if (remoteStreamRef.current) {
            remoteStreamRef.current.getTracks().forEach((track) => {
                try {
                    track.stop();
                    track.enabled = false;
                } catch (err) {
                    console.warn('Error stopping remote track:', err);
                }
            });
            remoteStreamRef.current = null;
        }
        setLocalStream(null);
        setRemoteStream(null);
        setIncomingCall(null);
        setIsMuted(false);
        setIsCameraOff(false);
        setIsAudioOnly(false);
        setCallDuration(0);
        callIdRef.current = '';
        targetUserIdRef.current = null;
        pendingCandidatesRef.current = [];
        wasConnectedRef.current = false;
        durationRef.current = 0;
        sessionDetailsRef.current = null;
    }, []);

    // Duration counter when connected
    useEffect(() => {
        if (callStatus === 'CONNECTED') {
            wasConnectedRef.current = true;
            setCallDuration(0);
            durationRef.current = 0;
            timerRef.current = setInterval(() => {
                setCallDuration((prev) => {
                    const next = prev + 1;
                    durationRef.current = next;
                    return next;
                });
            }, 1000);
        } else {
            if (timerRef.current) {
                clearInterval(timerRef.current);
                timerRef.current = null;
            }
        }
        return () => {
            if (timerRef.current) clearInterval(timerRef.current);
        };
    }, [callStatus]);

    // Check call completion with backend and trigger review modal if eligible
    const checkCallCompletion = (targetUserId, callId) => {
        const finalDuration = durationRef.current;
        const wasConnected = wasConnectedRef.current;
        if (wasConnected && targetUserId && user && finalDuration >= 5) {
            const activeBookingId = sessionDetailsRef.current?.bookingId || activePeer?.bookingId;
            console.log('[WebRTC:SESSION_COMPLETED] Reporting completed call session:', callId, 'duration:', finalDuration, 'bookingId:', activeBookingId);
            api.post('/call/session/complete', {
                callId: callId || callIdRef.current,
                peerUserId: targetUserId,
                durationSeconds: finalDuration,
                bookingId: activeBookingId || undefined,
            }).then((res) => {
                if (res.data?.success && res.data.data?.eligible) {
                    console.log('[WebRTC:REVIEW_PROMPT] Eligible for review:', res.data.data);
                    setPendingReviewSession({
                        bookingId: res.data.data.bookingId,
                        mentorId: res.data.data.mentorId,
                        mentorName: res.data.data.mentorName,
                    });
                }
            }).catch((err) => {
                console.warn('[WebRTC:SESSION_COMPLETED] Error checking call completion:', err);
            });
        }
    };

    // Safe user media acquisition with audio-only fallback and mobile device permissions
    const acquireMedia = async () => {
        if (
            typeof navigator === 'undefined' ||
            !navigator.mediaDevices ||
            typeof navigator.mediaDevices.getUserMedia !== 'function'
        ) {
            const isHttps =
                typeof window !== 'undefined' &&
                (window.location.protocol === 'https:' ||
                    window.location.hostname === 'localhost' ||
                    window.location.hostname === '127.0.0.1');
            let msg = 'Camera and microphone access is required for video calls. Please allow permissions in your browser settings.';
            if (!isHttps) {
                msg =
                    'Camera and microphone access requires a secure HTTPS connection when accessed on a mobile phone or local network. Please open Mentivo using https://' +
                    window.location.host;
            }
            console.error('[WebRTC:MEDIA_PERMISSION_DENIED] mediaDevices.getUserMedia not available (isSecureContext:', window.isSecureContext, ')');
            setErrorMessage(msg);
            throw new Error(msg);
        }

        try {
            // Attempt video + audio with current facingMode
            const stream = await navigator.mediaDevices.getUserMedia({
                video: {
                    width: { ideal: 1280, max: 1920 },
                    height: { ideal: 720, max: 1080 },
                    facingMode: { ideal: facingMode },
                },
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true,
                },
            });
            setIsAudioOnly(false);
            localStreamRef.current = stream;
            setLocalStream(stream);
            checkCameras();
            return stream;
        } catch (primaryErr) {
            console.warn('[WebRTC] Primary getUserMedia failed:', primaryErr.name, primaryErr.message);

            // 1. If camera constraint issue, try basic video constraints
            if (primaryErr.name === 'OverconstrainedError' || primaryErr.name === 'ConstraintNotSatisfiedError') {
                try {
                    const fallbackStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
                    setIsAudioOnly(false);
                    localStreamRef.current = fallbackStream;
                    setLocalStream(fallbackStream);
                    return fallbackStream;
                } catch {}
            }

            // 2. If camera is missing, in use by another app, or camera-only permission denied, fallback gracefully to AUDIO-ONLY
            if (
                primaryErr.name === 'NotFoundError' ||
                primaryErr.name === 'DevicesNotFoundError' ||
                primaryErr.name === 'NotReadableError' ||
                primaryErr.name === 'TrackStartError' ||
                primaryErr.name === 'NotAllowedError'
            ) {
                try {
                    console.log('[WebRTC] Camera unavailable, attempting audio-only connection...');
                    const audioStream = await navigator.mediaDevices.getUserMedia({
                        audio: {
                            echoCancellation: true,
                            noiseSuppression: true,
                            autoGainControl: true,
                        },
                        video: false,
                    });
                    setIsAudioOnly(true);
                    localStreamRef.current = audioStream;
                    setLocalStream(audioStream);
                    return audioStream;
                } catch (audioErr) {
                    console.error('[WebRTC] Audio fallback also failed:', audioErr);
                }
            }

            let msg = 'Camera and microphone access is required for calls. Please allow permissions in your browser settings.';
            if (primaryErr.name === 'NotAllowedError' || primaryErr.name === 'PermissionDeniedError') {
                msg = 'Camera or microphone permission was denied. Please allow access in your browser or device settings and try again.';
            } else if (primaryErr.name === 'NotFoundError' || primaryErr.name === 'DevicesNotFoundError') {
                msg = 'No camera or microphone was found on your device.';
            } else if (primaryErr.name === 'NotReadableError' || primaryErr.name === 'TrackStartError') {
                msg = 'Camera or microphone is already in use by another application.';
            } else if (primaryErr.name === 'SecurityError') {
                msg = 'Camera access is blocked due to an insecure context. Please ensure you are connected using HTTPS.';
            }
            setErrorMessage(msg);
            throw new Error(msg);
        }
    };

    // Switch between front and rear cameras on mobile devices
    const switchCamera = async () => {
        if (!localStreamRef.current || isCameraOff || isAudioOnly) return;
        try {
            const nextMode = facingMode === 'user' ? 'environment' : 'user';
            const oldVideoTrack = localStreamRef.current.getVideoTracks()[0];

            const newStream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: { ideal: nextMode } },
                audio: false,
            });
            const newVideoTrack = newStream.getVideoTracks()[0];

            if (newVideoTrack) {
                if (oldVideoTrack) {
                    oldVideoTrack.stop();
                    localStreamRef.current.removeTrack(oldVideoTrack);
                }
                localStreamRef.current.addTrack(newVideoTrack);
                newVideoTrack.onended = () => {
                    setIsCameraOff(true);
                };
                setLocalStream(new MediaStream(localStreamRef.current.getTracks()));
                setFacingMode(nextMode);

                // Replace track on RTCRtpSender
                if (pcRef.current) {
                    const senders = pcRef.current.getSenders();
                    const videoSender = senders.find((s) => s.track && s.track.kind === 'video');
                    if (videoSender) {
                        await videoSender.replaceTrack(newVideoTrack);
                    }
                }
            }
        } catch (err) {
            console.warn('[WebRTC] Camera flip failed:', err);
        }
    };

    // Create RTCPeerConnection with track & candidate listeners
    const createPeerConnection = (targetId, currentCallId) => {
        if (pcRef.current) {
            try {
                pcRef.current.onicecandidate = null;
                pcRef.current.ontrack = null;
                pcRef.current.onconnectionstatechange = null;
                pcRef.current.oniceconnectionstatechange = null;
                pcRef.current.close();
            } catch {}
            pcRef.current = null;
        }

        const pc = new RTCPeerConnection(rtcConfig);

        pc.onicecandidate = (event) => {
            if (event.candidate) {
                sendStompMessage('/app/call.signal', {
                    type: 'ICE_CANDIDATE',
                    callId: currentCallId,
                    senderId: user?.id,
                    targetUserId: targetId,
                    bookingId: sessionDetailsRef.current?.bookingId || undefined,
                    candidate: event.candidate.toJSON ? event.candidate.toJSON() : event.candidate,
                });
            }
        };

        pc.ontrack = (event) => {
            console.log('[WebRTC:PEER_CONNECTED] Received remote track:', event.track.kind);
            let stream = remoteStreamRef.current;
            if (!stream) {
                stream = new MediaStream();
                remoteStreamRef.current = stream;
            }
            if (!stream.getTracks().some((t) => t.id === event.track.id)) {
                stream.addTrack(event.track);
            }
            setRemoteStream(new MediaStream(stream.getTracks()));
        };

        pc.onconnectionstatechange = () => {
            console.log('[WebRTC:PEER_CONNECTED] ConnectionState changed to:', pc.connectionState);
            if (pc.connectionState === 'connected') {
                if (callTimeoutRef.current) {
                    clearTimeout(callTimeoutRef.current);
                    callTimeoutRef.current = null;
                }
                setCallStatus('CONNECTED');
            } else if (pc.connectionState === 'disconnected' || pc.connectionState === 'failed') {
                console.warn('[WebRTC:WEBRTC_ERROR] Peer connection state:', pc.connectionState);
                cleanupCall();
                setCallStatus('ENDED');
                setTimeout(() => {
                    setCallStatus('IDLE');
                    setActivePeer(null);
                }, 2000);
            }
        };

        pc.oniceconnectionstatechange = () => {
            console.log('[WebRTC:ICE_CANDIDATE] ICEConnectionState:', pc.iceConnectionState);
            if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
                if (callTimeoutRef.current) {
                    clearTimeout(callTimeoutRef.current);
                    callTimeoutRef.current = null;
                }
                setCallStatus('CONNECTED');
            } else if (pc.iceConnectionState === 'failed') {
                console.warn('[WebRTC:WEBRTC_ERROR] ICE Connection failed');
            }
        };

        pcRef.current = pc;
        return pc;
    };

    // Start outgoing call (supports optional session details for booked sessions)
    const startCall = async (targetUserId, targetUserName, targetUserImage, sessionDetails = null) => {
        if (!features.videoCalling) {
            setErrorMessage('1-to-1 Video Calling is currently inactive.');
            return;
        }
        if (!user) {
            setErrorMessage('You must be logged in to start a video call.');
            return;
        }
        const allowedStates = ['IDLE', 'FAILED', 'ENDED', 'REJECTED'];
        if (!allowedStates.includes(callStatusRef.current)) {
            console.warn('Cannot start call: already in state', callStatusRef.current);
            return;
        }

        console.log('[WebRTC:CALL_STARTED] Initiating call to user', targetUserId, targetUserName, 'session:', sessionDetails);
        cleanupCall();

        const generatedCallId = `call_${Date.now()}_${user.id}_${targetUserId}`;
        callIdRef.current = generatedCallId;
        targetUserIdRef.current = targetUserId;
        sessionDetailsRef.current = sessionDetails;
        setActivePeer({
            peerId: targetUserId,
            peerName: targetUserName,
            peerImage: targetUserImage,
            bookingId: sessionDetails?.bookingId,
            sessionId: sessionDetails?.sessionId,
            sessionTitle: sessionDetails?.sessionTitle,
        });
        setCallStatus('CALLING');
        ringtones.startOutgoing();

        // Start 35s unanswered call timeout for caller
        callTimeoutRef.current = setTimeout(() => {
            console.log('[WebRTC:TIMEOUT] Outgoing call unanswered after 35 seconds');
            if (targetUserId && user) {
                sendStompMessage('/app/call.signal', {
                    type: 'CALL_END',
                    callId: generatedCallId,
                    senderId: user.id,
                    targetUserId,
                    bookingId: sessionDetails?.bookingId,
                    reason: 'Call timed out (no answer)',
                });
            }
            cleanupCall();
            setCallStatus('REJECTED');
            setErrorMessage('No answer from the other participant.');
            setTimeout(() => {
                setCallStatus('IDLE');
                setActivePeer(null);
            }, 3000);
        }, 35000);

        try {
            // 1. Acquire media FIRST. If permission denied, do not send call offer.
            const stream = await acquireMedia();

            // 2. Create peer connection and bind local tracks
            const pc = createPeerConnection(targetUserId, generatedCallId);
            stream.getTracks().forEach((track) => pc.addTrack(track, stream));

            // 3. Create offer and set local description
            const offer = await pc.createOffer();
            await pc.setLocalDescription(offer);
            console.log('[WebRTC:OFFER_CREATED] Local offer created for call', generatedCallId);

            // 4. Send CALL_OFFER signal to peer
            const sent = sendStompMessage('/app/call.signal', {
                type: 'CALL_OFFER',
                callId: generatedCallId,
                senderId: user.id,
                senderName: user.fullName,
                senderImage: user.profileImage,
                targetUserId,
                sdp: offer,
                bookingId: sessionDetails?.bookingId,
                sessionId: sessionDetails?.sessionId,
                sessionTitle: sessionDetails?.sessionTitle,
            });

            if (!sent) {
                throw new Error('Signaling server is temporarily unavailable.');
            }
        } catch (err) {
            console.error('[WebRTC:START_CALL_ERROR]', err);
            cleanupCall();
            setCallStatus('FAILED');
            if (!errorMessage) {
                setErrorMessage(err?.message || 'Unable to start call. Please check camera and microphone permissions.');
            }
        }
    };

    // Accept incoming call
    const acceptCall = async () => {
        if (!incomingCall || !user) return;
        ringtones.stop();
        if (incomingTimeoutRef.current) {
            clearTimeout(incomingTimeoutRef.current);
            incomingTimeoutRef.current = null;
        }

        console.log('[WebRTC:CALL_ACCEPTED] Accepting incoming call', incomingCall.callId, 'from', incomingCall.callerName, 'booking:', incomingCall.bookingId);
        const { callId, callerId, callerName, callerImage, sdp, bookingId, sessionId, sessionTitle } = incomingCall;
        callIdRef.current = callId;
        targetUserIdRef.current = callerId;
        sessionDetailsRef.current = { bookingId, sessionId, sessionTitle };
        setActivePeer({
            peerId: callerId,
            peerName: callerName,
            peerImage: callerImage,
            bookingId,
            sessionId,
            sessionTitle,
        });
        setIncomingCall(null);
        setCallStatus('CONNECTING');

        try {
            // 1. Explicitly request camera and microphone only upon Accept
            const stream = await acquireMedia();

            // 2. Create peer connection and add local tracks
            const pc = createPeerConnection(callerId, callId);
            stream.getTracks().forEach((track) => pc.addTrack(track, stream));

            // 3. Set remote description from caller's offer
            await pc.setRemoteDescription(new RTCSessionDescription(sdp));

            // 4. Process any buffered ICE candidates received before remote description was ready
            while (pendingCandidatesRef.current.length > 0) {
                const candidate = pendingCandidatesRef.current.shift();
                if (candidate) {
                    try {
                        await pc.addIceCandidate(new RTCIceCandidate(candidate));
                    } catch (e) {
                        console.warn('Error adding buffered ICE candidate:', e);
                    }
                }
            }

            // 5. Create WebRTC answer
            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);
            console.log('[WebRTC:ANSWER_CREATED] Answer created for call', callId);

            // 6. Send CALL_ANSWER signal to caller
            sendStompMessage('/app/call.signal', {
                type: 'CALL_ANSWER',
                callId,
                senderId: user.id,
                senderName: user.fullName,
                senderImage: user.profileImage,
                targetUserId: callerId,
                sdp: answer,
                bookingId,
                sessionId,
                sessionTitle,
            });
        } catch (err) {
            console.error('[WebRTC:WEBRTC_ERROR] Failed to accept call:', err);
            const failureReason = err?.message || 'Receiver could not access camera or microphone.';
            setErrorMessage(failureReason);
            sendStompMessage('/app/call.signal', {
                type: 'CALL_REJECT',
                callId,
                senderId: user.id,
                targetUserId: callerId,
                reason: failureReason,
            });
            cleanupCall();
            setCallStatus('FAILED');
        }
    };

    // Reject incoming call
    const rejectCall = () => {
        ringtones.stop();
        if (incomingTimeoutRef.current) {
            clearTimeout(incomingTimeoutRef.current);
            incomingTimeoutRef.current = null;
        }

        if (incomingCall && user) {
            console.log('[WebRTC:CALL_REJECTED] Declining call', incomingCall.callId);
            sendStompMessage('/app/call.signal', {
                type: 'CALL_REJECT',
                callId: incomingCall.callId,
                senderId: user.id,
                targetUserId: incomingCall.callerId,
                reason: 'Call declined by user',
            });
        }
        setIncomingCall(null);
        cleanupCall();
        setCallStatus('REJECTED');
        setTimeout(() => {
            setCallStatus('IDLE');
            setActivePeer(null);
        }, 1500);
    };

    // End active call
    const endCall = () => {
        ringtones.stop();
        const targetId = targetUserIdRef.current;
        const currentCallId = callIdRef.current;
        console.log('[WebRTC:CALL_ENDED] Ending active call', currentCallId);

        if (targetId && user) {
            sendStompMessage('/app/call.signal', {
                type: 'CALL_END',
                callId: currentCallId,
                senderId: user.id,
                targetUserId: targetId,
                bookingId: sessionDetailsRef.current?.bookingId || undefined,
            });
        }
        checkCallCompletion(targetId, currentCallId);
        cleanupCall();
        setCallStatus('ENDED');
        setTimeout(() => {
            setCallStatus('IDLE');
            setActivePeer(null);
        }, 1500);
    };

    // Retry failed call
    const retryCall = () => {
        setErrorMessage(null);
        if (activePeer) {
            startCall(activePeer.peerId, activePeer.peerName, activePeer.peerImage);
        } else {
            setCallStatus('IDLE');
        }
    };

    // Media track controls
    const toggleMute = () => {
        if (localStreamRef.current) {
            const audioTracks = localStreamRef.current.getAudioTracks();
            if (audioTracks.length > 0) {
                const nextEnabled = !audioTracks[0].enabled;
                audioTracks.forEach((t) => (t.enabled = nextEnabled));
                setIsMuted(!nextEnabled);
            }
        }
    };

    const toggleCamera = () => {
        if (localStreamRef.current) {
            const videoTracks = localStreamRef.current.getVideoTracks();
            if (videoTracks.length > 0) {
                const nextEnabled = !videoTracks[0].enabled;
                videoTracks.forEach((t) => (t.enabled = nextEnabled));
                setIsCameraOff(!nextEnabled);
            }
        }
    };

    const dismissError = () => {
        setErrorMessage(null);
        if (callStatus === 'FAILED') {
            setCallStatus('IDLE');
            setActivePeer(null);
        }
    };

    // Global listener for call signaling: /topic/user/{user.id}/call
    useEffect(() => {
        if (!user?.id) return;

        const unsubscribe = subscribeToTopic(`/topic/user/${user.id}/call`, async (signal) => {
            if (!signal || !signal.type) return;

            // Filter out signals sent by myself
            if (signal.senderId === user.id) {
                return;
            }

            switch (signal.type) {
                case 'CALL_OFFER': {
                    if (!features.videoCalling) {
                        sendStompMessage('/app/call.signal', {
                            type: 'CALL_REJECT',
                            callId: signal.callId,
                            senderId: user.id,
                            targetUserId: signal.senderId,
                            reason: 'Video calling is currently inactive',
                        });
                        return;
                    }

                    if (callStatusRef.current !== 'IDLE') {
                        console.log('[WebRTC:WEBRTC_ERROR] User busy, rejecting incoming call', signal.callId);
                        sendStompMessage('/app/call.signal', {
                            type: 'CALL_BUSY',
                            callId: signal.callId,
                            senderId: user.id,
                            targetUserId: signal.senderId,
                        });
                        return;
                    }

                    console.log('[WebRTC:INCOMING_CALL] Received CALL_OFFER from', signal.senderName, signal.senderId, 'callId:', signal.callId, 'booking:', signal.bookingId);

                    callIdRef.current = signal.callId;
                    targetUserIdRef.current = signal.senderId;
                    sessionDetailsRef.current = {
                        bookingId: signal.bookingId,
                        sessionId: signal.sessionId,
                        sessionTitle: signal.sessionTitle,
                    };

                    setIncomingCall({
                        callId: signal.callId,
                        callerId: signal.senderId,
                        callerName: signal.senderName || 'Caller',
                        callerImage: signal.senderImage,
                        sdp: signal.sdp,
                        bookingId: signal.bookingId,
                        sessionId: signal.sessionId,
                        sessionTitle: signal.sessionTitle,
                    });

                    setCallStatus('INCOMING');
                    ringtones.startIncoming();

                    // 35s timeout for unanswered incoming call
                    if (incomingTimeoutRef.current) clearTimeout(incomingTimeoutRef.current);
                    incomingTimeoutRef.current = setTimeout(() => {
                        console.log('[WebRTC:TIMEOUT] Incoming call timed out after 35 seconds');
                        rejectCall();
                    }, 35000);
                    break;
                }

                case 'CALL_ANSWER': {
                    if (signal.callId !== callIdRef.current) {
                        console.warn('Ignoring CALL_ANSWER for mismatched callId:', signal.callId);
                        return;
                    }

                    if (callTimeoutRef.current) {
                        clearTimeout(callTimeoutRef.current);
                        callTimeoutRef.current = null;
                    }

                    ringtones.stop();
                    console.log('[WebRTC:PEER_CONNECTED] Received CALL_ANSWER for call', signal.callId);
                    setCallStatus('CONNECTING');

                    if (signal.bookingId && !sessionDetailsRef.current?.bookingId) {
                        sessionDetailsRef.current = {
                            bookingId: signal.bookingId,
                            sessionId: signal.sessionId,
                            sessionTitle: signal.sessionTitle,
                        };
                        setActivePeer((prev) => (prev ? {
                            ...prev,
                            bookingId: signal.bookingId,
                            sessionId: signal.sessionId,
                            sessionTitle: signal.sessionTitle,
                        } : prev));
                    }

                    if (pcRef.current && signal.sdp) {
                        try {
                            await pcRef.current.setRemoteDescription(new RTCSessionDescription(signal.sdp));
                            while (pendingCandidatesRef.current.length > 0) {
                                const candidate = pendingCandidatesRef.current.shift();
                                if (candidate) {
                                    try {
                                        await pcRef.current.addIceCandidate(new RTCIceCandidate(candidate));
                                    } catch (e) {
                                        console.warn('Error adding buffered candidate on answer:', e);
                                    }
                                }
                            }
                        } catch (err) {
                            console.error('[WebRTC:WEBRTC_ERROR] Failed to set remote answer:', err);
                        }
                    }
                    break;
                }

                case 'ICE_CANDIDATE': {
                    if (signal.candidate) {
                        if (signal.callId && callIdRef.current && signal.callId !== callIdRef.current) {
                            return;
                        }

                        if (pcRef.current && pcRef.current.remoteDescription) {
                            try {
                                await pcRef.current.addIceCandidate(new RTCIceCandidate(signal.candidate));
                            } catch (err) {
                                console.error('[WebRTC:WEBRTC_ERROR] Failed to add ICE candidate:', err);
                            }
                        } else {
                            pendingCandidatesRef.current.push(signal.candidate);
                        }
                    }
                    break;
                }

                case 'CALL_REJECT': {
                    ringtones.stop();
                    if (callTimeoutRef.current) {
                        clearTimeout(callTimeoutRef.current);
                        callTimeoutRef.current = null;
                    }
                    console.log('[WebRTC:CALL_REJECTED] Received CALL_REJECT:', signal.reason);
                    cleanupCall();
                    setCallStatus('REJECTED');
                    setErrorMessage(signal.reason || 'Call was declined.');
                    setTimeout(() => {
                        setCallStatus('IDLE');
                        setActivePeer(null);
                    }, 2500);
                    break;
                }

                case 'CALL_BUSY': {
                    ringtones.stop();
                    if (callTimeoutRef.current) {
                        clearTimeout(callTimeoutRef.current);
                        callTimeoutRef.current = null;
                    }
                    console.log('[WebRTC:WEBRTC_ERROR] Peer is busy on another call');
                    cleanupCall();
                    setCallStatus('REJECTED');
                    setErrorMessage('User is currently on another call.');
                    setTimeout(() => {
                        setCallStatus('IDLE');
                        setActivePeer(null);
                    }, 2500);
                    break;
                }

                case 'CALL_END': {
                    ringtones.stop();
                    if (callTimeoutRef.current) {
                        clearTimeout(callTimeoutRef.current);
                        callTimeoutRef.current = null;
                    }
                    if (incomingTimeoutRef.current) {
                        clearTimeout(incomingTimeoutRef.current);
                        incomingTimeoutRef.current = null;
                    }
                    console.log('[WebRTC:CALL_ENDED] Received CALL_END from peer');
                    const targetId = targetUserIdRef.current || signal.senderId;
                    const currentCallId = callIdRef.current || signal.callId;
                    checkCallCompletion(targetId, currentCallId);
                    cleanupCall();
                    setCallStatus('ENDED');
                    setTimeout(() => {
                        setCallStatus('IDLE');
                        setActivePeer(null);
                    }, 1500);
                    break;
                }
            }
        });

        return () => {
            unsubscribe();
        };
    }, [user?.id, features.videoCalling, subscribeToTopic, sendStompMessage, cleanupCall, rejectCall]);

    // Cleanup and notify peer on window close or tab navigation
    useEffect(() => {
        const handleBeforeUnload = () => {
            if (callStatusRef.current !== 'IDLE' && targetUserIdRef.current && user?.id) {
                sendStompMessage('/app/call.signal', {
                    type: 'CALL_END',
                    callId: callIdRef.current,
                    senderId: user.id,
                    targetUserId: targetUserIdRef.current,
                    reason: 'Participant closed the browser',
                });
                cleanupCall();
            }
        };
        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [user?.id, sendStompMessage, cleanupCall]);

    return (
        <CallContext.Provider
            value={{
                callStatus,
                incomingCall,
                activePeer,
                localStream,
                remoteStream,
                isMuted,
                isCameraOff,
                isAudioOnly,
                facingMode,
                hasMultipleCameras,
                callDuration,
                errorMessage,
                startCall,
                acceptCall,
                rejectCall,
                endCall,
                toggleMute,
                toggleCamera,
                switchCamera,
                dismissError,
                retryCall,
                pendingReviewSession,
                setPendingReviewSession,
                clearPendingReviewSession: () => setPendingReviewSession(null),
            }}
        >
            {children}
        </CallContext.Provider>
    );
};

export const useCall = () => {
    const context = useContext(CallContext);
    if (!context) throw new Error('useCall must be used within a CallProvider');
    return context;
};
