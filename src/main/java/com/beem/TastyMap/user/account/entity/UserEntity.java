package com.beem.TastyMap.user.account.entity;

import com.beem.TastyMap.user.account.model.DeleteReason;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import java.time.LocalDateTime;

@Indexed
@Entity
@Table(name = "users",uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@SQLRestriction("is_deleted = false")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @DocumentId
    @GenericField(projectable = Projectable.YES)
    private Long id;

    @FullTextField(projectable = Projectable.YES)
    @Column(nullable = false,unique = true,length = 20)
    private String username;

    @FullTextField(projectable = Projectable.YES)
    @Column(nullable = false,length = 20)
    private String name;

    @FullTextField(projectable = Projectable.YES)
    @Column(nullable = false,length = 20)
    private String surname;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(nullable = true)
    @GenericField(projectable = Projectable.YES, searchable = Searchable.NO)
    private String profile;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = true, length = 200)
    @FullTextField(projectable = Projectable.YES)
    private String biography;

    @JsonProperty("privateProfile")
    @Column(nullable = false)
    private boolean privateProfile = false;

    private LocalDateTime lastInteractionAt;

    private boolean emailVerified = true;

    @Column(nullable = false)
    private long postCount = 0;

    @Column(nullable = false)
    private long subscriberCount = 0;

    @Column(nullable = false)
    private long subscribedCount = 0;

    @Column(nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    private DeleteReason deleteReasonType;

    @Column(length = 500)
    private String customDeleteReason;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt = null;

    public LocalDateTime getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void setLastInteractionAt(LocalDateTime lastInteractionAt) {
        this.lastInteractionAt = lastInteractionAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isPrivateProfile() {
        return privateProfile;
    }

    public void setPrivateProfile(boolean privateProfile) {
        this.privateProfile = privateProfile;
    }

    public long getPostCount() {
        return postCount;
    }

    public void setPostCount(long postCount) {
        this.postCount = postCount;
    }

    public long getSubscriberCount() {
        return subscriberCount;
    }

    public void setSubscriberCount(long subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    public long getSubscribedCount() {
        return subscribedCount;
    }

    public void setSubscribedCount(long subscribedCount) {
        this.subscribedCount = subscribedCount;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public DeleteReason getDeleteReasonType() {
        return deleteReasonType;
    }

    public void setDeleteReasonType(DeleteReason deleteReasonType) {
        this.deleteReasonType = deleteReasonType;
    }

    public String getCustomDeleteReason() {
        return customDeleteReason;
    }

    public void setCustomDeleteReason(String customDeleteReason) {
        this.customDeleteReason = customDeleteReason;
    }
}
