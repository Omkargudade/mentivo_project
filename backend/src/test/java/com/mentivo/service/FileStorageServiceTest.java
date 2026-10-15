package com.mentivo.service;

import com.mentivo.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("storeProfilePicture stores valid JPEG file and returns public URL path")
    void storeProfilePicture_Jpeg_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0}
        );

        String relativePath = fileStorageService.storeProfilePicture(file);

        assertNotNull(relativePath);
        assertTrue(relativePath.startsWith("/uploads/profile-pictures/"));
        assertTrue(relativePath.endsWith(".jpg"));

        // Verify file was actually created on disk
        String filename = relativePath.substring("/uploads/profile-pictures/".length());
        File storedFile = tempDir.resolve("profile-pictures").resolve(filename).toFile();
        assertTrue(storedFile.exists());
        assertTrue(storedFile.length() > 0);
    }

    @Test
    @DisplayName("storeProfilePicture stores valid PNG file and WebP file")
    void storeProfilePicture_PngAndWebp_Success() {
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        String pngPath = fileStorageService.storeProfilePicture(pngFile);
        assertNotNull(pngPath);
        assertTrue(pngPath.endsWith(".png"));

        MockMultipartFile webpFile = new MockMultipartFile(
                "file",
                "profile.webp",
                "image/webp",
                new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}
        );

        String webpPath = fileStorageService.storeProfilePicture(webpFile);
        assertNotNull(webpPath);
        assertTrue(webpPath.endsWith(".webp"));
    }

    @Test
    @DisplayName("storeProfilePicture throws exception for empty file")
    void storeProfilePicture_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileStorageService.storeProfilePicture(emptyFile)
        );
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("storeProfilePicture throws exception when file exceeds 5MB")
    void storeProfilePicture_ExceedsSize_ThrowsException() {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeBytes
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileStorageService.storeProfilePicture(largeFile)
        );
        assertTrue(ex.getMessage().contains("5 MB"));
    }

    @Test
    @DisplayName("storeProfilePicture throws exception for unsupported MIME content type")
    void storeProfilePicture_UnsupportedMime_ThrowsException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileStorageService.storeProfilePicture(pdfFile)
        );
        assertTrue(ex.getMessage().contains("content type"));
    }

    @Test
    @DisplayName("storeProfilePicture throws exception for mismatched/unsupported file extension")
    void storeProfilePicture_UnsupportedExtension_ThrowsException() {
        MockMultipartFile invalidExt = new MockMultipartFile(
                "file",
                "malicious.exe",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                fileStorageService.storeProfilePicture(invalidExt)
        );
        assertTrue(ex.getMessage().contains("extension"));
    }

    @Test
    @DisplayName("deleteFile successfully deletes file from disk")
    void deleteFile_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "to-delete.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0}
        );

        String relativePath = fileStorageService.storeProfilePicture(file);
        String filename = relativePath.substring("/uploads/profile-pictures/".length());
        File storedFile = tempDir.resolve("profile-pictures").resolve(filename).toFile();
        assertTrue(storedFile.exists());

        fileStorageService.deleteFile(relativePath);
        assertFalse(storedFile.exists());
    }

    @Test
    @DisplayName("deleteFile safely ignores external URLs and null/empty paths")
    void deleteFile_ExternalOrNull_SafeIgnore() {
        assertDoesNotThrow(() -> fileStorageService.deleteFile(null));
        assertDoesNotThrow(() -> fileStorageService.deleteFile(""));
        assertDoesNotThrow(() -> fileStorageService.deleteFile("https://example.com/avatar.jpg"));
        assertDoesNotThrow(() -> fileStorageService.deleteFile("https://api.dicebear.com/7.x/avataaars/svg"));
    }

    @Test
    @DisplayName("deleteFile guards against path traversal attempts")
    void deleteFile_PathTraversal_Guarded() {
        assertDoesNotThrow(() -> fileStorageService.deleteFile("/uploads/profile-pictures/../../secret.txt"));
    }
}
