package com.beem.TastyMap.security.banned;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class BanDurationFormatter {

    private final MessageSource messageSource;

    public BanDurationFormatter(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private String getMessage(String code, Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    public String formatBanDuration(LocalDateTime bannedUntil) {
        long totalMinutes = Duration.between(LocalDateTime.now(), bannedUntil).toMinutes();

        if (totalMinutes <= 0) {
            return getMessage("ban.duration.zero", null);
        }

        if (totalMinutes < 60) {
            return getMessage("ban.duration.minutes", new Object[]{totalMinutes});
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (minutes == 0) {
            return getMessage("ban.duration.hours", new Object[]{hours});
        }

        return getMessage("ban.duration.hours.and.minutes", new Object[]{hours, minutes});
    }
}