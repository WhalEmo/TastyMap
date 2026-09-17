package com.beem.TastyMap.socialnotificaiton.model;

public enum NotificationActionStatus {
    NONE,      // Takip isteği dışındaki genel bildirimler için (Beğeni, Yorum vs.)
    PENDING,   // Henüz onaylanmadı/reddedilmedi (Butonlar görünür)
    ACCEPTED,  // Onaylandı ("Takip isteğini kabul ettin" yazar)
    REJECTED   // Reddedildi
}
