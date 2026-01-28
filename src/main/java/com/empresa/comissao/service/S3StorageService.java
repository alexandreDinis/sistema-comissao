package com.empresa.comissao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Set;

/**
 * S3 Storage Service for file operations.
 * Handles upload, download URL generation, and deletion.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "aws.s3.bucket", matchIfMissing = false)
public class S3StorageService implements StorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp");
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

    private final S3Client s3Client;

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    @Value("${aws.s3.public-url:#{null}}")
    private String publicUrl;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Upload file to S3 bucket.
     *
     * @param file The multipart file to upload
     * @param key  The S3 object key (path/filename)
     * @return The public URL or key of the uploaded file
     */
    @Override
    public String uploadFile(MultipartFile file, String key) throws IOException {
        validateFile(file);

        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
        log.info("File uploaded successfully: {}", key);

        return getFileUrl(key);
    }

    /**
     * Get public URL for a file.
     * Uses configured public URL if available (for CDN/custom domains).
     */
    @Override
    public String getFileUrl(String key) {
        if (publicUrl != null && !publicUrl.isEmpty()) {
            return publicUrl + "/" + key;
        }
        // Default: return just the key for internal resolution
        return key;
    }

    /**
     * Delete file from S3 bucket.
     */
    @Override
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("File deleted successfully: {}", key);
        } catch (S3Exception e) {
            log.warn("Failed to delete file {}: {}", key, e.getMessage());
        }
    }

    /**
     * Check if file exists in S3.
     */
    @Override
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * Get file bytes from S3 (for proxying).
     */
    @Override
    public byte[] getFileBytes(String key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(getRequest).asByteArray();
        } catch (NoSuchKeyException e) {
            log.warn("File not found: {}", key);
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
