package bookbridge.model;

import java.util.List;

/** Records read from SQLite before the service validates and restores them. */
public record LibraryState(List<Member> members, List<LibraryBook> books,
        List<LoanRecord> loans, List<Reservation> pendingReservations,
        List<String> closedReservationIds) {
    public LibraryState {
        members = List.copyOf(members);
        books = List.copyOf(books);
        loans = List.copyOf(loans);
        pendingReservations = List.copyOf(pendingReservations);
        closedReservationIds = List.copyOf(closedReservationIds);
    }
}
