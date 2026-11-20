package com.beem.TastyMap.support.dto;

import com.beem.TastyMap.support.model.SupportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateSupportTicketRequest {

    @NotNull(message = "{validation.support.category.notblank}")
    private SupportCategory category;
    @NotBlank(message = "{validation.support.message.notblank}")
    @Size(min = 10, max = 500, message = "{validation.support.message.size}")
    private String message;

    @Size(max = 20)
    private String appVersion;

    @Size(max = 150)
    private String deviceInfo;

    public SupportCategory getCategory() { return category; }
    public void setCategory(SupportCategory category) { this.category = category; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
}