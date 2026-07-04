package com.beem.TastyMap;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RateLimitingFilterTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testRateLimitingMechanisms_ShouldReturn429TooManyRequests() {
        String targetUrl = "/search?searchText=çiğköfte&page=0&size=20";

        int requestCount = 25;

        boolean targetHit429 = false;

        System.out.println("=== RATE LIMIT TESTI BAŞLIYOR ===");

        for (int i = 1; i <= requestCount; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(targetUrl, String.class);

            System.out.println(i + ". Istek Durumu: " + response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                targetHit429 = true;
                System.out.println("--> BAŞARILI: Sistem " + i + ". istekte rate limite takildi ve 429 firlatti!");
                break;
            }
        }

        System.out.println("=== TEST SONA ERDİ ===");
        assertThat(targetHit429).isTrue();
    }
}