package com.beem.TastyMap.support.service;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.support.dto.CreateSupportTicketRequest;
import com.beem.TastyMap.support.entity.SupportTicketEntity;
import com.beem.TastyMap.support.repo.SupportTicketRepo;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class SupportTicketService {

    private final SupportTicketRepo supportTicketRepo;
    private final MessageSource messageSource;

    public SupportTicketService(SupportTicketRepo supportTicketRepo, MessageSource messageSource) {
        this.supportTicketRepo = supportTicketRepo;
        this.messageSource = messageSource;
    }
    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public void createTicket(Long userId, CreateSupportTicketRequest request) {
        long recentCount = supportTicketRepo.countByUserIdAndCreatedAtAfter(
                userId, LocalDateTime.now().minusHours(24)
        );
        if (recentCount >= 5) {
            throw new CustomExceptions.TooManyRequestsException(getMessage("support.limit"));
        }

        SupportTicketEntity ticket = new SupportTicketEntity();
        ticket.setUserId(userId);
        ticket.setCategory(request.getCategory());
        ticket.setMessage(request.getMessage().trim());
        ticket.setAppVersion(request.getAppVersion());
        ticket.setDeviceInfo(request.getDeviceInfo());

        supportTicketRepo.save(ticket);
    }
}