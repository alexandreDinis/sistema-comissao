package com.empresa.comissao.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Storage Service Interface for file operations.
 * Allows switching between S3 (prod) and local filesystem (dev).
 */
public interface StorageService {

    /**
     * Upload file to storage.
     *
     * @param file The multipart file to upload
     * @param key  The storage key (path/filename)
     * @return The URL or key of the uploaded file
     */
    String uploadFile(MultipartFile file, String key) throws IOException;

    /**
     * Get public URL for a file.
     */
    String getFileUrl(String key);

    /**
     * Delete file from storage.
     */
    void deleteFile(String key);

    /**
     * Check if file exists.
     */
    boolean fileExists(String key);

    /**
     * Get file bytes (for proxying).
     */
    byte[] getFileBytes(String key);
}
