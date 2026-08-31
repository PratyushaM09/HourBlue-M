package com.hourblue.post;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Mood {
    CALM("Calm"),
    DREAMY("Dreamy"),
    COZY("Cozy"),
    ROMANTIC("Romantic"),
    ADVENTUROUS("Adventurous"),
    NOSTALGIC("Nostalgic");

    private final String displayName;
    private final String slug;

    Mood(String displayName) {
        this.displayName = displayName;
        slug = name().toLowerCase(Locale.ROOT);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSlug() {
        return slug;
    }

    public static Optional<Mood> fromSlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(mood -> mood.slug.equals(slug))
                .findFirst();
    }
}
