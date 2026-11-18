package bookbridge.model;

import bookbridge.util.Validation;

public record Reservation(String reservationId, String memberId, String isbn,
                          int requestedDays) implements Notifiable {
    public Reservation {
        reservationId = Validation.requiredText(reservationId, "Reservation ID");
        memberId = Validation.requiredText(memberId, "Member ID");
        isbn = Validation.requiredText(isbn, "ISBN");
        if (requestedDays < 1 || requestedDays > 30) {
            throw new IllegalArgumentException("Requested days must be from 1 to 30.");
        }
    }

    @Override public String notificationMessage() {
        return "Reservation " + reservationId + " recorded for " + memberId + ": " + isbn;
    }
}
