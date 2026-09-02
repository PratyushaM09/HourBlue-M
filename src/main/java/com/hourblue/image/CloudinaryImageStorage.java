package com.hourblue.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cloudinary.Cloudinary;

import org.springframework.web.multipart.MultipartFile;

public class CloudinaryImageStorage {

    private static final List<String> ALLOWED_FORMATS = List.of("jpg", "jpeg", "png", "webp");

    private final Cloudinary cloudinary;
    private final String folder;
    private final long maxImageSize;

    CloudinaryImageStorage(Cloudinary cloudinary, String folder, long maxImageSize) {
        this.cloudinary = cloudinary;
        this.folder = folder;
        this.maxImageSize = maxImageSize;
    }

    public UploadedImage upload(MultipartFile file) {
        validate(file);

        try {
            Map<?, ?> response = cloudinary.uploader().upload(file.getBytes(), Map.of(
                    "resource_type", "image",
                    "folder", folder,
                    "public_id", UUID.randomUUID().toString(),
                    "allowed_formats", ALLOWED_FORMATS,
                    "overwrite", false));

            Object secureUrl = response.get("secure_url");
            Object publicId = response.get("public_id");
            if (!(secureUrl instanceof String secureUrlText) || !isHttpsUrl(secureUrlText)
                    || !(publicId instanceof String publicIdText) || isBlank(publicIdText)) {
                throw new ImageStorageException();
            }

            return new UploadedImage(secureUrlText, publicIdText);
        } catch (ImageStorageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ImageStorageException(exception);
        }
    }

    public void delete(String publicId) {
        if (isBlank(publicId)) {
            throw new ImageStorageException();
        }

        try {
            Map<?, ?> response = cloudinary.uploader().destroy(publicId, Map.of(
                    "resource_type", "image",
                    "invalidate", true));
            Object result = response.get("result");
            if (!"ok".equals(result) && !"not found".equals(result)) {
                throw new ImageStorageException();
            }
        } catch (ImageStorageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ImageStorageException(exception);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > maxImageSize) {
            throw new ImageStorageException();
        }

        try (InputStream input = file.getInputStream()) {
            if (!isSupported(input.readNBytes(12))) {
                throw new ImageStorageException();
            }
        } catch (IOException exception) {
            throw new ImageStorageException(exception);
        }
    }

    private boolean isSupported(byte[] bytes) {
        return isJpeg(bytes) || isPng(bytes) || isWebp(bytes);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4e
                && bytes[3] == 0x47
                && bytes[4] == 0x0d
                && bytes[5] == 0x0a
                && bytes[6] == 0x1a
                && bytes[7] == 0x0a;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 0x52
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x46
                && bytes[8] == 0x57
                && bytes[9] == 0x45
                && bytes[10] == 0x42
                && bytes[11] == 0x50;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isHttpsUrl(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
