package com.beem.TastyMap.Maps.Entity;

import com.beem.TastyMap.Maps.Data.PlaceResult;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Entity
@Table(name = "places")
public class PlaceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false, unique = true)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grid_id", nullable = false)
    private GridEntity grid;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "place_types",
            joinColumns = @JoinColumn(name = "place_id")
    )
    @Column(name = "type")
    private Set<String> types = new HashSet<>();

    @OneToMany(
            mappedBy = "place",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PhotoEntity> photos = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    public static PlaceEntity fromDto(PlaceResult dto) {
        PlaceEntity entity = new PlaceEntity();

        if(dto.getId() != null) entity.setId(dto.getId());

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
                    })
                    .collect(Collectors.toList());
            entity.setPhotos(photos);
        }


        return entity;
    }

    public static List<PlaceEntity> fromDtoList(List<PlaceResult> dtoList){
        return dtoList
                .stream()
                .map(PlaceEntity::fromDto)
                .toList();
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
        this.types.clear();
        if (types != null) {
            this.types.addAll(types);
        }
    }

    public List<PhotoEntity> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PhotoEntity> photos) {
        this.photos.clear();
        if (photos != null) {
            this.photos.addAll(photos);
        }
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

    public GridEntity getGrid() {
        return grid;
    }

    public void setGrid(GridEntity grid) {
        this.grid = grid;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
