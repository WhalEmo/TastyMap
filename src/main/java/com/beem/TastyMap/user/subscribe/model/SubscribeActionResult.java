package com.beem.TastyMap.user.subscribe.model;


public record SubscribeActionResult(
        Long targetUserId,
        RelationStatus relationStatus,// Butonun dönüşeceği durum (FOLLOWING, FOLLOW_BACK, PENDING, NOT_FOLLOWING)
        boolean hasPendingIncomingRequest, // Karşı tarafın size attığı bekleyen istek var mı?,
        boolean isFollower

) {}