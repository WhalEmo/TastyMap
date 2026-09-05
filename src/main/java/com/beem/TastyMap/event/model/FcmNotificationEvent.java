package com.beem.TastyMap.event.model;

import java.util.Map;

public class FcmNotificationEvent {
    private final Long userId; // Bildirimin gideceği kişi
    private final String titleCode; // Çoklu dil için başlık kodu
    private final String bodyCode; // Çoklu dil için içerik kodu
    private final Object[] bodyArgs; // İçeriğe gömülecek değişkenler (Şehir veya Kullanıcı Adı)
    private final Map<String, String> payloadData; // Mobile gidecek gizli veriler (type, userId vb.)

    public FcmNotificationEvent(Long userId, String titleCode, String bodyCode,
                                Object[] bodyArgs, Map<String, String> payloadData) {
        this.userId = userId;
        this.titleCode = titleCode;
        this.bodyCode = bodyCode;
        this.bodyArgs = bodyArgs;
        this.payloadData = payloadData;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitleCode() {
        return titleCode;
    }

    public String getBodyCode() {
        return bodyCode;
    }

    public Object[] getBodyArgs() {
        return bodyArgs;
    }

    public Map<String, String> getPayloadData() {
        return payloadData;
    }
}