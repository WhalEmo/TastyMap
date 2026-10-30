package com.beem.TastyMap.rag.factory;

import com.beem.TastyMap.rag.data.enums.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AiProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    private final Map<AiProvider, ChatClient> clientRegistry = new EnumMap<>(AiProvider.class);
    private final AiProvider defaultProvider;

    public AiProviderFactory(
            @Qualifier("groqChatClient") ChatClient groqClient,
            @Qualifier("evrenChatClient") ChatClient evrenClient,
            @Qualifier("ollamaChatClient") ChatClient ollamaClient,
            @Qualifier("ollamaIndexerChatClient") ChatClient ollamaIndexerClient,
            @Value("${tastymap.ai.active-provider:groq}") String configuredProvider
    ) {
        this.clientRegistry.put(AiProvider.GROQ, groqClient);
        this.clientRegistry.put(AiProvider.EVREN, evrenClient);
        this.clientRegistry.put(AiProvider.OLLAMA, ollamaClient);
        this.clientRegistry.put(AiProvider.OLLAMA_INDEXER, ollamaIndexerClient);

        this.defaultProvider = AiProvider.fromString(configuredProvider);
        log.info("AiProviderFactory ayağa kalktı. Varsayılan Sağlayıcı: {}", this.defaultProvider);
    }

    /**
     * application.properties üzerinden belirlenen varsayılan sağlayıcıyı döner.
     */
    public ChatClient getDefaultClient() {
        return getClient(this.defaultProvider);
    }

    /**
     * İstemcinin spesifik talebine göre istemci döner (Örn: mobil uygulamadan seçilen motor).
     * İstenen motor null veya geçersizse sessizce varsayılana fallback yapar.
     */
    public ChatClient getClient(AiProvider provider) {
        if (provider == null) {
            return clientRegistry.get(this.defaultProvider);
        }

        ChatClient client = clientRegistry.get(provider);
        if (client == null) {
            log.warn("İstenen AI Sağlayıcı ({}) bulunamadı. Varsayılana ({}) dönülüyor.", provider, defaultProvider);
            return clientRegistry.get(this.defaultProvider);
        }

        return client;
    }

    public AiProvider getDefaultProvider() {
        return this.defaultProvider;
    }
}