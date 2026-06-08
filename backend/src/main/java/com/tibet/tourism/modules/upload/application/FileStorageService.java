package com.tibet.tourism.modules.upload.application;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final int GENERAL_MAX_BYTES = 5 * 1024 * 1024;
    private static final int AVATAR_MAX_BYTES = 2 * 1024 * 1024;
    private static final UploadPolicy GENERAL_POLICY = new UploadPolicy(
            GENERAL_MAX_BYTES, 8000, 8000, 20_000_000L);
    private static final UploadPolicy AVATAR_POLICY = new UploadPolicy(
            AVATAR_MAX_BYTES, 2048, 2048, 2048L * 2048L);
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );
    private static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;
    private static final Set<String> DANGEROUS_EMBEDDED_EXTENSIONS = Set.of(
            ".svg", ".html", ".htm", ".xhtml", ".xml", ".php", ".phtml", ".jsp", ".jspx",
            ".asp", ".aspx", ".js", ".mjs", ".css", ".exe", ".dll", ".bat", ".cmd",
            ".sh", ".ps1", ".jar", ".war", ".zip", ".rar", ".7z", ".pdf"
    );
    private static final List<byte[]> ACTIVE_CONTENT_MARKERS = List.of(
            ascii("<!doctype html"),
            ascii("<html"),
            ascii("<script"),
            ascii("<svg"),
            ascii("<iframe"),
            ascii("<?xml"),
            ascii("<?php"),
            ascii("javascript:"),
            ascii("data:text/html")
    );

    private final Path uploadRoot;
    private final FileMover fileMover;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this(uploadDir, (source, target, options) -> Files.move(source, target, options));
    }

    FileStorageService(String uploadDir, FileMover fileMover) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileMover = fileMover;
    }

    public String storeCommentImage(MultipartFile file) throws IOException {
        return storeImage(file, "comments", GENERAL_POLICY);
    }

    public String storeAvatar(MultipartFile file) throws IOException {
        return storeImage(file, "avatars", AVATAR_POLICY);
    }

    public String storeAdminImage(MultipartFile file) throws IOException {
        return storeImage(file, "admin", GENERAL_POLICY);
    }

    private String storeImage(MultipartFile file, String folder, UploadPolicy policy) throws IOException {
        UploadCandidate candidate = validateDeclaredImage(file, policy);
        BufferedImage image = decodeAndValidateImage(candidate.bytes(), candidate.imageType(), policy);
        ImageType inputType = candidate.imageType();
        OutputImageType outputType = outputTypeFor(image, inputType);
        String filename = UUID.randomUUID() + outputType.extension();

        Path targetDir = uploadRoot.resolve(folder).normalize();
        Files.createDirectories(targetDir);

        Path targetLocation = targetDir.resolve(filename).normalize();
        if (!targetLocation.startsWith(targetDir) || !targetLocation.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Path tempLocation = Files.createTempFile(targetDir, "upload-", ".tmp");
        try {
            writeSanitizedImage(image, outputType, tempLocation);
            if (Files.size(tempLocation) > policy.maxBytes()) {
                throw new IllegalArgumentException("Sanitized image exceeds size limit");
            }
            moveIntoPlace(tempLocation, targetLocation);
        } finally {
            Files.deleteIfExists(tempLocation);
        }

        return "/uploads/" + folder + "/" + filename;
    }

    private void moveIntoPlace(Path tempLocation, Path targetLocation) throws IOException {
        try {
            fileMover.move(tempLocation, targetLocation, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            fileMover.move(tempLocation, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private UploadCandidate validateDeclaredImage(MultipartFile file, UploadPolicy policy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (file.getSize() > policy.maxBytes()) {
            throw new IllegalArgumentException("Image size exceeds limit");
        }

        String originalFilename = validateOriginalFilename(file.getOriginalFilename());
        String normalizedContentType = normalizeContentType(file.getContentType());
        if (normalizedContentType == null || !ALLOWED_IMAGE_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException("Unsupported image content type");
        }

        String extension = getFileExtension(originalFilename);
        ImageType inputType = ImageType.fromExtension(extension);
        if (inputType == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image extension");
        }
        if (!inputType.contentType().equals(normalizedContentType)) {
            throw new IllegalArgumentException("Image extension and content type do not match");
        }

        byte[] bytes = readUploadBytes(file, policy);
        if (!hasValidImageSignature(bytes, inputType)) {
            throw new IllegalArgumentException("Image signature does not match extension");
        }
        validateImageContainer(bytes, inputType);
        rejectActiveContentMarkers(bytes);

        return new UploadCandidate(inputType, bytes);
    }

    private BufferedImage decodeAndValidateImage(byte[] bytes, ImageType inputType, UploadPolicy policy) {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (imageInput == null) {
                throw new IllegalArgumentException("Invalid image");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Invalid image");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                if (!inputType.matchesReaderFormat(reader.getFormatName())) {
                    throw new IllegalArgumentException("Image content does not match extension");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height, policy);

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("Invalid image");
                }
                validateDimensions(image.getWidth(), image.getHeight(), policy);
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException | IndexOutOfBoundsException exception) {
            throw new IllegalArgumentException("Invalid image");
        }
    }

    private byte[] readUploadBytes(MultipartFile file, UploadPolicy policy) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] bytes = inputStream.readNBytes(policy.maxBytes() + 1);
            if (bytes.length == 0) {
                throw new IllegalArgumentException("File must not be empty");
            }
            if (bytes.length > policy.maxBytes()) {
                throw new IllegalArgumentException("Image size exceeds limit");
            }
            return bytes;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read upload file", exception);
        }
    }

    private String validateOriginalFilename(String originalFilename) {
        if (originalFilename == null) {
            throw new IllegalArgumentException("Image filename is required");
        }

        String normalized = Normalizer.normalize(originalFilename, Normalizer.Form.NFKC).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Image filename is required");
        }
        if (normalized.length() > MAX_ORIGINAL_FILENAME_LENGTH) {
            throw new IllegalArgumentException("Image filename is too long");
        }
        if (containsUnsafeFilenameCharacter(normalized) || containsTraversal(normalized)) {
            throw new IllegalArgumentException("Unsafe image filename");
        }

        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == normalized.length() - 1) {
            throw new IllegalArgumentException("Unsupported image extension");
        }
        rejectDangerousEmbeddedExtension(normalized.substring(0, dotIndex));
        return normalized;
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

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String value = contentType.trim().toLowerCase(Locale.ROOT);
        if (value.indexOf(';') >= 0 || containsAsciiControl(value)) {
            return value;
        }
        return value;
    }

    private boolean hasValidImageSignature(byte[] bytes, ImageType inputType) {
        if (bytes.length == 0) {
            return false;
        }

        return switch (inputType) {
            case JPEG -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case PNG -> bytes.length >= 8
                    && Arrays.equals(Arrays.copyOf(bytes, 8),
                    new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case WEBP -> bytes.length >= 12
                    && bytes[0] == 'R'
                    && bytes[1] == 'I'
                    && bytes[2] == 'F'
                    && bytes[3] == 'F'
                    && bytes[8] == 'W'
                    && bytes[9] == 'E'
                    && bytes[10] == 'B'
                    && bytes[11] == 'P';
        };
    }

    private void validateImageContainer(byte[] bytes, ImageType inputType) {
        switch (inputType) {
            case JPEG -> validateJpegContainer(bytes);
            case PNG -> validatePngContainer(bytes);
            case WEBP -> validateWebpContainer(bytes);
        }
    }

    private void validateJpegContainer(byte[] bytes) {
        int end = lastIndexOf(bytes, new byte[]{(byte) 0xFF, (byte) 0xD9});
        if (end < 0) {
            throw new IllegalArgumentException("Invalid JPEG structure");
        }
        if (end != bytes.length - 2) {
            throw new IllegalArgumentException("Unexpected image data after image end");
        }
    }

    private void validatePngContainer(byte[] bytes) {
        int offset = 8;
        boolean foundEnd = false;
        while (offset < bytes.length) {
            if (offset + 12 > bytes.length) {
                throw new IllegalArgumentException("Invalid PNG structure");
            }

            long length = readUnsignedIntBigEndian(bytes, offset);
            long dataStart = offset + 8L;
            long nextOffset = dataStart + length + 4L;
            if (length > Integer.MAX_VALUE || nextOffset > bytes.length) {
                throw new IllegalArgumentException("Invalid PNG structure");
            }

            String chunkType = new String(bytes, offset + 4, 4, StandardCharsets.US_ASCII);
            offset = (int) nextOffset;
            if ("IEND".equals(chunkType)) {
                foundEnd = true;
                break;
            }
        }

        if (!foundEnd) {
            throw new IllegalArgumentException("Invalid PNG structure");
        }
        if (offset != bytes.length) {
            throw new IllegalArgumentException("Unexpected image data after image end");
        }
    }

    private void validateWebpContainer(byte[] bytes) {
        long riffPayloadSize = readUnsignedIntLittleEndian(bytes, 4);
        if (riffPayloadSize + 8L != bytes.length) {
            throw new IllegalArgumentException("Unexpected image data after image end");
        }
    }

    private void rejectActiveContentMarkers(byte[] bytes) {
        for (byte[] marker : ACTIVE_CONTENT_MARKERS) {
            if (containsAsciiCaseInsensitive(bytes, marker)) {
                throw new IllegalArgumentException("Active content is not allowed in image uploads");
            }
        }
    }

    private void validateDimensions(int width, int height, UploadPolicy policy) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid image dimensions");
        }
        long pixels = (long) width * height;
        if (width > policy.maxWidth()
                || height > policy.maxHeight()
                || pixels > policy.maxPixels()) {
            throw new IllegalArgumentException("Image dimensions too large");
        }
    }

    private OutputImageType outputTypeFor(BufferedImage image, ImageType inputType) {
        if (inputType == ImageType.PNG || image.getColorModel().hasAlpha()) {
            return OutputImageType.PNG;
        }
        return OutputImageType.JPEG;
    }

    private void writeSanitizedImage(BufferedImage source, OutputImageType outputType, Path target) throws IOException {
        BufferedImage output = prepareForOutput(source, outputType);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(outputType.formatName());
        if (!writers.hasNext()) {
            throw new IOException("No ImageIO writer for " + outputType.formatName());
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(target.toFile())) {
            if (outputStream == null) {
                throw new IOException("Unable to create image output stream");
            }
            writer.setOutput(outputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (outputType == OutputImageType.JPEG && writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(0.9f);
            }
            writer.write(null, new IIOImage(output, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage prepareForOutput(BufferedImage source, OutputImageType outputType) {
        int imageType = outputType == OutputImageType.JPEG
                ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        if (source.getType() == imageType) {
            return source;
        }

        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), imageType);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if (outputType == OutputImageType.JPEG) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
            }
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private boolean containsUnsafeFilenameCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current <= 31 || current == 127
                    || current == '/' || current == '\\' || current == ':'
                    || current == ';' || current == '?' || current == '#'
                    || current == '%' || current == '\u0000') {
                return true;
            }
        }
        return false;
    }

    private boolean containsAsciiControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current <= 31 || current == 127) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTraversal(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("..")
                || lower.contains("%2e")
                || lower.contains("%2f")
                || lower.contains("%5c");
    }

    private void rejectDangerousEmbeddedExtension(String filenameStem) {
        String lowerStem = filenameStem.toLowerCase(Locale.ROOT);
        for (String extension : DANGEROUS_EMBEDDED_EXTENSIONS) {
            if (lowerStem.endsWith(extension) || lowerStem.contains(extension + ".")) {
                throw new IllegalArgumentException("Unsafe image filename");
            }
        }
    }

    private int lastIndexOf(byte[] value, byte[] pattern) {
        for (int i = value.length - pattern.length; i >= 0; i--) {
            boolean matched = true;
            for (int j = 0; j < pattern.length; j++) {
                if (value[i + j] != pattern[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return i;
            }
        }
        return -1;
    }

    private long readUnsignedIntBigEndian(byte[] bytes, int offset) {
        return ((long) bytes[offset] & 0xFF) << 24
                | ((long) bytes[offset + 1] & 0xFF) << 16
                | ((long) bytes[offset + 2] & 0xFF) << 8
                | ((long) bytes[offset + 3] & 0xFF);
    }

    private long readUnsignedIntLittleEndian(byte[] bytes, int offset) {
        return ((long) bytes[offset] & 0xFF)
                | (((long) bytes[offset + 1] & 0xFF) << 8)
                | (((long) bytes[offset + 2] & 0xFF) << 16)
                | (((long) bytes[offset + 3] & 0xFF) << 24);
    }

    private boolean containsAsciiCaseInsensitive(byte[] value, byte[] marker) {
        if (marker.length == 0 || value.length < marker.length) {
            return false;
        }

        for (int i = 0; i <= value.length - marker.length; i++) {
            boolean matched = true;
            for (int j = 0; j < marker.length; j++) {
                if (toAsciiLower(value[i + j]) != marker[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private byte toAsciiLower(byte value) {
        if (value >= 'A' && value <= 'Z') {
            return (byte) (value + ('a' - 'A'));
        }
        return value;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private enum ImageType {
        JPEG("image/jpeg", "JPEG", ".jpg", ".jpeg"),
        PNG("image/png", "PNG", ".png"),
        WEBP("image/webp", "WebP", ".webp");

        private final String contentType;
        private final String readerFormatName;
        private final Set<String> extensions;

        ImageType(String contentType, String readerFormatName, String... extensions) {
            this.contentType = contentType;
            this.readerFormatName = readerFormatName;
            this.extensions = Set.of(extensions);
        }

        static ImageType fromExtension(String extension) {
            for (ImageType imageType : values()) {
                if (imageType.extensions.contains(extension)) {
                    return imageType;
                }
            }
            return null;
        }

        String contentType() {
            return contentType;
        }

        boolean matchesReaderFormat(String formatName) {
            return readerFormatName.equalsIgnoreCase(formatName);
        }
    }

    private enum OutputImageType {
        JPEG("JPEG", ".jpg"),
        PNG("PNG", ".png");

        private final String formatName;
        private final String extension;

        OutputImageType(String formatName, String extension) {
            this.formatName = formatName;
            this.extension = extension;
        }

        String formatName() {
            return formatName;
        }

        String extension() {
            return extension;
        }
    }

    private record UploadPolicy(int maxBytes, int maxWidth, int maxHeight, long maxPixels) {
    }

    private record UploadCandidate(ImageType imageType, byte[] bytes) {
    }

    @FunctionalInterface
    interface FileMover {
        void move(Path source, Path target, StandardCopyOption... options) throws IOException;
    }
}
