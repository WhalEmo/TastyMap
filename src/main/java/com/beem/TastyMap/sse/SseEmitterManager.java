package com.beem.TastyMap.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterManager {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterManager.class);

    private final Map<Long, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    private static final Long SSE_TIMEOUT = 120_000L;

    public SseEmitterManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter createEmitter(Long userId) {
        removeEmitter(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(()-> {
            log.debug("🔌 [SSE] Bağlantı tamamlandı (User: {})", userId);
            activeEmitters.remove(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("⏱️ [SSE] Zaman aşımı, soket kapatılıyor (User: {})", userId);
            emitter.complete();
            activeEmitters.remove(userId,emitter);
        });

        emitter.onError(ex -> {
            log.warn("⚠️ [SSE] İstemci ayrıldı / soket koptu (User: {}): {}", userId, ex.getMessage());
            activeEmitters.remove(userId, emitter);
        });

        activeEmitters.put(userId, emitter);

        return emitter;
    }

    public void sendToUser(Long userId, boolean isComplete, String name, Object payload) {
        SseEmitter emitter = activeEmitters.get(userId);
        if (emitter == null) {
            log.debug("ℹ️ [SSE] Kullanıcının aktif SSE bağlantısı yok (User: {})", userId);
            return;
        }

        synchronized (emitter) {
            try {

                String safetyName = (name != null) ? name.toLowerCase(Locale.ROOT) : "message";

                emitter.send(SseEmitter.event()
                        .name(safetyName)
                        .data(payload, MediaType.APPLICATION_JSON)
                );

                if (isComplete) {
                    emitter.complete();
                    activeEmitters.remove(userId, emitter);
                }

            } catch (IOException e) {
                log.warn("⚠️ [SSE] Veri gönderilemedi, soket düşmüş (User: {}): {}", userId, e.getMessage());
                emitter.complete();
                activeEmitters.remove(userId, emitter);
            } catch (Exception e) {
                log.error("❌ [SSE] Beklenmeyen paketleme hatası: {}", e.getMessage());
            }
        }
    }

    private void removeEmitter(Long userId) {
        SseEmitter old = activeEmitters.remove(userId);
        if (old != null) {
            try {
                old.complete();
            } catch (Exception e) {
                //LOGLANABİLİR
            }
        }
    }

    public boolean isClientConnected(Long userId) {
        return activeEmitters.containsKey(userId);
    }
}
