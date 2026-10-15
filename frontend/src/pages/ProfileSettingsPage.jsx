import React, { useState, useEffect, useRef } from 'react';
import { MapPin, Globe, Briefcase, DollarSign, Award, Save, CheckCircle, User as UserIcon, Image as ImageIcon, Link as LinkIcon, GraduationCap, Shield, Upload, Trash2, Camera } from 'lucide-react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';

const LinkedinIcon = ({ className = 'w-3.5 h-3.5' }) => (
    <svg className={className} fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.79-1.75-1.764s.784-1.764 1.75-1.764 1.75.79 1.75 1.764-.783 1.764-1.75 1.764zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z" />
    </svg>
);

const GithubIcon = ({ className = 'w-3.5 h-3.5' }) => (
    <svg className={className} fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
        <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
    </svg>
);

export const ProfileSettingsPage = () => {
    const { user, refreshUser } = useAuth();
    const { showToast } = useNotification();
    const [profile, setProfile] = useState(null);
    const [fullName, setFullName] = useState('');
    const [headline, setHeadline] = useState('');
    const [bio, setBio] = useState('');
    const [profileImage, setProfileImage] = useState('');
    const [location, setLocation] = useState('');
    const [languages, setLanguages] = useState('');
    const [education, setEducation] = useState('');
    const [experience, setExperience] = useState('');
    const [website, setWebsite] = useState('');
    const [linkedIn, setLinkedIn] = useState('');
    const [gitHub, setGitHub] = useState('');
    const [isPublic, setIsPublic] = useState(true);
    const [hourlyRate, setHourlyRate] = useState('800');
    const [experienceYears, setExperienceYears] = useState('5');
    const [allCategories, setAllCategories] = useState([]);
    const [allSkills, setAllSkills] = useState([]);
    const [selectedCategoryIds, setSelectedCategoryIds] = useState([]);
    const [selectedSkillIds, setSelectedSkillIds] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [uploadingImage, setUploadingImage] = useState(false);
    const [showUrlInput, setShowUrlInput] = useState(false);
    const fileInputRef = useRef(null);

    useEffect(() => {
        fetchProfile();
    }, []);

    const fetchProfile = async () => {
        try {
            setLoading(true);
            const [userRes, catsRes, skillsRes] = await Promise.all([
                api.get('/users/me'),
                api.get('/categories').catch(() => ({ data: { data: [] } })),
                api.get('/skills').catch(() => ({ data: { data: [] } }))
            ]);

            if (catsRes.data?.success && catsRes.data.data) {
                setAllCategories(catsRes.data.data);
            }
            if (skillsRes.data?.success && skillsRes.data.data) {
                setAllSkills(skillsRes.data.data);
            }

            if (userRes.data?.success && userRes.data.data) {
                const u = userRes.data.data;
                setProfile(u);
                setFullName(u.fullName || '');
                setHeadline(u.headline || '');
                setBio(u.bio || '');
                setProfileImage(u.profileImage || '');
                setLocation(u.location || '');
                setLanguages(u.languages || 'English, Hindi');
                setEducation(u.education || '');
                setExperience(u.experience || '');
                setWebsite(u.website || '');
                setLinkedIn(u.linkedIn || '');
                setGitHub(u.gitHub || '');
                setIsPublic(u.public !== undefined ? Boolean(u.public) : true);
            }

            // If mentor, fetch rate, exp, categories, and skills
            try {
                const mentorRes = await api.get('/mentors/me');
                if (mentorRes.data?.success && mentorRes.data.data) {
                    const mData = mentorRes.data.data;
                    setHourlyRate(mData.hourlyRate !== undefined && mData.hourlyRate !== null ? String(mData.hourlyRate) : '0');
                    setExperienceYears(String(mData.experienceYears !== undefined && mData.experienceYears !== null ? mData.experienceYears : 5));
                    if (mData.categories && Array.isArray(mData.categories)) {
                        setSelectedCategoryIds(mData.categories.map(c => c.id));
                    }
                    if (mData.skills && Array.isArray(mData.skills)) {
                        setSelectedSkillIds(mData.skills.map(s => s.id));
                    }
                }
            } catch {
                // Not a mentor yet
            }
        } catch (err) {
            console.error('Failed to load profile', err);
            showToast('Unable to load profile data', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleFileSelect = (e) => {
        const file = e.target.files?.[0];
        if (file) {
            uploadFile(file);
        }
    };

    const uploadFile = async (file) => {
        // Validate size (5MB max)
        if (file.size > 5 * 1024 * 1024) {
            showToast('File size exceeds 5MB limit. Please choose a smaller image.', 'error');
            return;
        }

        // Validate type
        const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!validTypes.includes(file.type)) {
            showToast('Invalid image format. Allowed formats are JPEG, PNG, or WebP.', 'error');
            return;
        }

        try {
            setUploadingImage(true);
            const formData = new FormData();
            formData.append('file', file);

            const res = await api.post('/users/me/profile-picture', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });

            if (res.data?.success && res.data.data) {
                const updated = res.data.data;
                setProfile(updated);
                setProfileImage(updated.profileImage || '');
                if (refreshUser) await refreshUser();
                showToast('Profile picture uploaded successfully!', 'success');
            }
        } catch (err) {
            console.error('Upload failed', err);
            const msg = err.response?.data?.message || 'Failed to upload profile picture. Please try again.';
            showToast(msg, 'error');
        } finally {
            setUploadingImage(false);
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    };

    const handleRemovePicture = async () => {
        try {
            setUploadingImage(true);
            const res = await api.delete('/users/me/profile-picture');
            if (res.data?.success && res.data.data) {
                const updated = res.data.data;
                setProfile(updated);
                setProfileImage('');
                if (refreshUser) await refreshUser();
                showToast('Profile picture removed successfully!', 'success');
            }
        } catch (err) {
            console.error('Delete failed', err);
            showToast('Failed to remove profile picture.', 'error');
        } finally {
            setUploadingImage(false);
        }
    };

    const handleSave = async (e) => {
        if (e) e.preventDefault();
        if (saving) return;

        // Validation
        if (!fullName.trim()) {
            showToast('Full name is required', 'error');
            return;
        }

        try {
            setSaving(true);

            // 1. Save user profile details
            const userPayload = {
                fullName: fullName.trim(),
                headline: headline.trim(),
                bio: bio.trim(),
                profileImage: profileImage.trim(),
                location: location.trim(),
                languages: languages.trim(),
                education: education.trim(),
                experience: experience.trim(),
                website: website.trim(),
                linkedIn: linkedIn.trim(),
                gitHub: gitHub.trim(),
                isPublic: Boolean(isPublic),
            };

            const userRes = await api.put('/users/me', userPayload);

            // 2. If mentor, update hourly rate, experience, categories, and skills
            const isMentor = user?.primaryRole === 'MENTOR' || user?.primaryRole === 'BOTH' || profile?.primaryRole === 'MENTOR' || profile?.primaryRole === 'BOTH';
            if (isMentor) {
                const parsedRate = parseFloat(hourlyRate);
                const finalRate = isNaN(parsedRate) ? 0 : Math.max(0, parsedRate);
                await api.put('/mentors/me', {
                    hourlyRate: finalRate,
                    experienceYears: Math.max(0, parseInt(experienceYears, 10) || 0),
                    bioSummary: bio.trim(),
                    categoryIds: selectedCategoryIds,
                    skillIds: selectedSkillIds,
                });
            }

            // 3. Update local state from confirmed server response
            if (userRes.data?.success && userRes.data.data) {
                const updated = userRes.data.data;
                setProfile(updated);
                setFullName(updated.fullName || '');
                setHeadline(updated.headline || '');
                setBio(updated.bio || '');
                setProfileImage(updated.profileImage || '');
                setLocation(updated.location || '');
                setLanguages(updated.languages || '');
                setEducation(updated.education || '');
                setExperience(updated.experience || '');
                setWebsite(updated.website || '');
                setLinkedIn(updated.linkedIn || '');
                setGitHub(updated.gitHub || '');
                setIsPublic(updated.public !== undefined ? Boolean(updated.public) : true);
            }

            // 4. Refresh global auth context
            if (refreshUser) {
                await refreshUser();
            }

            // 5. Display success toast notification
            showToast('Your profile has been updated successfully!', 'success');
        } catch (err) {
            console.error('Failed to save profile', err);
            const msg = err.response?.data?.message || err.message || 'Failed to save profile changes. Please try again.';
            showToast(msg, 'error');
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center min-h-[60vh]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
            </div>
        );
    }

    const isMentorRole = user?.primaryRole === 'MENTOR' || user?.primaryRole === 'BOTH' || profile?.primaryRole === 'MENTOR' || profile?.primaryRole === 'BOTH';

    return (
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* Header Card */}
            <div className="bg-white rounded-2xl p-6 sm:p-8 border border-gray-100 shadow-xs mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-6">
                <div className="flex items-center gap-4">
                    <img
                        src={profileImage || profile?.profileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${fullName || profile?.fullName || 'User'}`}
                        alt={fullName || profile?.fullName}
                        className="w-16 h-16 rounded-full border-2 border-indigo-100 object-cover"
                        onError={(e) => {
                            e.target.src = `https://api.dicebear.com/7.x/avataaars/svg?seed=${fullName || 'User'}`;
                        }}
                    />
                    <div>
                        <div className="flex items-center gap-2">
                            <h1 className="text-xl font-bold text-gray-900">{profile?.fullName}</h1>
                            <Badge variant={profile?.isVerified ? 'success' : 'default'}>
                                {profile?.verificationLevel || 'UNVERIFIED'}
                            </Badge>
                        </div>
                        <p className="text-xs text-gray-500 mt-0.5">{profile?.email}</p>
                        <p className="text-xs text-indigo-600 font-medium mt-1">
                            Role: {profile?.primaryRole} • Active in {profile?.currentMode} Mode
                        </p>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    {!profile?.isVerified && (
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={async () => {
                                try {
                                    await api.post('/users/me/verification-request', { requestedLevel: 'EXPERT_VERIFIED' });
                                    showToast('Verification request submitted for admin review!', 'success');
                                } catch {
                                    showToast('Request submitted.', 'info');
                                }
                            }}
                            className="flex items-center gap-1.5 text-xs text-indigo-700 bg-indigo-50 border-indigo-200 cursor-pointer"
                        >
                            <Award className="w-4 h-4" /> Request Verified Badge
                        </Button>
                    )}
                </div>
            </div>

            {/* Profile Form */}
            <form onSubmit={handleSave} className="bg-white rounded-2xl p-6 sm:p-8 border border-gray-100 shadow-xs space-y-6">
                <div>
                    <h2 className="text-base font-bold text-gray-900 border-b border-gray-100 pb-3">
                        Public Profile Details
                    </h2>
                </div>

                {/* Profile Picture Upload Section */}
                <div className="p-4 sm:p-5 rounded-2xl bg-indigo-50/40 border border-indigo-100 flex flex-col sm:flex-row items-center gap-5">
                    <div className="relative group shrink-0">
                        <img
                            src={profileImage || profile?.profileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${fullName || 'User'}`}
                            alt="Profile"
                            className="w-20 h-20 rounded-full border-2 border-indigo-200 object-cover bg-white shadow-xs"
                            onError={(e) => {
                                e.target.src = `https://api.dicebear.com/7.x/avataaars/svg?seed=${fullName || 'User'}`;
                            }}
                        />
                        {uploadingImage && (
                            <div className="absolute inset-0 bg-black/40 rounded-full flex items-center justify-center">
                                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                            </div>
                        )}
                    </div>

                    <div className="flex-1 text-center sm:text-left space-y-2">
                        <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
                            <input
                                ref={fileInputRef}
                                type="file"
                                accept="image/jpeg,image/png,image/webp"
                                className="hidden"
                                onChange={handleFileSelect}
                            />
                            <Button
                                type="button"
                                variant="primary"
                                size="sm"
                                disabled={uploadingImage}
                                onClick={() => fileInputRef.current?.click()}
                                className="flex items-center gap-1.5 text-xs py-2 px-3.5 rounded-xl cursor-pointer"
                            >
                                <Upload className="w-3.5 h-3.5" />
                                {uploadingImage ? 'Uploading...' : 'Upload New Photo'}
                            </Button>

                            {profileImage && (
                                <button
                                    type="button"
                                    disabled={uploadingImage}
                                    onClick={handleRemovePicture}
                                    className="inline-flex items-center gap-1 text-xs font-semibold text-rose-600 hover:text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 py-1.5 px-3 rounded-xl transition-colors cursor-pointer"
                                >
                                    <Trash2 className="w-3.5 h-3.5" />
                                    Remove Photo
                                </button>
                            )}

                            <button
                                type="button"
                                onClick={() => setShowUrlInput(!showUrlInput)}
                                className="text-xs text-indigo-600 hover:text-indigo-800 underline ml-1 cursor-pointer"
                            >
                                {showUrlInput ? 'Hide URL input' : 'Or paste image URL'}
                            </button>
                        </div>
                        <p className="text-[11px] text-gray-500">
                            Supported: JPEG, PNG, WebP • Maximum file size: 5 MB
                        </p>
                        {showUrlInput && (
                            <div className="pt-2">
                                <input
                                    type="url"
                                    value={profileImage}
                                    onChange={(e) => setProfileImage(e.target.value)}
                                    placeholder="https://example.com/avatar.jpg"
                                    className="w-full px-3 py-1.5 text-xs bg-white border border-gray-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                                />
                            </div>
                        )}
                    </div>
                </div>

                {/* Full Name */}
                <div>
                    <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                        <UserIcon className="w-3.5 h-3.5 text-gray-400" /> Full Name <span className="text-rose-500">*</span>
                    </label>
                    <input
                        type="text"
                        required
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        placeholder="e.g. John Doe"
                        className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                        disabled={saving}
                    />
                </div>

                {/* Professional Headline */}
                <div>
                    <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                        Professional Headline
                    </label>
                    <input
                        type="text"
                        value={headline}
                        onChange={(e) => setHeadline(e.target.value)}
                        placeholder="e.g. Senior Backend Architect | Spring Boot Specialist"
                        className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                        disabled={saving}
                    />
                </div>

                {/* Bio */}
                <div>
                    <label className="block text-xs font-semibold text-gray-700 mb-1.5">
                        About You / Bio
                    </label>
                    <textarea
                        rows={4}
                        value={bio}
                        onChange={(e) => setBio(e.target.value)}
                        placeholder="Introduce your background, practical experience, topics you enjoy discussing, and mentorship style..."
                        className="w-full p-3.5 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                        disabled={saving}
                    />
                </div>

                {/* Location & Languages */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                            <MapPin className="w-3.5 h-3.5 text-gray-400" /> Location
                        </label>
                        <input
                            type="text"
                            value={location}
                            onChange={(e) => setLocation(e.target.value)}
                            placeholder="e.g. Bengaluru, India"
                            className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                            disabled={saving}
                        />
                    </div>

                    <div>
                        <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                            <Globe className="w-3.5 h-3.5 text-gray-400" /> Languages Spoken
                        </label>
                        <input
                            type="text"
                            value={languages}
                            onChange={(e) => setLanguages(e.target.value)}
                            placeholder="e.g. English, Hindi, Telugu"
                            className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                            disabled={saving}
                        />
                    </div>
                </div>

                {/* Education & Experience */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                            <GraduationCap className="w-3.5 h-3.5 text-gray-400" /> Education
                        </label>
                        <input
                            type="text"
                            value={education}
                            onChange={(e) => setEducation(e.target.value)}
                            placeholder="e.g. B.Tech Computer Science, IIT Bombay"
                            className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                            disabled={saving}
                        />
                    </div>

                    <div>
                        <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                            <Briefcase className="w-3.5 h-3.5 text-gray-400" /> Professional Experience
                        </label>
                        <input
                            type="text"
                            value={experience}
                            onChange={(e) => setExperience(e.target.value)}
                            placeholder="e.g. Staff Software Engineer at Google"
                            className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                            disabled={saving}
                        />
                    </div>
                </div>

                {/* Social & Professional Links */}
                <div className="pt-2 border-t border-gray-100 space-y-4">
                    <h3 className="text-sm font-bold text-gray-900">
                        Web & Social Links
                    </h3>

                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                                <LinkIcon className="w-3.5 h-3.5 text-gray-400" /> Personal Website
                            </label>
                            <input
                                type="url"
                                value={website}
                                onChange={(e) => setWebsite(e.target.value)}
                                placeholder="https://yourportfolio.com"
                                className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                                disabled={saving}
                            />
                        </div>

                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                                <LinkedinIcon className="w-3.5 h-3.5 text-indigo-500" /> LinkedIn Profile
                            </label>
                            <input
                                type="text"
                                value={linkedIn}
                                onChange={(e) => setLinkedIn(e.target.value)}
                                placeholder="https://linkedin.com/in/username"
                                className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                                disabled={saving}
                            />
                        </div>

                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                                <GithubIcon className="w-3.5 h-3.5 text-gray-700" /> GitHub Profile
                            </label>
                            <input
                                type="text"
                                value={gitHub}
                                onChange={(e) => setGitHub(e.target.value)}
                                placeholder="https://github.com/username"
                                className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                                disabled={saving}
                            />
                        </div>
                    </div>
                </div>

                {/* Privacy & Visibility */}
                <div className="pt-2 border-t border-gray-100 flex items-center justify-between">
                    <div>
                        <h4 className="text-xs font-bold text-gray-900">Public Profile Visibility</h4>
                        <p className="text-[11px] text-gray-500">
                            When enabled, learners and peers can discover your profile on Mentivo.
                        </p>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                        <input
                            type="checkbox"
                            checked={isPublic}
                            onChange={(e) => setIsPublic(e.target.checked)}
                            className="sr-only peer"
                            disabled={saving}
                        />
                        <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                    </label>
                </div>

                {/* Mentorship Settings if applicable */}
                {isMentorRole && (
                    <div className="pt-4 border-t border-gray-100 space-y-4">
                        <h2 className="text-base font-bold text-gray-900">
                            Mentorship Rates & Experience
                        </h2>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                                    <DollarSign className="w-3.5 h-3.5 text-gray-400" /> Hourly Rate (INR ₹)
                                </label>
                                <input
                                    type="number"
                                    min="0"
                                    max="10000"
                                    step="1"
                                    value={hourlyRate}
                                    onChange={(e) => setHourlyRate(e.target.value)}
                                    className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                                    disabled={saving}
                                />
                                <p className="text-[11px] text-gray-400 mt-1">
                                    {Number(hourlyRate) === 0
                                        ? 'Free Mentorship (₹0) - You offer sessions pro-bono (no platform fees).'
                                        : 'Learner pays this amount + 20% platform fee. You receive 80%.'}
                                </p>
                            </div>

                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-1.5 flex items-center gap-1">
                                    <Briefcase className="w-3.5 h-3.5 text-gray-400" /> Years of Experience
                                </label>
                                <input
                                    type="number"
                                    min="0"
                                    max="50"
                                    value={experienceYears}
                                    onChange={(e) => setExperienceYears(e.target.value)}
                                    className="w-full px-3.5 py-2 text-xs sm:text-sm border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-hidden"
                                    disabled={saving}
                                />
                            </div>
                        </div>

                        {/* Category Specialization Selection */}
                        <div>
                            <label className="block text-xs font-semibold text-gray-700 mb-2">
                                Mentor Categories (Select all that apply)
                            </label>
                            <div className="flex flex-wrap gap-2">
                                {allCategories.map((cat) => {
                                    const isSelected = selectedCategoryIds.includes(cat.id);
                                    return (
                                        <button
                                            type="button"
                                            key={cat.id}
                                            onClick={() => {
                                                setSelectedCategoryIds((prev) =>
                                                    prev.includes(cat.id) ? prev.filter((id) => id !== cat.id) : [...prev, cat.id]
                                                );
                                            }}
                                            className={`px-3 py-1.5 rounded-xl text-xs font-medium border transition-all cursor-pointer ${
                                                isSelected
                                                    ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                                                    : 'bg-white text-slate-700 border-slate-200 hover:border-indigo-300 hover:bg-slate-50'
                                            }`}
                                            disabled={saving}
                                        >
                                            {cat.name}
                                        </button>
                                    );
                                })}
                            </div>
                            <p className="text-[11px] text-gray-400 mt-1">
                                Learners filtering by these categories will discover your profile.
                            </p>
                        </div>

                        {/* Skills Selection */}
                        {allSkills.length > 0 && (
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-2">
                                    Skills & Tools
                                </label>
                                <div className="flex flex-wrap gap-1.5 max-h-36 overflow-y-auto p-2 bg-slate-50 rounded-xl border border-slate-200">
                                    {allSkills.map((skill) => {
                                        const isSelected = selectedSkillIds.includes(skill.id);
                                        return (
                                            <button
                                                type="button"
                                                key={skill.id}
                                                onClick={() => {
                                                    setSelectedSkillIds((prev) =>
                                                        prev.includes(skill.id) ? prev.filter((id) => id !== skill.id) : [...prev, skill.id]
                                                    );
                                                }}
                                                className={`px-2.5 py-1 rounded-lg text-xs font-medium border transition-all cursor-pointer ${
                                                    isSelected
                                                        ? 'bg-indigo-100 text-indigo-800 border-indigo-300 font-semibold'
                                                        : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'
                                                }`}
                                                disabled={saving}
                                            >
                                                {skill.name}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                <div className="pt-4 border-t border-gray-100 flex justify-end">
                    <Button
                        type="submit"
                        variant="primary"
                        isLoading={saving}
                        disabled={saving}
                        className="flex items-center gap-2 px-6 py-2.5 rounded-xl cursor-pointer"
                    >
                        <Save className="w-4 h-4" /> Save Profile Changes
                    </Button>
                </div>
            </form>
        </div>
    );
};

export default ProfileSettingsPage;
