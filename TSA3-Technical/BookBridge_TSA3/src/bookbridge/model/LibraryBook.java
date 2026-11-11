package bookbridge.model;

import java.util.Objects;
import bookbridge.util.Validation;

/** Immutable snapshot: only the service replaces the registered book's status. */
public record LibraryBook(String isbn, String title, BookStatus status) {
    public LibraryBook {
        isbn = Validation.requiredText(isbn, "ISBN");
        title = Validation.requiredText(title, "Book title");
        Objects.requireNonNull(status, "Book status is required.");
    }

    public LibraryBook(String isbn, String title) {
        this(isbn, title, BookStatus.AVAILABLE);
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public boolean isAvailable() { return status == BookStatus.AVAILABLE; }
}
