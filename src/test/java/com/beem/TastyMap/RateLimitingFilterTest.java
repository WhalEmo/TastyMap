package com.beem.TastyMap;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RateLimitingFilterTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    @DisplayName("Anonim IP kotası: 60 istekten sonra 429 dönmeli")
    void shouldBlockAnonymousUserAfter60Requests() throws Exception {
        String testEndpoint = "/auth/check-password?userId=2";
        // Redis'teki önceki testlerle çakışmaması için teste özel sahte bir IP atıyoruz
        String testIp = "192.168.10.50";

        for (int i = 1; i <= 60; i++) {
            mockMvc.perform(get(testEndpoint)
                            .with(request -> {
                                request.setRemoteAddr(testIp);
                                return request;
                            }))
                    .andExpect(status().isOk());
        }

        // 61. istekte 429 ve özel JSON gövdesi beklenir
        mockMvc.perform(get(testEndpoint)
                        .with(request -> {
                            request.setRemoteAddr(testIp);
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    @DisplayName("Anonim IP kotası: Kota bittiğinde 429 dönmeli")
    void shouldBlockAnonymousUserWhenLimitExceeded() throws Exception {
        String testEndpoint = "/auth/check-password?userId=2";
        // Her çalıştırmada sıfır sayaç için benzersiz IP
        String testIp = "192.168.10." + (int)(Math.random() * 200 + 10);

        int blockedAtRequest = -1;

        // Greedy saniyede 1 jeton doldurabileceği için 75 istek gönderiyoruz
        for (int i = 1; i <= 75; i++) {
            int statusCode = mockMvc.perform(get(testEndpoint)
                            .with(request -> {
                                request.setRemoteAddr(testIp);
                                return request;
                            }))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            if (statusCode == 429) {
                blockedAtRequest = i;
                break;
            }
        }

        System.out.println("--> SISTEM " + blockedAtRequest + ". ISTEKTE RATE LIMIT'E TAKILDI!");

        // 60 kotası olan sistemin 60 ile 65 arasında engellemesi beklenir
        org.junit.jupiter.api.Assertions.assertTrue(blockedAtRequest >= 60 && blockedAtRequest <= 65,
                "Beklenen aralıkta engellenmedi! Engellendiği istek: " + blockedAtRequest);
    }
}