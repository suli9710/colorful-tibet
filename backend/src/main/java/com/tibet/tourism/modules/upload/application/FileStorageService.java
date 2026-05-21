package com.tibet.tourism.modules.upload.application;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
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

    private final Path uploadRoot;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
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
        ImageType inputType = validateDeclaredImage(file, policy);
        BufferedImage image = decodeAndValidateImage(file, inputType, policy);
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
            Files.move(tempLocation, targetLocation);
        } finally {
            Files.deleteIfExists(tempLocation);
        }

        return "/uploads/" + folder + "/" + filename;
    }

    private ImageType validateDeclaredImage(MultipartFile file, UploadPolicy policy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (file.getSize() > policy.maxBytes()) {
            throw new IllegalArgumentException("Image size exceeds limit");
        }

        String normalizedContentType = normalizeContentType(file.getContentType());
        if (normalizedContentType == null || !ALLOWED_IMAGE_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException("Unsupported image content type");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        ImageType inputType = ImageType.fromExtension(extension);
        if (inputType == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image extension");
        }
        if (!inputType.contentType().equals(normalizedContentType)) {
            throw new IllegalArgumentException("Image extension and content type do not match");
        }

        try {
            if (!hasValidImageSignature(file, inputType)) {
                throw new IllegalArgumentException("Image signature does not match extension");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read upload file");
        }

        return inputType;
    }

    private BufferedImage decodeAndValidateImage(MultipartFile file, ImageType inputType, UploadPolicy policy) {
        try (InputStream inputStream = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(inputStream)) {
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
        int parameterIndex = contentType.indexOf(';');
        String value = parameterIndex == -1 ? contentType : contentType.substring(0, parameterIndex);
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasValidImageSignature(MultipartFile file, ImageType inputType) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        }
        if (read <= 0) {
            return false;
        }

        return switch (inputType) {
            case JPEG -> read >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            case PNG -> read >= 8
                    && Arrays.equals(Arrays.copyOf(header, 8),
                    new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case WEBP -> read >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';
        };
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
}
