package com.beem.TastyMap.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String baseUploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    // Tekli Dosya Kaydetme
    public String saveFile(MultipartFile file, String folderName) throws IOException {
        Path uploadPath = Paths.get(baseUploadDir, folderName);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        String newFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(newFileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // URL Temizleme (Çift '/' karakterlerini önleme)
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanUploadDir = baseUploadDir.replaceAll("^/|/$", "");

        return cleanBaseUrl + "/" + cleanUploadDir + "/" + folderName + "/" + newFileName;
    }

    // YENİ: Çoklu Dosya Kaydetme
    public List<String> saveFiles(List<MultipartFile> files, String folderName) throws IOException {
        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                fileUrls.add(saveFile(file, folderName));
            }
        }
        return fileUrls;
    }

    // URL'den sunucudaki dosyayı silme
    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            String relativePathStr = fileUrl.replace(cleanBaseUrl, "");

            Path filePath = Paths.get(relativePathStr);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Dosya silinirken hata oluştu: " + e.getMessage());
        }
    }

    // Çoklu dosyaları silme
    public void deleteFilesByUrls(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) return;

        for (String url : fileUrls) {
            deleteFileByUrl(url);
        }
    }
}