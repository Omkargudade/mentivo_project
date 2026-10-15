import React, { useEffect, useRef } from 'react';
import { useCall } from '../../context/CallContext';
import UserAvatar from '../common/UserAvatar';
import { Mic, MicOff, Video, VideoOff, PhoneOff, Sparkles, AlertCircle, X, ShieldCheck, RefreshCw, Volume2 } from 'lucide-react';

export const VideoCallModal = () => {
    const {
        callStatus,
        activePeer,
        localStream,
        remoteStream,
        isMuted,
        isCameraOff,
        isAudioOnly,
        hasMultipleCameras,
        callDuration,
        errorMessage,
        endCall,
        toggleMute,
        toggleCamera,
        switchCamera,
        dismissError,
        retryCall,
    } = useCall();

    const localVideoRef = useRef(null);
    const remoteVideoRef = useRef(null);

    // Check whether remote stream has an active video track
    const hasRemoteVideo = Boolean(
        remoteStream &&
        remoteStream.getVideoTracks().length > 0 &&
        remoteStream.getVideoTracks().some((t) => t.enabled && t.readyState === 'live')
    );

    // Attach local stream to local video element with auto-play catch
    useEffect(() => {
        if (localVideoRef.current && localStream) {
            localVideoRef.current.srcObject = localStream;
            localVideoRef.current.play().catch((err) => {
                console.warn('[WebRTC] Local video auto-play caught:', err);
            });
        }
    }, [localStream, callStatus]);

    // Attach remote stream to remote video element with auto-play catch
    useEffect(() => {
        if (remoteVideoRef.current && remoteStream) {
            remoteVideoRef.current.srcObject = remoteStream;
            remoteVideoRef.current.volume = 1.0;
            remoteVideoRef.current.play().catch((err) => {
                console.warn('[WebRTC] Remote video auto-play caught:', err);
            });
        }
    }, [remoteStream, callStatus]);

    const formatTime = (secs) => {
        const m = Math.floor(secs / 60).toString().padStart(2, '0');
        const s = (secs % 60).toString().padStart(2, '0');
        return `${m}:${s}`;
    };

    // Do not render VideoCallModal if in incoming state (IncomingCallModal handles this) or idle with no error
    if (callStatus === 'INCOMING' || (callStatus === 'IDLE' && !errorMessage)) {
        return null;
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-xl p-2 sm:p-6 animate-in fade-in duration-200">
            <div className="w-full max-w-5xl h-[88vh] bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-slate-800 flex flex-col relative">
                {/* Call Header */}
                <div className="absolute top-0 inset-x-0 z-20 px-6 py-4 bg-gradient-to-b from-slate-950/80 to-transparent flex items-center justify-between text-white">
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-indigo-600/30 border border-indigo-500/40 flex items-center justify-center text-indigo-400">
                            <Sparkles className="w-4 h-4" />
                        </div>
                        <div>
                            <div className="flex items-center gap-2">
                                <h3 className="text-sm sm:text-base font-bold text-white leading-none">
                                    {activePeer?.peerName || '1-to-1 Mentoring Session'}
                                </h3>
                                <span className="text-[10px] font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 px-2 py-0.5 rounded-full">
                                    {activePeer?.sessionTitle ? `Session: ${activePeer.sessionTitle}` : '1-on-1 Mentoring'}
                                </span>
                                {isAudioOnly && (
                                    <span className="text-[10px] font-semibold bg-amber-500/20 text-amber-300 border border-amber-500/30 px-2 py-0.5 rounded-full flex items-center gap-1">
                                        <Volume2 className="w-3 h-3" />
                                        Your Mic Only
                                    </span>
                                )}
                            </div>
                            <p className="text-[11px] text-slate-400 mt-1 flex items-center gap-1.5">
                                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                                <span>WebRTC Peer-to-Peer Encrypted</span>
                            </p>
                        </div>
                    </div>

                    {/* Call Timer / Status */}
                    <div className="flex items-center gap-3">
                        {callStatus === 'CONNECTED' && (
                            <div className="px-3.5 py-1.5 rounded-full bg-slate-800/80 border border-slate-700/80 text-xs font-mono font-bold text-emerald-400 flex items-center gap-2">
                                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                                <span>{formatTime(callDuration)}</span>
                            </div>
                        )}
                        {callStatus === 'CALLING' && (
                            <div className="px-3.5 py-1.5 rounded-full bg-indigo-900/60 border border-indigo-700/60 text-xs font-semibold text-indigo-300 animate-pulse">
                                Calling...
                            </div>
                        )}
                        {callStatus === 'CONNECTING' && (
                            <div className="px-3.5 py-1.5 rounded-full bg-emerald-900/60 border border-emerald-700/60 text-xs font-semibold text-emerald-300 animate-pulse">
                                Connecting...
                            </div>
                        )}
                        {callStatus === 'FAILED' && (
                            <div className="px-3.5 py-1.5 rounded-full bg-rose-900/60 border border-rose-700/60 text-xs font-semibold text-rose-300">
                                Call Failed
                            </div>
                        )}
                    </div>
                </div>

                {/* Error Notification Toast if any */}
                {errorMessage && callStatus !== 'FAILED' && (
                    <div className="absolute top-16 inset-x-4 sm:inset-x-auto sm:left-1/2 sm:-translate-x-1/2 z-30 max-w-md bg-rose-900/90 text-white border border-rose-700/80 rounded-2xl p-4 shadow-xl flex items-start gap-3 backdrop-blur-md">
                        <AlertCircle className="w-5 h-5 text-rose-300 shrink-0 mt-0.5" />
                        <div className="flex-1 text-xs leading-relaxed">
                            <p className="font-bold text-sm text-rose-100">Notice</p>
                            <p className="mt-0.5 text-rose-200">{errorMessage}</p>
                        </div>
                        <button onClick={dismissError} className="text-rose-300 hover:text-white cursor-pointer">
                            <X className="w-4 h-4" />
                        </button>
                    </div>
                )}

                {/* Main Video Arena */}
                <div className="flex-1 relative bg-slate-950 flex items-center justify-center overflow-hidden">
                    {/* Outgoing Calling State Screen */}
                    {callStatus === 'CALLING' && (
                        <div className="text-center space-y-4 z-10 px-4">
                            <div className="relative mx-auto w-28 h-28">
                                <div className="absolute inset-0 rounded-full bg-indigo-500/20 animate-ping" />
                                <div className="relative w-28 h-28 rounded-full ring-4 ring-indigo-500/40 overflow-hidden shadow-2xl mx-auto flex items-center justify-center bg-slate-800">
                                    <UserAvatar
                                        src={activePeer?.peerImage}
                                        name={activePeer?.peerName || 'Mentor'}
                                        size="xl"
                                        className="w-full h-full object-cover"
                                    />
                                </div>
                            </div>
                            <div>
                                <h4 className="text-xl font-bold text-white">{activePeer?.peerName}</h4>
                                <p className="text-xs text-indigo-300 mt-1">Ringing... Waiting for answer</p>
                            </div>
                        </div>
                    )}

                    {/* Connecting State Screen */}
                    {callStatus === 'CONNECTING' && (
                        <div className="text-center space-y-4 z-10 px-4">
                            <div className="relative mx-auto w-24 h-24">
                                <div className="absolute inset-0 rounded-full bg-emerald-500/20 animate-ping" />
                                <div className="relative w-24 h-24 rounded-full ring-4 ring-emerald-500/40 overflow-hidden shadow-2xl mx-auto flex items-center justify-center bg-slate-800">
                                    <UserAvatar
                                        src={activePeer?.peerImage}
                                        name={activePeer?.peerName || 'Peer'}
                                        size="xl"
                                        className="w-full h-full object-cover"
                                    />
                                </div>
                            </div>
                            <div>
                                <h4 className="text-xl font-bold text-white">{activePeer?.peerName}</h4>
                                <p className="text-xs text-emerald-300 mt-1 flex items-center justify-center gap-1.5">
                                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                                    <span>Establishing secure media peer connection...</span>
                                </p>
                            </div>
                        </div>
                    )}

                    {/* Connected State: Remote Video */}
                    {callStatus === 'CONNECTED' && (
                        <div className="w-full h-full relative flex items-center justify-center bg-black">
                            <video
                                ref={remoteVideoRef}
                                autoPlay
                                playsInline
                                className={`w-full h-full object-cover ${hasRemoteVideo ? 'block' : 'hidden'}`}
                            />

                            {/* Fallback if remote stream has no video */}
                            {!hasRemoteVideo && (
                                <div className="absolute inset-0 flex flex-col items-center justify-center bg-slate-900/90 text-center space-y-3 z-10">
                                    <div className="w-24 h-24 rounded-full overflow-hidden shadow-xl ring-4 ring-slate-700 bg-slate-800 mx-auto flex items-center justify-center">
                                        <UserAvatar
                                            src={activePeer?.peerImage}
                                            name={activePeer?.peerName || 'Peer'}
                                            size="xl"
                                            className="w-full h-full object-cover"
                                        />
                                    </div>
                                    <div>
                                        <h4 className="text-lg font-bold text-white">{activePeer?.peerName}</h4>
                                        <p className="text-xs text-slate-400 mt-1 flex items-center justify-center gap-1.5">
                                            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                                            <span>Remote Audio Stream Connected</span>
                                        </p>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Failed / Permission Error State */}
                    {callStatus === 'FAILED' && (
                        <div className="text-center space-y-4 z-10 px-6 max-w-md">
                            <div className="w-16 h-16 rounded-full bg-rose-500/20 border border-rose-500/40 text-rose-400 flex items-center justify-center mx-auto shadow-lg shadow-rose-500/10">
                                <AlertCircle className="w-8 h-8" />
                            </div>
                            <div>
                                <h4 className="text-lg font-bold text-white">Call Failed</h4>
                                <p className="text-xs text-rose-300 mt-2 leading-relaxed">
                                    {errorMessage || 'Unable to establish video connection. Please ensure camera and microphone permissions are granted.'}
                                </p>
                            </div>
                            <div className="flex items-center justify-center gap-3 pt-2">
                                <button
                                    onClick={retryCall}
                                    className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold transition-all shadow-md active:scale-95 cursor-pointer flex items-center gap-2"
                                >
                                    <Sparkles className="w-4 h-4" />
                                    <span>Try Again</span>
                                </button>
                                <button
                                    onClick={dismissError}
                                    className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold transition-all border border-slate-700 active:scale-95 cursor-pointer"
                                >
                                    Dismiss
                                </button>
                            </div>
                        </div>
                    )}

                    {/* Ended / Rejected State */}
                    {(callStatus === 'ENDED' || callStatus === 'REJECTED') && (
                        <div className="text-center space-y-3 z-10">
                            <div className="w-16 h-16 rounded-full bg-slate-800 border border-slate-700 text-slate-400 flex items-center justify-center mx-auto">
                                <PhoneOff className="w-7 h-7" />
                            </div>
                            <h4 className="text-lg font-bold text-white">
                                {callStatus === 'REJECTED' ? 'Call Declined' : 'Call Ended'}
                            </h4>
                            <p className="text-xs text-slate-400">
                                {callDuration > 0 ? `Duration: ${formatTime(callDuration)}` : 'Returning to session...'}
                            </p>
                        </div>
                    )}

                    {/* Picture-in-Picture Local Video (Bottom-Right) */}
                    {localStream && callStatus !== 'ENDED' && callStatus !== 'REJECTED' && callStatus !== 'FAILED' && (
                        <div className="absolute bottom-24 right-4 sm:bottom-28 sm:right-6 w-32 h-44 sm:w-44 sm:h-60 rounded-2xl overflow-hidden shadow-2xl border-2 border-slate-700 bg-slate-800 z-20 group">
                            <video
                                ref={localVideoRef}
                                autoPlay
                                playsInline
                                muted
                                className={`w-full h-full object-cover ${isCameraOff || isAudioOnly ? 'hidden' : 'block'} -scale-x-100`}
                            />
                            {(isCameraOff || isAudioOnly) && (
                                <div className="w-full h-full flex flex-col items-center justify-center bg-slate-800 text-slate-400 p-2 text-center">
                                    {isAudioOnly ? (
                                        <>
                                            <Volume2 className="w-6 h-6 mb-1 text-indigo-400" />
                                            <span className="text-[10px] font-semibold text-slate-300">Audio Only</span>
                                        </>
                                    ) : (
                                        <>
                                            <VideoOff className="w-6 h-6 mb-1 text-slate-500" />
                                            <span className="text-[10px] font-semibold">Camera Off</span>
                                        </>
                                    )}
                                </div>
                            )}
                            <div className="absolute bottom-2 left-2 px-2 py-0.5 rounded-md bg-slate-900/80 text-[10px] font-semibold text-slate-300 backdrop-blur-xs">
                                You
                            </div>
                        </div>
                    )}
                </div>

                {/* Bottom Control Bar */}
                <div className="px-6 py-4 bg-slate-950/90 border-t border-slate-800/80 flex items-center justify-center gap-3 sm:gap-5 z-20">
                    {callStatus === 'FAILED' ? (
                        <button
                            onClick={dismissError}
                            className="px-6 h-12 rounded-2xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 font-bold text-xs flex items-center justify-center gap-2 transition-all active:scale-95 cursor-pointer"
                        >
                            <X className="w-4 h-4" />
                            <span>Close</span>
                        </button>
                    ) : (
                        <>
                            {/* Mute Microphone Button */}
                            <button
                                onClick={toggleMute}
                                disabled={!localStream || callStatus === 'ENDED' || callStatus === 'REJECTED'}
                                className={`w-12 h-12 min-w-[48px] min-h-[48px] rounded-2xl flex items-center justify-center transition-all cursor-pointer shadow-md disabled:opacity-40 disabled:cursor-not-allowed ${
                                    isMuted
                                        ? 'bg-rose-500 hover:bg-rose-600 text-white'
                                        : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700'
                                }`}
                                title={isMuted ? 'Unmute Microphone' : 'Mute Microphone'}
                            >
                                {isMuted ? <MicOff className="w-5 h-5" /> : <Mic className="w-5 h-5" />}
                            </button>

                            {/* Toggle Camera Button */}
                            {!isAudioOnly && (
                                <button
                                    onClick={toggleCamera}
                                    disabled={!localStream || callStatus === 'ENDED' || callStatus === 'REJECTED'}
                                    className={`w-12 h-12 min-w-[48px] min-h-[48px] rounded-2xl flex items-center justify-center transition-all cursor-pointer shadow-md disabled:opacity-40 disabled:cursor-not-allowed ${
                                        isCameraOff
                                            ? 'bg-rose-500 hover:bg-rose-600 text-white'
                                            : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700'
                                    }`}
                                    title={isCameraOff ? 'Turn Camera On' : 'Turn Camera Off'}
                                >
                                    {isCameraOff ? <VideoOff className="w-5 h-5" /> : <Video className="w-5 h-5" />}
                                </button>
                            )}

                            {/* Flip / Switch Camera Button (Mobile & Multi-camera support) */}
                            {!isAudioOnly && !isCameraOff && hasMultipleCameras && (
                                <button
                                    onClick={switchCamera}
                                    disabled={!localStream || callStatus === 'ENDED' || callStatus === 'REJECTED'}
                                    className="w-12 h-12 min-w-[48px] min-h-[48px] rounded-2xl flex items-center justify-center transition-all cursor-pointer shadow-md bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 active:scale-95 disabled:opacity-40"
                                    title="Switch Camera (Front / Rear)"
                                >
                                    <RefreshCw className="w-5 h-5" />
                                </button>
                            )}

                            {/* End Call Button */}
                            <button
                                onClick={endCall}
                                className="px-6 h-12 min-h-[48px] rounded-2xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-lg shadow-rose-600/30 transition-all hover:scale-105 active:scale-95 cursor-pointer"
                                title="End Call"
                            >
                                <PhoneOff className="w-5 h-5" />
                                <span>End Call</span>
                            </button>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default VideoCallModal;
