package com.mentivo.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class SettingUpdateDto {

    @NotBlank(message = "Setting key is required")
    private String settingKey;

    @NotBlank(message = "Setting value is required")
    private String settingValue;

    private String description;

    public SettingUpdateDto() {}

    public SettingUpdateDto(String settingKey, String settingValue, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
