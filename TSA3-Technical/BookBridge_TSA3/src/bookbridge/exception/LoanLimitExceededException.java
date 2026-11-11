package bookbridge.exception;

/** Requested duration exceeds the polymorphic membership allowance. */
public final class LoanLimitExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LoanLimitExceededException(String message) { super(message); }
}
