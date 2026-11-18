package campusconnect.exception;

public class DuplicateTicketException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DuplicateTicketException(String message) {
        super(message);
    }
}
