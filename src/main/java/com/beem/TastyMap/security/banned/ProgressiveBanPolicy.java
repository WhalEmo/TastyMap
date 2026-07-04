package com.beem.TastyMap.security.banned;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProgressiveBanPolicy {

    public LocalDateTime calculateBanReleaseTime(int previousViolations) {
        if (previousViolations == 0) {
            return LocalDateTime.now().plusHours(24);
        } else if (previousViolations == 1) {
            return LocalDateTime.now().plusDays(7);
        } else {
            return LocalDateTime.now().plusDays(30);
        }
    }
}
