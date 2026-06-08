package com.tibet.tourism.modules.upload.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.stream.Stream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    @TempDir
    private Path uploadRoot;

    @Test
    void storesReencodedImageWithoutOriginalMetadata() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());
        String marker = "GPSLatitude=29.6500;DEVICE=phone;camera-note";
        byte[] jpegWithExif = withExifSegment(imageBytes("JPEG", 8, 8, false), marker);

        String storedPath = service.storeAdminImage(multipart("photo.jpg", "image/jpeg", jpegWithExif));

        assertThat(storedPath).startsWith("/uploads/admin/").endsWith(".jpg");
        byte[] storedBytes = Files.readAllBytes(resolveStoredPath(storedPath));
        String storedText = new String(storedBytes, StandardCharsets.ISO_8859_1);
        assertThat(storedText).doesNotContain(marker);
        assertThat(ImageIO.read(resolveStoredPath(storedPath).toFile())).isNotNull();
    }

    @Test
    void fallsBackWhenAtomicMoveIsUnsupported() throws Exception {
        AtomicInteger moveAttempts = new AtomicInteger();
        FileStorageService service = new FileStorageService(uploadRoot.toString(), (source, target, options) -> {
            moveAttempts.incrementAndGet();
            boolean atomicMove = Stream.of(options).anyMatch(StandardCopyOption.ATOMIC_MOVE::equals);
            if (atomicMove) {
                throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), "not supported");
            }
            Files.move(source, target, options);
        });

        String storedPath = service.storeAdminImage(
                multipart("photo.jpg", "image/jpeg", imageBytes("JPEG", 8, 8, false)));

        assertThat(moveAttempts).hasValue(2);
        assertThat(Files.exists(resolveStoredPath(storedPath))).isTrue();
        assertThat(ImageIO.read(resolveStoredPath(storedPath).toFile())).isNotNull();
    }

    @Test
    void rejectsTrailingPayloadPolyglotBeforeStorage() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());
        byte[] polyglot = appendPayload(imageBytes("JPEG", 8, 8, false), "PK\u0003\u0004evil.zip");

        assertThatThrownBy(() -> service.storeAdminImage(
                multipart("photo.jpg", "image/jpeg", polyglot)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected image data");

        assertThat(Files.exists(uploadRoot.resolve("admin"))).isFalse();
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

    @Test
    void rejectsParameterizedMimeTypeBeforeStorage() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());

        assertThatThrownBy(() -> service.storeAdminImage(
                multipart("photo.jpg", "image/jpeg; charset=utf-8", imageBytes("JPEG", 4, 4, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content type");

        assertThat(Files.exists(uploadRoot.resolve("admin"))).isFalse();
    }

    @Test
    void rejectsMagicNumberMismatchBeforeStorage() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());

        assertThatThrownBy(() -> service.storeAdminImage(
                multipart("photo.jpg", "image/jpeg", imageBytes("PNG", 4, 4, true))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature");

        assertThat(Files.exists(uploadRoot.resolve("admin"))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("disguisedNonImageUploads")
    void rejectsSvgAndHtmlDisguisedAsImages(String originalName, String contentType, String payload) {
        FileStorageService service = new FileStorageService(uploadRoot.toString());

        assertThatThrownBy(() -> service.storeAdminImage(
                multipart(originalName, contentType, payload.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(Files.exists(uploadRoot.resolve("admin"))).isFalse();
    }

    @Test
    void rejectsHtmlPayloadWithForgedJpegSignature() throws IOException {
        FileStorageService service = new FileStorageService(uploadRoot.toString());
        byte[] forgedJpeg = appendPayload(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
                "<html><script>alert(1)</script></html>");

        assertThatThrownBy(() -> service.storeAdminImage(
                multipart("photo.jpg", "image/jpeg", forgedJpeg)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(Files.exists(uploadRoot.resolve("admin"))).isFalse();
    }

    @Test
    void rejectsActiveMarkupEmbeddedInImageMetadata() throws IOException {
        FileStorageService service = new FileStorageService(uploadRoot.toString());
        byte[] jpegWithScriptMetadata = withExifSegment(
                imageBytes("JPEG", 8, 8, false),
                "<svg><script>alert(1)</script></svg>");

        assertThatThrownBy(() -> service.storeAdminImage(
                multipart("photo.jpg", "image/jpeg", jpegWithScriptMetadata)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active content");

        assertThat(Files.exists(uploadRoot.resolve("admin"))).isFalse();
    }

    @Test
    void rejectsCompressedImageHeaderAbovePixelPolicyWithoutAllocatingImage() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());

        assertThatThrownBy(() -> service.storeCommentImage(
                multipart("large.png", "image/png", pngWithDimensions(5000, 5000))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions");

        assertThat(Files.exists(uploadRoot.resolve("comments"))).isFalse();
    }

    @Test
    void rejectsPathTraversalOriginalFilenameBeforeStorage() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());

        assertThatThrownBy(() -> service.storeAdminImage(
                multipart("..\\..//escape.jpg", "image/jpeg", imageBytes("JPEG", 4, 4, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsafe image filename");

        assertThat(Files.exists(uploadRoot.resolve("admin"))).isFalse();
        assertThat(Files.exists(uploadRoot.resolve("escape.jpg"))).isFalse();
    }

    @Test
    void rejectsDangerousDoubleExtensionBeforeStorage() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());

        assertThatThrownBy(() -> service.storeAdminImage(
                multipart("payload.svg.jpg", "image/jpeg", imageBytes("JPEG", 4, 4, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsafe image filename");

        assertThat(Files.exists(uploadRoot.resolve("admin"))).isFalse();
    }

    @Test
    void cleansTemporaryFileWhenSanitizedOutputExceedsLimit() throws Exception {
        FileStorageService service = new FileStorageService(uploadRoot.toString());
        byte[] expensiveJpeg = noisyJpegBytes(2048, 2048, 0.25f);

        assertThat(expensiveJpeg.length).isLessThan(2 * 1024 * 1024);
        assertThatThrownBy(() -> service.storeAvatar(
                multipart("avatar.jpg", "image/jpeg", expensiveJpeg)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sanitized image exceeds size limit");

        Path avatarDir = uploadRoot.resolve("avatars");
        assertThat(avatarDir).isDirectory();
        try (Stream<Path> files = Files.list(avatarDir)) {
            assertThat(files).isEmpty();
        }
    }

    private static Stream<Arguments> disguisedNonImageUploads() {
        return Stream.of(
                Arguments.of("avatar.svg", "image/svg+xml", "<svg><script>alert(1)</script></svg>"),
                Arguments.of("avatar.jpg", "image/jpeg", "<svg><script>alert(1)</script></svg>"),
                Arguments.of("avatar.png", "image/png", "<!doctype html><script>alert(1)</script>")
        );
    }

    private MockMultipartFile multipart(String originalName, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", originalName, contentType, bytes);
    }

    private MockMultipartFile multipartWithReportedSize(
            String originalName, String contentType, byte[] bytes, long reportedSize) {
        return new MockMultipartFile("file", originalName, contentType, bytes) {
            @Override
            public long getSize() {
                return reportedSize;
            }
        };
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

    private byte[] noisyJpegBytes(int width, int height, float quality) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, random.nextInt(0x1000000));
            }
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("JPEG").next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    private byte[] pngWithDimensions(int width, int height) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        writeInt(ihdr, width);
        writeInt(ihdr, height);
        ihdr.write(8);
        ihdr.write(6);
        ihdr.write(0);
        ihdr.write(0);
        ihdr.write(0);
        writePngChunk(output, "IHDR", ihdr.toByteArray());
        writePngChunk(output, "IEND", new byte[0]);
        return output.toByteArray();
    }

    private void writePngChunk(ByteArrayOutputStream output, String type, byte[] data) throws IOException {
        writeInt(output, data.length);
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        output.write(typeBytes);
        output.write(data);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        writeInt(output, (int) crc.getValue());
    }

    private void writeInt(ByteArrayOutputStream output, int value) {
        output.write((value >>> 24) & 0xFF);
        output.write((value >>> 16) & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }
}
