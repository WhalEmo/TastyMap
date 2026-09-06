package com.beem.TastyMap.userRelated.socialnotifications.enums;

public enum NotificationActionStatus {
    NONE,      // Takip isteği dışındaki genel bildirimler için (Beğeni, Yorum vs.)
    PENDING,   // Henüz onaylanmadı/reddedilmedi (Butonlar görünür)
    ACCEPTED,  // Onaylandı ("Takip isteğini kabul ettin" yazar)
    REJECTED   // Reddedildi
}
