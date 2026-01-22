package com.beem.TastyMap.Maps.Entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "grids",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"center_lat", "center_lng"})
        }
)
public class GridEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "center_lat", nullable = false, precision = 8, scale = 4)
    private BigDecimal centerLat;

    @Column(name = "center_lng", nullable = false, precision = 8, scale = 4)
    private BigDecimal centerLng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GridStatus status;

    @Column(nullable = false)
    private Integer scanCount = 0;

    private LocalDateTime lastScannedAt;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "grid")
    private List<PlaceEntity> places = new ArrayList<>();



    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = GridStatus.EMPTY;
    }


    public void markScanned(GridStatus newStatus) {
        this.status = newStatus;
        this.scanCount++;
        this.lastScannedAt = LocalDateTime.now();
    }

    public GridEntity() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(BigDecimal centerLat) {
        this.centerLat = centerLat;
    }

    public BigDecimal getCenterLng() {
        return centerLng;
    }

    public void setCenterLng(BigDecimal centerLng) {
        this.centerLng = centerLng;
    }

    public GridStatus getStatus() {
        return status;
    }

    public void setStatus(GridStatus status) {
        this.status = status;
    }

    public Integer getScanCount() {
        return scanCount;
    }

    public void setScanCount(Integer scanCount) {
        this.scanCount = scanCount;
    }

    public LocalDateTime getLastScannedAt() {
        return lastScannedAt;
    }

    public void setLastScannedAt(LocalDateTime lastScannedAt) {
        this.lastScannedAt = lastScannedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<PlaceEntity> getPlaces() {
        return places;
    }

    public void setPlaces(List<PlaceEntity> places) {
        this.places = places;
    }
}
