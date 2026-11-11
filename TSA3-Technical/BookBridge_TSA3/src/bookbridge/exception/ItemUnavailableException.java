package bookbridge.exception;

/** A book is checked out or reserved for an earlier requester. */
public final class ItemUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ItemUnavailableException(String message) { super(message); }
}
