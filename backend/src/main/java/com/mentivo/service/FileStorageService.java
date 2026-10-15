package com.mentivo.service;

import com.mentivo.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.upload.dir:uploads}")
    private String baseUploadDir;

    @Value("${app.upload.max-file-size:5242880}") // 5MB default
    private long maxFileSize;

    private Path profilePicturesDir;

    public FileStorageService() {
        this.baseUploadDir = "uploads";
        this.maxFileSize = 5242880L;
    }

    public FileStorageService(String baseUploadDir) {
        this.baseUploadDir = baseUploadDir;
        this.maxFileSize = 5242880L;
        init();
    }

    @PostConstruct
    public void init() {
        try {
            Path base = Paths.get(baseUploadDir).toAbsolutePath().normalize();
            this.profilePicturesDir = base.resolve("profile-pictures").normalize();
            Files.createDirectories(profilePicturesDir);
            log.info("Initialized secure profile picture storage at: {}", profilePicturesDir);
        } catch (IOException e) {
            log.error("Failed to initialize profile pictures directory: {}", e.getMessage());
            throw new RuntimeException("Could not initialize upload storage", e);
        }
    }

    /**
     * Stores an uploaded profile picture safely with magic bytes inspection and UUID naming.
     * Returns the relative public URL path: /uploads/profile-pictures/{filename}
     */
    public String storeProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String ct = contentType.toLowerCase();
            if (!ct.equals("image/jpeg") && !ct.equals("image/jpg") && !ct.equals("image/png") && !ct.equals("image/webp")) {
                throw new BadRequestException("Unsupported content type: " + contentType + ". Only JPEG, PNG, or WebP images are allowed.");
            }
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".webp")) {
                throw new BadRequestException("Unsupported file extension: " + ext + ". Only .jpg, .jpeg, .png, or .webp files are allowed.");
            }
        }

        String extension = detectImageExtensionFromMagicBytes(file);

        // Generate server-controlled random filename
        String safeFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path destination = profilePicturesDir.resolve(safeFileName).normalize();

        // Enforce path traversal protection
        if (!destination.startsWith(profilePicturesDir)) {
            throw new BadRequestException("Invalid storage destination");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored profile picture {} ({} bytes)", safeFileName, file.getSize());
            return "/uploads/profile-pictures/" + safeFileName;
        } catch (IOException e) {
            log.error("Failed to store profile picture: {}", e.getMessage());
            throw new RuntimeException("Failed to store uploaded file", e);
        }
    }

    /**
     * Safely deletes an existing uploaded profile picture if stored on local storage.
     */
    public void deleteProfilePicture(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String fileName = null;
        if (imageUrl.startsWith("/uploads/profile-pictures/")) {
            fileName = imageUrl.substring("/uploads/profile-pictures/".length());
        } else if (imageUrl.startsWith("/api/uploads/profile-pictures/")) {
            fileName = imageUrl.substring("/api/uploads/profile-pictures/".length());
        } else {
            return; // Not a local upload or null
        }

        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            log.warn("Potential path traversal attempt in file deletion: {}", fileName);
            return;
        }

        Path target = profilePicturesDir.resolve(fileName).normalize();
        if (target.startsWith(profilePicturesDir) && Files.exists(target)) {
            try {
                Files.delete(target);
                log.info("Removed old profile picture file: {}", fileName);
            } catch (IOException e) {
                log.warn("Could not delete old profile picture file {}: {}", fileName, e.getMessage());
            }
        }
    }

    /**
     * Alias for deleteProfilePicture for generic callers.
     */
    public void deleteFile(String imageUrl) {
        deleteProfilePicture(imageUrl);
    }

    /**
     * Inspects magic bytes (file signature) to detect actual image content.
     * Strictly allows only JPEG, PNG, or WebP. Rejects SVG, executables, HTML, scripts.
     */
    private String detectImageExtensionFromMagicBytes(MultipartFile file) {
        byte[] header = new byte[12];
        int bytesRead;
        try (InputStream in = file.getInputStream()) {
            bytesRead = in.read(header);
            if (bytesRead < 3) {
                throw new BadRequestException("Uploaded file is too small or corrupt");
            }
        } catch (IOException e) {
            throw new BadRequestException("Unable to inspect uploaded file content");
        }

        // JPEG: FF D8 FF
        if (bytesRead >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytesRead >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47 &&
                header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return ".png";
        }

        // WebP: RIFF (bytes 0..3) ... WEBP (bytes 8..11)
        if (bytesRead >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
                header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return ".webp";
        }

        throw new BadRequestException("Unsupported image format. Only real JPEG, PNG, or WebP images are allowed. SVG and executable files are rejected.");
    }
}
