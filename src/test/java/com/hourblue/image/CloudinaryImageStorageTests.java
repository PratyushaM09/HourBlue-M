package com.hourblue.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class CloudinaryImageStorageTests {

    @Test
    void rejectsEmptyOversizeAndUnsupportedFilesBeforeNetworkAccess() {
        Cloudinary cloudinary = mock(Cloudinary.class);
        CloudinaryImageStorage storage = new CloudinaryImageStorage(cloudinary, "hourblue/posts", 4);

        assertThrows(ImageStorageException.class, () -> storage.upload(null));
        assertThrows(ImageStorageException.class, () -> storage.upload(file()));
        assertThrows(ImageStorageException.class, () -> storage.upload(file(0xff, 0xd8, 0xff, 0x00, 0x00)));
        assertThrows(ImageStorageException.class, () -> storage.upload(file(0x47, 0x49, 0x46)));

        verifyNoInteractions(cloudinary);
    }

    @Test
    void acceptsJpegPngAndWebpByLeadingBytes() throws Exception {
        assertUploadSucceeds(file(0xff, 0xd8, 0xff));
        assertUploadSucceeds(file(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a));
        assertUploadSucceeds(file(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50));
    }

    @Test
    void misleadingMimeTypeAndExtensionCannotBypassByteValidation() {
        Cloudinary cloudinary = mock(Cloudinary.class);
        CloudinaryImageStorage storage = new CloudinaryImageStorage(cloudinary, "hourblue/posts", 5_000_000);

        assertThrows(
                ImageStorageException.class,
                () -> storage.upload(new MockMultipartFile(
                        "file",
                        "image.jpg",
                        "image/jpeg",
                        bytes(0x47, 0x49, 0x46))));

        verifyNoInteractions(cloudinary);
    }

    @Test
    void sendsExpectedUploadOptionsAndMapsResponse() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.example/image.jpg",
                "public_id", "hourblue/posts/" + UUID.randomUUID()));

        UploadedImage image = new CloudinaryImageStorage(cloudinary, "hourblue/posts", 5_000_000)
                .upload(file(0xff, 0xd8, 0xff));

        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), options.capture());
        assertEquals("https://res.cloudinary.example/image.jpg", image.secureUrl());
        assertEquals("image", options.getValue().get("resource_type"));
        assertEquals("hourblue/posts", options.getValue().get("folder"));
        assertEquals(false, options.getValue().get("overwrite"));
        assertEquals(java.util.List.of("jpg", "jpeg", "png", "webp"), options.getValue().get("allowed_formats"));
    }

    @Test
    void missingUploadResponseValuesAreRejected() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "https://example.test/image.jpg"));

        assertThrows(
                ImageStorageException.class,
                () -> new CloudinaryImageStorage(cloudinary, "hourblue/posts", 5_000_000)
                        .upload(file(0xff, 0xd8, 0xff)));
    }

    @Test
    void uploadFailureIsWrappedSafely() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("low level"));

        ImageStorageException exception = assertThrows(
                ImageStorageException.class,
                () -> new CloudinaryImageStorage(cloudinary, "hourblue/posts", 5_000_000)
                        .upload(file(0xff, 0xd8, 0xff)));

        assertEquals("Image storage operation failed.", exception.getMessage());
    }

    @Test
    void blankDeleteIdIsRejectedBeforeNetworkAccess() {
        Cloudinary cloudinary = mock(Cloudinary.class);

        assertThrows(
                ImageStorageException.class,
                () -> new CloudinaryImageStorage(cloudinary, "hourblue/posts", 5_000_000).delete(" "));

        verifyNoInteractions(cloudinary);
    }

    @Test
    void deleteHandlesSuccessfulAndAlreadyMissingAssets() throws Exception {
        assertDeleteSucceeds("ok");
        assertDeleteSucceeds("not found");
    }

    @Test
    void deleteUsesImageResourceTypeAndCdnInvalidation() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(), anyMap())).thenReturn(Map.of("result", "ok"));

        new CloudinaryImageStorage(cloudinary, "hourblue/posts", 5_000_000).delete("hourblue/posts/image");

        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(any(), options.capture());
        assertEquals("image", options.getValue().get("resource_type"));
        assertEquals(true, options.getValue().get("invalidate"));
    }

    @Test
    void deleteFailureIsWrappedSafely() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(), anyMap())).thenThrow(new IOException("low level"));

        ImageStorageException exception = assertThrows(
                ImageStorageException.class,
                () -> new CloudinaryImageStorage(cloudinary, "hourblue/posts", 5_000_000).delete("public-id"));

        assertEquals("Image storage operation failed.", exception.getMessage());
    }

    private void assertUploadSucceeds(MockMultipartFile file) throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.example/image.jpg",
                "public_id", "hourblue/posts/" + UUID.randomUUID()));

        new CloudinaryImageStorage(cloudinary, "hourblue/posts", 5_000_000).upload(file);

        verify(uploader).upload(any(), anyMap());
    }

    private void assertDeleteSucceeds(String result) throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(), anyMap())).thenReturn(Map.of("result", result));

        new CloudinaryImageStorage(cloudinary, "hourblue/posts", 5_000_000).delete("hourblue/posts/image");
    }

    private MockMultipartFile file(int... bytes) {
        return new MockMultipartFile("file", "upload.bin", "application/octet-stream", bytes(bytes));
    }

    private byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }
}
