package bookbridge.app;

import bookbridge.exception.LoanLimitExceededException;
import bookbridge.model.FacultyMember;
import bookbridge.model.GuestMember;
import bookbridge.model.LibraryBook;
import bookbridge.model.LoanRecord;
import bookbridge.model.MembershipType;
import bookbridge.model.StudentMember;
import bookbridge.service.LibraryService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Reproducible demonstration; all business decisions belong to LibraryService. */
public final class BookBridgeApp {
    private BookBridgeApp() { }

    public static void main(String[] args) {
        LibraryService service = new LibraryService(
                Clock.fixed(Instant.parse("2026-11-11T00:00:00Z"), ZoneOffset.UTC));
        service.registerMember(new StudentMember("S100", "Test Student"));
        service.registerMember(new FacultyMember("F100", "Test Faculty"));
        service.registerMember(new GuestMember("G100", "Test Guest"));
        service.addBook(new LibraryBook("ISBN-A", "Java Collections"));
        service.addBook(new LibraryBook("ISBN-B", "Library Design"));
        System.out.println("BOOKBRIDGE TSA3 — fixed demonstration date: 2026-11-11");
        System.out.println("Boundary preview: " + service.classifyRequest("S100", "ISBN-A", 14));
        LoanRecord loan = service.issueLoan("S100", "ISBN-A", 14);
        System.out.println("Issued " + loan.loanId() + "; due " + loan.dueDate());
        System.out.println("Unavailable preview: " + service.classifyRequest("F100", "ISBN-A", 30));
        System.out.println(service.reserveBook("F100", "ISBN-A", 30).notificationMessage());
        System.out.println("Unavailable queue result: " + service.processNextReservation());
        try {
            service.issueLoan("G100", "ISBN-B", 8);
        } catch (LoanLimitExceededException exception) {
            System.out.println("Expected denial: " + exception.getMessage());
        }
        service.returnBook(loan.loanId());
        service.processNextReservation().ifPresent(next ->
                System.out.println("Reservation fulfilled: " + next.member().getMemberId() + ", " + next.loanId()));
        System.out.println("Title search: " + service.searchBooksByTitle("java"));
        System.out.println("Sorted loans: " + service.getSortedLoans());
        var summary = service.getCirculationSummary();
        System.out.printf("Summary: available=%d, checked out=%d, active=%d, history=%d, pending=%d, fines=PHP %.2f%n",
                summary.availableBooks(), summary.checkedOutBooks(), summary.activeLoans(),
                summary.historicalLoans(), summary.pendingReservations(), summary.estimatedFines());
        for (MembershipType type : MembershipType.values()) {
            System.out.println("Active " + type + " loans: " + summary.activeLoansByMembershipType().get(type));
        }
    }
}
