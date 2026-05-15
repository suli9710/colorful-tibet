package com.tibet.tourism.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;
    private static final java.util.Set<String> ALLOWED_IMAGE_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeCommentImage(MultipartFile file) throws IOException {
        return storeImage(file, "comments");
    }

    public String storeAvatar(MultipartFile file) throws IOException {
        return storeImage(file, "avatars");
    }

    public String storeAdminImage(MultipartFile file) throws IOException {
        return storeImage(file, "admin");
    }

    private String storeImage(MultipartFile file, String folder) throws IOException {
        validateImage(file);
        String extension = getFileExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;

        Path targetDir = uploadRoot.resolve(folder).normalize();
        Files.createDirectories(targetDir);

        Path targetLocation = targetDir.resolve(filename).normalize();
        if (!targetLocation.startsWith(targetDir) || !targetLocation.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + folder + "/" + filename;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("仅支持图片文件上传");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 JPG、PNG 或 WebP 图片");
        }

        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new IllegalArgumentException("图片大小不能超过5MB");
        }

        try {
            if (!hasValidImageSignature(file, extension)) {
                throw new IllegalArgumentException("图片内容与文件类型不匹配");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("无法读取上传文件");
        }
    }

    private String getFileExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }
        return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private boolean hasValidImageSignature(MultipartFile file, String extension) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        }
        if (read <= 0) {
            return false;
        }

        return switch (extension) {
            case ".jpg", ".jpeg" -> read >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            case ".png" -> read >= 8
                    && Arrays.equals(Arrays.copyOf(header, 8),
                    new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case ".webp" -> read >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';
            default -> false;
        };
    }
}
















