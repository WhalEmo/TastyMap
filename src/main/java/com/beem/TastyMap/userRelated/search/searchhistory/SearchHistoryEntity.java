package com.beem.TastyMap.userRelated.search.searchhistory;

import com.beem.TastyMap.registerLogin.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_history")
public class SearchHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //araayan kisi yanı ben
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "searcher_id", nullable = false)
    private UserEntity searcher;

    // aranan kisi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "searched_user_id", nullable = false)
    private UserEntity searchedUser;

    @Column(nullable = false)
    private LocalDateTime searchedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getSearcher() {
        return searcher;
    }

    public void setSearcher(UserEntity searcher) {
        this.searcher = searcher;
    }

    public UserEntity getSearchedUser() {
        return searchedUser;
    }

    public void setSearchedUser(UserEntity searchedUser) {
        this.searchedUser = searchedUser;
    }

    public LocalDateTime getSearchedAt() {
        return searchedAt;
    }

    public void setSearchedAt(LocalDateTime searchedAt) {
        this.searchedAt = searchedAt;
    }
}
