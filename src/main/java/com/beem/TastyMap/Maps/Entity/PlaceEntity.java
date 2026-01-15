package com.beem.TastyMap.Maps.Entity;

import com.beem.TastyMap.Maps.Data.PlaceResult;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "places")
public class PlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String placeId;

    @Column(nullable = false)
    private String name;

    private String vicinity;

    private Double rating;

    private Integer priceLevel;

    private Integer userRatingsTotal;

    private String businessStatus;

    private Double latitude;

    private Double longitude;

    @Column(name = "grid_lat", nullable = false, precision = 8, scale = 4)
    private BigDecimal gridLat;

    @Column(name = "grid_lng", nullable = false, precision = 8, scale = 4)
    private BigDecimal gridLng;

    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "place_types", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "type")
    private Set<String> types;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoEntity> photos;


    public PlaceEntity() {
    }

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public static PlaceEntity fromDto(PlaceResult dto) {
        PlaceEntity entity = new PlaceEntity();

        entity.setPlaceId(dto.getPlace_id());
        entity.setName(dto.getName());
        entity.setVicinity(dto.getVicinity());
        entity.setRating(dto.getRating());
        entity.setPriceLevel(dto.getPrice_level());
        entity.setUserRatingsTotal(dto.getUser_ratings_total());
        entity.setBusinessStatus(dto.getBusiness_status());

        if (dto.getGeometry() != null) {
            entity.setLatitude(dto.getGeometry().getLocation().getLat());
            entity.setLongitude(dto.getGeometry().getLocation().getLng());
        }


        entity.setTypes(new HashSet<>(dto.getTypes()));

        if (dto.getPhotos() != null) {
            List<PhotoEntity> photos = dto.getPhotos().stream()
                    .map(p -> {
                        PhotoEntity photo = new PhotoEntity();
                        photo.setPhotoReference(p.getPhoto_reference());
                        photo.setWidth(p.getWidth());
                        photo.setHeight(p.getHeight());
                        photo.setPlace(entity);
                        return photo;
                    }).toList();
            entity.setPhotos(photos);
        }

        return entity;
    }

    public BigDecimal getGridLat() {
        return gridLat;
    }

    public void setGridLat(BigDecimal gridLat) {
        this.gridLat = gridLat;
    }

    public BigDecimal getGridLng() {
        return gridLng;
    }

    public void setGridLng(BigDecimal gridLng) {
        this.gridLng = gridLng;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public List<PhotoEntity> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PhotoEntity> photos) {
        this.photos = photos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVicinity() {
        return vicinity;
    }

    public void setVicinity(String vicinity) {
        this.vicinity = vicinity;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
    }

    public Integer getUserRatingsTotal() {
        return userRatingsTotal;
    }

    public void setUserRatingsTotal(Integer userRatingsTotal) {
        this.userRatingsTotal = userRatingsTotal;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
