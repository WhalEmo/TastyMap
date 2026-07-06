package com.beem.TastyMap.security.banned;

import java.time.Duration;
import java.time.LocalDateTime;

public class BanDurationFormatter {
    public static String formatBanDuration(LocalDateTime bannedUntil) {
        long totalMinutes = Duration.between(LocalDateTime.now(), bannedUntil).toMinutes();

        if (totalMinutes <= 0) {
            return "0 dakika";
        }

        if (totalMinutes < 60) {
            return totalMinutes + " dakika";
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (minutes == 0) {
            return hours + " saat";
        }

        return hours + " saat " + minutes + " dakika";
    }
}
