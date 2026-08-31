package com.hourblue.image;

public class ImageStorageException extends RuntimeException {

    public ImageStorageException() {
        super("Image storage operation failed.");
    }

    public ImageStorageException(Throwable cause) {
        super("Image storage operation failed.", cause);
    }
}
