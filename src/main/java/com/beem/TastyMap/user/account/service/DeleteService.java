package com.beem.TastyMap.user.account.service;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.user.account.entity.UserEntity;
import com.beem.TastyMap.user.account.repo.UserRepo;
import com.beem.TastyMap.user.account.dto.DeleteAccountRequest;
import com.beem.TastyMap.security.refreshtoken.repo.RefreshTokenRepo;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public class DeleteService {
    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    public DeleteService(UserRepo userRepo, RefreshTokenRepo refreshTokenRepo, PasswordEncoder passwordEncoder, MessageSource messageSource) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public void deleteAccount(Long currentUserId, DeleteAccountRequest request) {

        UserEntity user = userRepo.findById(currentUserId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomExceptions.InvalidCredentialsException(
                    getMessage("user.login.invalid.credentials")
            );
        }

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());

        userRepo.save(user);

        refreshTokenRepo.revokeAllUserTokens(user.getId());
    }
}
