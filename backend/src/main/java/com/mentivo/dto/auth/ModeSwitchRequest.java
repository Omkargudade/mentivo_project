package com.mentivo.dto.auth;

import com.mentivo.entity.enums.CurrentMode;
import jakarta.validation.constraints.NotNull;

public class ModeSwitchRequest {

    @NotNull(message = "Mode is required (LEARNER or MENTOR)")
    private CurrentMode mode;

    public ModeSwitchRequest() {}

    public CurrentMode getMode() { return mode; }
    public void setMode(CurrentMode mode) { this.mode = mode; }
}
