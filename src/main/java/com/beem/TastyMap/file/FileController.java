package com.beem.TastyMap.file;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private static final List<String> ALLOWED_FOLDERS = List.of("profiles", "posts");

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/upload/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSingleFile(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {

        if (!ALLOWED_FOLDERS.contains(type.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Geçersiz yükleme tipi."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dosya seçilmelidir."));
        }

        try {
            String imageUrl = fileStorageService.saveFile(file, type.toLowerCase());
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Hata: " + e.getMessage()));
        }
    }

    // YENİ: Çoklu Dosya Yükleme Endpoint'i
    @PostMapping(value = "/upload-multiple/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMultipleFiles(
            @PathVariable String type,
            @RequestParam("files") List<MultipartFile> files) {

        if (!ALLOWED_FOLDERS.contains(type.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Geçersiz yükleme tipi."));
        }

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "En az bir dosya seçilmelidir."));
        }

        try {
            List<String> imageUrls = fileStorageService.saveFiles(files, type.toLowerCase());
            // Yanıt olarak URL LİSTESİ dönüyoruz
            return ResponseEntity.ok(Map.of("imageUrls", imageUrls));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Dosyalar kaydedilirken hata oluştu: " + e.getMessage()));
        }
    }
}