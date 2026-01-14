package com.empresa.comissao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

/**
 * Local Storage Service for development environment.
 * Stores files in local filesystem.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "aws.s3.bucket", matchIfMissing = true, havingValue = "false")
public class LocalStorageService implements StorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp");
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

    @Value("${app.upload.dir:uploads/logos}")
    private String uploadDir;

    @Override
    public String uploadFile(MultipartFile file, String key) throws IOException {
        validateFile(file);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(key);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("File uploaded locally: {}", filePath);

        return key;
    }

    @Override
    public String getFileUrl(String key) {
        return "/api/v1/empresa/logo/" + key;
    }

    @Override
    public void deleteFile(String key) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(key);
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean fileExists(String key) {
        Path filePath = Paths.get(uploadDir).resolve(key);
        return Files.exists(filePath);
    }

    @Override
    public byte[] getFileBytes(String key) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(key);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
            return null;
        } catch (IOException e) {
            log.warn("Failed to read file {}: {}", key, e.getMessage());
            return null;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio não é permitido.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo permitido: 2MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido. Use PNG, JPEG ou WebP.");
        }
    }
}
