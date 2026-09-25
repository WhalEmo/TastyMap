package com.beem.TastyMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/profiles/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Tüm yüklenen dosyalar (profiles, posts vb.) için genel dizin yönlendirmesi
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }

        // "/uploads/**" isteği geldiğinde diskteki "uploads/" klasörünün altına yönlendirir.
        // Örn: /uploads/posts/photo.jpg -> <proje_dizini>/uploads/posts/photo.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath)
                .setCachePeriod(3600)
                .resourceChain(false);

        // 2. .well-known dizini yönlendirmesi
        registry.addResourceHandler("/.well-known/**")
                .addResourceLocations("classpath:/static/.well-known/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:8081",
                        "https://coleman-nonethic-marinda.ngrok-free.dev"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}