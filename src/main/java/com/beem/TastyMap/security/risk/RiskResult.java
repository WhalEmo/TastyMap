package com.beem.TastyMap.security.risk;

import com.beem.TastyMap.security.device.UserDeviceDTO;

public class RiskResult {
    private final int score;
    private final UserDeviceDTO deviceDto;

    public RiskResult(int score, UserDeviceDTO deviceDto) {
        this.score = score;
        this.deviceDto = deviceDto;
    }
    public int getScore() { return score; }
    public UserDeviceDTO getDeviceDto() { return deviceDto; }
}
