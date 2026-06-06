package com.beem.TastyMap.maps.entity;

import com.beem.TastyMap.maps.data.PlaceDetailsResult;
import com.beem.TastyMap.maps.data.PlaceResult;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import jakarta.persistence.*;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Indexed
@Entity
@Table(name = "places")
public class PlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @DocumentId
    @GenericField(projectable = Projectable.YES)
    private Long id;

    @Column(name = "place_id", nullable = false, unique = true)
    private String placeId;

    @FullTextField(projectable = Projectable.YES)
    @Column(nullable = false)
    private String name;

    @FullTextField(projectable = Projectable.YES)
    private String vicinity;

    private Double rating;

    private Integer priceLevel;

    private Integer userRatingsTotal;

    private String businessStatus;

    private String formattedPhoneNumber;
    private String internationalPhoneNumber;
    private String website;

    @Embedded
    private OpeningHoursEmbeddable openingHours;

    @OneToMany(
            mappedBy = "place",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReviewEntity> reviews = new ArrayList<>();

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

    private String formattedAddress;


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
        return entity;
    }

    public static PlaceEntity fromDetailsDto(PlaceDetailsResult dto){
        PlaceEntity entity = new PlaceEntity();

        entity.setPlaceId(dto.getPlace_id());
        entity.setName(dto.getName());

        entity.setRating(dto.getRating());
        entity.setPriceLevel(dto.getPrice_level());
        entity.setUserRatingsTotal(dto.getUser_ratings_total());



        if (dto.getGeometry() != null) {
            entity.setLatitude(dto.getGeometry().getLocation().getLat());
            entity.setLongitude(dto.getGeometry().getLocation().getLng());
        }

        entity.setTypes(new HashSet<>(dto.getTypes()));

        entity.setFormattedPhoneNumber(dto.getFormatted_phone_number());
        entity.setInternationalPhoneNumber(dto.getInternational_phone_number());
        entity.setWebsite(dto.getWebsite());


        entity.openingHours = new OpeningHoursEmbeddable(
                dto.getOpening_hours().getOpen_now(),
                dto.getOpening_hours().getWeekday_text()
        );

        entity.setFormattedAddress(dto.getFormatted_address());

        return entity;
    }

    public void updateFromDetailsDto(PlaceDetailsResult dto) {
        this.setName(dto.getName());
        this.setRating(dto.getRating());
        this.setPriceLevel(dto.getPrice_level());
        this.setUserRatingsTotal(dto.getUser_ratings_total());

        if (dto.getGeometry() != null) {
            this.setLatitude(dto.getGeometry().getLocation().getLat());
            this.setLongitude(dto.getGeometry().getLocation().getLng());
        }

        if (dto.getTypes() != null) {
            this.setTypes(new HashSet<>(dto.getTypes()));
        }

        this.setFormattedPhoneNumber(dto.getFormatted_phone_number());
        this.setInternationalPhoneNumber(dto.getInternational_phone_number());
        this.setWebsite(dto.getWebsite());
        this.setFormattedAddress(dto.getFormatted_address());

        if (dto.getOpening_hours() != null) {
            this.setOpeningHours(new OpeningHoursEmbeddable(
                    dto.getOpening_hours().getOpen_now(),
                    dto.getOpening_hours().getWeekday_text()
            ));
        }
    }

    public void addPhoto(PhotoEntity photo) {
        photos.add(photo);
        photo.setPlace(this);
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

    public String getFormattedPhoneNumber() {
        return formattedPhoneNumber;
    }

    public void setFormattedPhoneNumber(String formattedPhoneNumber) {
        this.formattedPhoneNumber = formattedPhoneNumber;
    }

    public String getInternationalPhoneNumber() {
        return internationalPhoneNumber;
    }

    public void setInternationalPhoneNumber(String internationalPhoneNumber) {
        this.internationalPhoneNumber = internationalPhoneNumber;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public OpeningHoursEmbeddable getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(OpeningHoursEmbeddable openingHours) {
        if(this.openingHours.equals(openingHours)){
            return;
        }
        this.openingHours = openingHours;
    }

    public List<ReviewEntity> getReviews() {
        return reviews;
    }


    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }


    public void setPhotos(List<PhotoEntity> photos) {
        this.photos = photos;
    }

    public void setReviews(List<ReviewEntity> reviews) {
        this.reviews = reviews;
    }
}
