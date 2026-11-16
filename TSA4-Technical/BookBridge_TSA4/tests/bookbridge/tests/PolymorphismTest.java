package bookbridge.tests;

import bookbridge.exception.LoanLimitExceededException;
import bookbridge.model.FacultyMember;
import bookbridge.model.GuestMember;
import bookbridge.model.LibraryBook;
import bookbridge.model.Member;
import bookbridge.model.RequestOutcome;
import bookbridge.model.StudentMember;
import bookbridge.service.LibraryService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Assertion-based adaptation of the original print-only TSA2 test. */
public final class PolymorphismTest {
    private PolymorphismTest() { }

    public static void main(String[] args) {
        Member[] members = {new StudentMember("S", "Student"),
                new FacultyMember("F", "Faculty"), new GuestMember("G", "Guest")};
        int[] days = {14, 30, 7};
        double[] rates = {5.0, 2.0, 10.0};
        for (int i = 0; i < members.length; i++) {
            Member member = members[i];
            LibraryService service = new LibraryService(
                    Clock.fixed(Instant.parse("2026-11-11T00:00:00Z"), ZoneOffset.UTC));
            service.registerMember(member);
            service.addBook(new LibraryBook("A", "Test"));
            check(member.getMaximumLoanDays() == days[i], "Maximum days");
            check(member.getDailyFine() == rates[i], "Daily fine");
            check(service.classifyRequest(member.getMemberId(), "A", days[i]) == RequestOutcome.APPROVED,
                    "Exact boundary must be approved");
            check(service.classifyRequest(member.getMemberId(), "A", days[i] + 1) == RequestOutcome.DENIED,
                    "Above boundary must be denied");
            var loan = service.issueLoan(member.getMemberId(), "A", days[i]);
            check(loan.calculateLateCharge(loan.dueDate().plusDays(3)).doubleValue() == rates[i] * 3,
                    "Runtime polymorphic fine");
            check(service.classifyRequest(member.getMemberId(), "A", days[i]) == RequestOutcome.RESERVED,
                    "Unavailable preview");
            try {
                service.issueLoan(member.getMemberId(), "A", days[i] + 1);
                throw new AssertionError("Expected duration violation");
            } catch (LoanLimitExceededException expected) {
                check(service.listActiveLoans().size() == 1, "Denial must not change loans");
            }
            System.out.println("PASS POLY-" + (i + 1) + " " + member.getMembershipType()
                    + " limit=" + days[i] + " rate=" + rates[i] + " boundary/outcomes/fine");
        }
        System.out.println("Polymorphism regression: 3 passed, 0 failed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
