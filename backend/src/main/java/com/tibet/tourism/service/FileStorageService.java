package com.tibet.tourism.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeCommentImage(MultipartFile file) throws IOException {
        validateImage(file);
        String extension = getFileExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;

        Path commentDir = uploadRoot.resolve("comments");
        Files.createDirectories(commentDir);

        Path targetLocation = commentDir.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/comments/" + filename;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片文件上传");
        }

        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new IllegalArgumentException("图片大小不能超过5MB");
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
}
















