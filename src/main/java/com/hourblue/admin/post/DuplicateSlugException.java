package com.hourblue.admin.post;

public class DuplicateSlugException extends RuntimeException {

    public DuplicateSlugException() {
        super("Post slug already exists.");
    }
}
