package com.beem.TastyMap.security.banned;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProgressiveBanPolicy {

    public LocalDateTime calculateBanReleaseTime(int previousViolations) {
        if (previousViolations == 0) {
            return LocalDateTime.now().plusMinutes(30);
        } else if (previousViolations == 1) {
            return LocalDateTime.now().plusHours(24);
        } else if(previousViolations == 2){
            return LocalDateTime.now().plusDays(30);
        }else{
            return null;
        }
    }
}
