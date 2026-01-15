package com.beem.TastyMap.Maps.Data;

import com.beem.TastyMap.Maps.Entity.PhotoEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Photo {
    private Integer height;
    private Integer width;
    private String photo_reference;

    public Photo() {
    }

    public Integer getHeight() {
        return height;
    }

    public static Photo fromEntity(PhotoEntity entity){
        Photo photo = new Photo();
        photo.setHeight(entity.getHeight());
        photo.setWidth(entity.getWidth());
        photo.setPhoto_reference(entity.getPhotoReference());
        return photo;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getPhoto_reference() {
        return photo_reference;
    }

    public void setPhoto_reference(String photo_reference) {
        this.photo_reference = photo_reference;
    }
}
