package bookbridge.model;

import java.util.List;

/** Immutable result passed from a database worker to Swing. */
public record LibrarySnapshot(List<Member> members, List<LibraryBook> books,
        List<LoanRecord> loans, List<Reservation> reservations,
        List<LoanRecord> sortedLoans, CirculationSummary summary) {
    public LibrarySnapshot {
        members = List.copyOf(members);
        books = List.copyOf(books);
        loans = List.copyOf(loans);
        reservations = List.copyOf(reservations);
        sortedLoans = List.copyOf(sortedLoans);
    }
}
