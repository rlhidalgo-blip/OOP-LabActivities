package bookbridge.exception;

/** A member, book, reservation, or generated identifier already exists. */
public final class DuplicateEntityException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DuplicateEntityException(String message) { super(message); }
}
