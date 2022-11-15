package io.github.eroberer.helidon.books.model;

import java.util.UUID;

public record Book(
        String name,
        String author,
        String isbn,
        String language) {
}

