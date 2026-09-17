package com.beem.TastyMap.file;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dosya boş olamaz"));
        }

        try {
            String fileUrl = fileStorageService.saveFile(file);
            return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Dosya kaydedilirken hata oluştu: " + e.getMessage()));
        }
    }
}
