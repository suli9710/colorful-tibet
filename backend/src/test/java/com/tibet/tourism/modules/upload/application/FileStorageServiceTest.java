package com.tibet.tourism.modules.upload.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    @TempDir
    private Path uploadRoot;

    @Test
    void storesReencodedImageWithoutOriginalMetadataOrTrailingPayload() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());
        String marker = "GPSLatitude=29.6500;DEVICE=phone;<?php echo 'x'; ?>";
        byte[] jpegWithExif = withExifSegment(imageBytes("JPEG", 8, 8, false), marker);
        byte[] polyglot = appendPayload(jpegWithExif, "PK\u0003\u0004evil.zip");

        String storedPath = service.storeAdminImage(multipart("photo.jpg", "image/jpeg", polyglot));

        assertThat(storedPath).startsWith("/uploads/admin/").endsWith(".jpg");
        byte[] storedBytes = Files.readAllBytes(resolveStoredPath(storedPath));
        String storedText = new String(storedBytes, StandardCharsets.ISO_8859_1);
        assertThat(storedText).doesNotContain(marker);
        assertThat(storedText).doesNotContain("evil.zip");
        assertThat(ImageIO.read(resolveStoredPath(storedPath).toFile())).isNotNull();
    }

    @Test
    void rejectsGeneralImagesAboveDimensionPolicy() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());
        byte[] oversizedWidthPng = imageBytes("PNG", 8001, 1, true);

        assertThatThrownBy(() -> service.storeCommentImage(
                multipart("wide.png", "image/png", oversizedWidthPng)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions");

        assertThat(Files.exists(uploadRoot.resolve("comments"))).isFalse();
    }

    @Test
    void acceptsWebpInputAndStoresCanonicalImageOutput() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());
        byte[] webp = Base64.getDecoder().decode("UklGRh4AAABXRUJQVlA4TBEAAAAvAUAAAAdQvFIUuf+BiOh/AAA=");

        String storedPath = service.storeAdminImage(multipart("photo.webp", "image/webp", webp));

        assertThat(storedPath).startsWith("/uploads/admin/");
        assertThat(storedPath).doesNotEndWith(".webp");
        assertThat(ImageIO.read(resolveStoredPath(storedPath).toFile())).isNotNull();
    }

    @Test
    void appliesSmallerAvatarByteAndDimensionPolicy() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());

        assertThatThrownBy(() -> service.storeAvatar(
                multipart("large.png", "image/png", oversizedPngPayload())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");

        assertThatThrownBy(() -> service.storeAvatar(
                multipart("wide.png", "image/png", imageBytes("PNG", 2049, 1, true))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions");
    }

    @Test
    void rejectsMismatchedExtensionAndContentTypeBeforeStorage() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());

        assertThatThrownBy(() -> service.storeAdminImage(
                multipart("photo.png", "image/jpeg", imageBytes("PNG", 4, 4, true))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content type");
    }

    private MockMultipartFile multipart(String originalName, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", originalName, contentType, bytes);
    }

    private Path resolveStoredPath(String storedPath) {
        String relativePath = storedPath.replaceFirst("^/uploads/", "").replace("/", java.io.File.separator);
        return uploadRoot.resolve(relativePath);
    }

    private byte[] imageBytes(String format, int width, int height, boolean alpha) throws IOException {
        BufferedImage image = new BufferedImage(
                width,
                height,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(alpha ? new Color(20, 120, 200, 180) : Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private byte[] withExifSegment(byte[] jpegBytes, String metadata) throws IOException {
        byte[] payload = ("Exif\0\0" + metadata).getBytes(StandardCharsets.ISO_8859_1);
        int length = payload.length + 2;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(jpegBytes, 0, 2);
        output.write(0xFF);
        output.write(0xE1);
        output.write((length >>> 8) & 0xFF);
        output.write(length & 0xFF);
        output.write(payload);
        output.write(jpegBytes, 2, jpegBytes.length - 2);
        return output.toByteArray();
    }

    private byte[] appendPayload(byte[] imageBytes, String payload) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(imageBytes);
        output.write(payload.getBytes(StandardCharsets.ISO_8859_1));
        return output.toByteArray();
    }

    private byte[] oversizedPngPayload() throws IOException {
        byte[] png = imageBytes("PNG", 4, 4, true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(png);
        output.write(new byte[(2 * 1024 * 1024) + 1]);
        return output.toByteArray();
    }
}
