package bookbridge.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import bookbridge.util.Validation;

/** Immutable loan; return replaces this value in active/history collections. */
public record LoanRecord(String loanId, Member member, String isbn,
                         LocalDate issueDate, LocalDate dueDate, LocalDate returnDate) {
    public LoanRecord {
        loanId = Validation.requiredText(loanId, "Loan ID");
        isbn = Validation.requiredText(isbn, "ISBN");
        Objects.requireNonNull(member, "Member is required.");
        Objects.requireNonNull(issueDate, "Issue date is required.");
        Objects.requireNonNull(dueDate, "Due date is required.");
        long days = ChronoUnit.DAYS.between(issueDate, dueDate);
        if (days < 1 || days > member.getMaximumLoanDays()) {
            throw new IllegalArgumentException("Due date violates the member's borrowing duration.");
        }
        if (returnDate != null && returnDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Return date precedes issue date.");
        }
    }

    public LoanStatus status() { return returnDate == null ? LoanStatus.ACTIVE : LoanStatus.RETURNED; }
    public int getRequestedDays() { return (int) ChronoUnit.DAYS.between(issueDate, dueDate); }
    public boolean isReturned() { return status() == LoanStatus.RETURNED; }
    public boolean isOverdue(LocalDate asOf) {
        Objects.requireNonNull(asOf, "Report date is required.");
        return !isReturned() && dueDate.isBefore(asOf);
    }
    public BigDecimal calculateLateCharge(LocalDate asOf) {
        Objects.requireNonNull(asOf, "Report date is required.");
        LocalDate end = isReturned() ? returnDate : asOf;
        long lateDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, end));
        return BigDecimal.valueOf(member.getDailyFine()).multiply(BigDecimal.valueOf(lateDays));
    }
}
