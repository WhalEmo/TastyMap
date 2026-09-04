package com.beem.TastyMap.userRelated.subscribe;


public record SubscribeActionResult(
        Long targetUserId,
        RelationStatus relationStatus,        // Butonun dönüşeceği durum (FOLLOWING, FOLLOW_BACK, PENDING, NOT_FOLLOWING)
        boolean hasPendingIncomingRequest  // Karşı tarafın size attığı bekleyen istek var mı?
) {}