package com.beem.TastyMap.user.subscribe.model;

public enum RelationStatus {
    SELF,           // Kendi profilim
    FOLLOWING,      // Takip ediyorum (ACCEPTED)
    PENDING,        // Takip isteği attım, onay bekliyor (PENDING)
    FOLLOW_BACK,    // O beni takip ediyor, ben etmiyorum
    NOT_FOLLOWING   // Arada hiçbir takip ilişkisi yok
}
