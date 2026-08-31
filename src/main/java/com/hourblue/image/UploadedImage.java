package com.hourblue.image;

import java.net.URI;

public record UploadedImage(String secureUrl, String publicId) {

    public UploadedImage {
        if (!isSecureUrl(secureUrl) || isBlank(publicId)) {
            throw new IllegalArgumentException("Uploaded image values are invalid.");
        }
    }

    private static boolean isSecureUrl(String value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}