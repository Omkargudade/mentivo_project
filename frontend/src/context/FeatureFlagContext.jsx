import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import api from '../api/client';
const defaultFeatures = {
    messaging: true,
    videoCalling: true,
};
const FeatureFlagContext = createContext({
    features: defaultFeatures,
    isLoading: false,
    refreshFeatures: async () => { },
    updateFeature: async () => false,
});
export const FeatureFlagProvider = ({ children }) => {
    const [features, setFeatures] = useState(defaultFeatures);
    const [isLoading, setIsLoading] = useState(true);
    const refreshFeatures = useCallback(async () => {
        try {
            const res = await api.get('/features');
            if (res.data?.success && res.data.data) {
                setFeatures({
                    messaging: res.data.data.messaging ?? true,
                    videoCalling: res.data.data.videoCalling ?? true,
                });
            }
        }
        catch (err) {
            console.warn('Could not load feature flags from server, using defaults', err);
            setFeatures(defaultFeatures);
        }
        finally {
            setIsLoading(false);
        }
    }, []);
    const updateFeature = useCallback(async (featureName, enabled) => {
        try {
            const res = await api.put(`/admin/features/${featureName}?enabled=${enabled}`);
            if (res.data?.success) {
                setFeatures((prev) => ({
                    ...prev,
                    [featureName === 'messaging' ? 'messaging' : 'videoCalling']: enabled,
                }));
                return true;
            }
            return false;
        }
        catch (err) {
            console.error('Failed to update feature flag', err);
            return false;
        }
    }, []);
    useEffect(() => {
        refreshFeatures();
    }, [refreshFeatures]);
    return (<FeatureFlagContext.Provider value={{ features, isLoading, refreshFeatures, updateFeature }}>
      {children}
    </FeatureFlagContext.Provider>);
};
export const useFeatureFlags = () => useContext(FeatureFlagContext);
