package com.mentivo.service;

import com.mentivo.entity.PlatformSetting;
import com.mentivo.repository.PlatformSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FeatureFlagService {

    public static final String KEY_MESSAGING = "FEATURE_MESSAGING";
    public static final String KEY_VIDEO_CALLING = "FEATURE_VIDEO_CALLING";

    private final PlatformSettingRepository settingRepository;

    @Value("${features.messaging:true}")
    private boolean defaultMessaging;

    @Value("${features.video-calling:true}")
    private boolean defaultVideoCalling;

    public FeatureFlagService(PlatformSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public boolean isMessagingActive() {
        return settingRepository.findById(KEY_MESSAGING)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue()))
                .orElse(defaultMessaging);
    }

    public boolean isVideoCallingActive() {
        return settingRepository.findById(KEY_VIDEO_CALLING)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue()))
                .orElse(defaultVideoCalling);
    }

    public Map<String, Boolean> getFeatures() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("messaging", isMessagingActive());
        features.put("videoCalling", isVideoCallingActive());
        return features;
    }

    @Transactional
    public void updateFeature(String featureName, boolean enabled) {
        String key;
        String desc;
        if ("messaging".equalsIgnoreCase(featureName) || KEY_MESSAGING.equalsIgnoreCase(featureName)) {
            key = KEY_MESSAGING;
            desc = "Direct Messaging feature active flag";
        } else if ("videoCalling".equalsIgnoreCase(featureName) || "video-calling".equalsIgnoreCase(featureName) || KEY_VIDEO_CALLING.equalsIgnoreCase(featureName)) {
            key = KEY_VIDEO_CALLING;
            desc = "1-to-1 Video Calling feature active flag";
        } else {
            throw new IllegalArgumentException("Unknown feature flag: " + featureName);
        }

        PlatformSetting setting = settingRepository.findById(key)
                .orElse(new PlatformSetting(key, String.valueOf(enabled), desc));
        setting.setSettingValue(String.valueOf(enabled));
        settingRepository.save(setting);
    }
}
