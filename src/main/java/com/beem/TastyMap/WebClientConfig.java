package com.beem.TastyMap;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        ConnectionProvider provider = ConnectionProvider.builder("google-places-pool")
                .maxConnections(50)                             // Google'a aynı anda 50 paralel soket
                .pendingAcquireMaxCount(200)                    // Soketler meşgulse 200 alt isteğe kadar kuyrukta bekle
                .pendingAcquireTimeout(Duration.ofSeconds(5))   // Kuyrukta en fazla 5 sn bekle (zaten 200-300ms'de boşalır)
                .maxIdleTime(Duration.ofSeconds(15))            // 15 sn boş duran soketi temizle (Google kapatmadan biz kapatalım)
                .maxLifeTime(Duration.ofSeconds(60))            // 60 sn'den yaşlı soketi yenile
                .evictInBackground(Duration.ofSeconds(20))      // Arka planda ölü soketleri süpür
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(8))
                .keepAlive(true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}